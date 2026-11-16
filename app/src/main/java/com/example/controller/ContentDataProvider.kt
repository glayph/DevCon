package com.example.controller

import android.annotation.SuppressLint
import android.app.usage.UsageStatsManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.CalendarContract
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import com.example.model.AppUsageInfo
import com.example.model.BluetoothDeviceInfo
import com.example.model.BluetoothSummary
import com.example.model.CalendarEventInfo
import com.example.model.CallLogInfo
import com.example.model.ContactInfo
import com.example.model.FileBrowseResult
import com.example.model.FileItemInfo
import com.example.model.LocationInfo
import com.example.model.MediaItemInfo
import com.example.model.PhoneNetworkInfo
import com.example.model.SmsMessageInfo
import java.io.File
import java.io.InputStream
import java.util.TimeZone

object ContentDataProvider {

    fun browseFiles(dirPath: String?, context: Context): FileBrowseResult {
        val hasAllFiles = PermissionHelper.isAllFilesGranted(context)
        val rootDir = runCatching { Environment.getExternalStorageDirectory() }.getOrNull()
        val fallbackDir = runCatching {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }.getOrNull()
            ?: runCatching { context.getExternalFilesDir(null) }.getOrNull()
            ?: context.filesDir

        val target = when {
            !dirPath.isNullOrBlank() -> File(dirPath)
            hasAllFiles && rootDir != null && rootDir.exists() && rootDir.canRead() -> rootDir
            else -> fallbackDir
        }

        val items = if (target.exists() && target.isDirectory) {
            target.listFiles()?.map {
                FileItemInfo(
                    name = it.name,
                    path = it.absolutePath,
                    isDirectory = it.isDirectory,
                    sizeBytes = if (it.isDirectory) 0L else it.length(),
                    lastModified = it.lastModified()
                )
            }?.sortedWith(compareBy<FileItemInfo> { !it.isDirectory }.thenBy { it.name.lowercase() }) ?: emptyList()
        } else {
            emptyList()
        }

        val parent = target.parentFile?.let { p ->
            if (p.canRead() || hasAllFiles) p.absolutePath else null
        }

        return FileBrowseResult(
            currentPath = target.absolutePath,
            parentPath = parent,
            hasAllFilesAccess = hasAllFiles,
            items = items
        )
    }

    fun deleteFile(path: String): Boolean {
        return runCatching {
            val f = File(path)
            if (!f.exists()) return false
            if (f.isDirectory) f.deleteRecursively() else f.delete()
        }.getOrDefault(false)
    }

    fun createFolder(parentPath: String, folderName: String): Boolean {
        return runCatching {
            val parent = if (parentPath.isBlank()) Environment.getExternalStorageDirectory() else File(parentPath)
            val newDir = File(parent, folderName)
            newDir.mkdirs()
        }.getOrDefault(false)
    }

    fun renameFile(path: String, newName: String): Pair<Boolean, String> {
        val cleanName = newName.trim()
        if (cleanName.isBlank() || cleanName.contains("/") || cleanName.contains("\\") || cleanName == "." || cleanName == "..") {
            return false to "Invalid filename"
        }
        return runCatching {
            val src = File(path)
            if (!src.exists()) return false to "File not found"
            val parent = src.parentFile ?: return false to "Invalid parent directory"
            val dest = File(parent, cleanName)
            if (dest.exists()) return false to "Target name already exists"
            val ok = src.renameTo(dest)
            if (ok) true to dest.absolutePath else false to "Rename failed"
        }.getOrElse { false to (it.message ?: "Rename failed") }
    }

    fun readTextFile(path: String, maxBytes: Int = 512 * 1024): Triple<Boolean, String, String> {
        return runCatching {
            val f = File(path)
            if (!f.exists() || f.isDirectory) return Triple(false, "", "File not found")
            if (!f.canRead()) return Triple(false, "", "File not readable")
            if (f.length() > maxBytes) return Triple(false, "", "File exceeds 512 KB editor limit")
            val text = f.readText(Charsets.UTF_8)
            Triple(true, text, "")
        }.getOrElse { Triple(false, "", it.message ?: "Could not read file") }
    }

    fun writeTextFile(path: String, content: String): Pair<Boolean, String> {
        return runCatching {
            val f = File(path)
            if (f.isDirectory) return false to "Path is a directory"
            f.parentFile?.mkdirs()
            f.writeText(content, Charsets.UTF_8)
            true to "Saved ${f.name}"
        }.getOrElse { false to (it.message ?: "Could not write file") }
    }

    fun guessMimeType(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "svg" -> "image/svg+xml"
            "mp4", "m4v" -> "video/mp4"
            "webm" -> "video/webm"
            "mp3" -> "audio/mpeg"
            "m4a", "aac" -> "audio/mp4"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "pdf" -> "application/pdf"
            "json" -> "application/json; charset=utf-8"
            "html", "htm" -> "text/html; charset=utf-8"
            "css" -> "text/css; charset=utf-8"
            "js", "ts", "kt", "java", "py", "sh", "xml", "yml", "yaml", "md", "txt", "log", "csv", "conf", "ini" -> "text/plain; charset=utf-8"
            else -> "application/octet-stream"
        }
    }

    fun getMediaItems(context: Context, type: String): List<MediaItemInfo> {
        val uri = when (type.lowercase()) {
            "videos", "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            "audio", "music" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.MIME_TYPE
        )

        val list = mutableListOf<MediaItemInfo>()
        runCatching {
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

                var count = 0
                while (cursor.moveToNext() && count < 150) {
                    list.add(
                        MediaItemInfo(
                            id = cursor.getLong(idCol),
                            name = cursor.getString(nameCol) ?: "Unknown",
                            type = type.lowercase(),
                            sizeBytes = cursor.getLong(sizeCol),
                            dateAddedSec = cursor.getLong(dateCol),
                            mimeType = cursor.getString(mimeCol) ?: "application/octet-stream"
                        )
                    )
                    count++
                }
            }
        }
        return list
    }

    fun openMediaStream(context: Context, id: Long, type: String): Pair<String, InputStream>? {
        val baseUri = when (type.lowercase()) {
            "videos", "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            "audio", "music" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val itemUri = ContentUris.withAppendedId(baseUri, id)
        val mime = context.contentResolver.getType(itemUri) ?: "application/octet-stream"
        val stream = context.contentResolver.openInputStream(itemUri) ?: return null
        return mime to stream
    }

    fun openMediaDescriptor(context: Context, id: Long, type: String): Triple<String, Long, InputStream>? {
        val baseUri = when (type.lowercase()) {
            "videos", "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            "audio", "music" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val itemUri = ContentUris.withAppendedId(baseUri, id)
        val mime = context.contentResolver.getType(itemUri) ?: "application/octet-stream"
        val afd = runCatching { context.contentResolver.openAssetFileDescriptor(itemUri, "r") }.getOrNull()
        if (afd != null) {
            val len = afd.length
            val stream = afd.createInputStream()
            return Triple(mime, len, stream)
        }
        val stream = context.contentResolver.openInputStream(itemUri) ?: return null
        return Triple(mime, -1L, stream)
    }

    fun getContacts(context: Context): List<ContactInfo> {
        val result = mutableMapOf<String, ContactInfo>()
        runCatching {
            val emails = mutableMapOf<String, String>()
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Email.ADDRESS
                ),
                null, null, null
            )?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
                val addrIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
                while (c.moveToNext()) {
                    val cid = c.getString(idIdx) ?: continue
                    val addr = c.getString(addrIdx) ?: continue
                    if (!emails.containsKey(cid)) emails[cid] = addr
                }
            }

            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext() && result.size < 300) {
                    val cid = cursor.getString(idIdx) ?: continue
                    val name = cursor.getString(nameIdx) ?: "Unknown"
                    val phone = cursor.getString(numIdx) ?: ""
                    if (!result.containsKey(cid)) {
                        result[cid] = ContactInfo(
                            id = cid,
                            name = name,
                            phone = phone,
                            email = emails[cid] ?: ""
                        )
                    }
                }
            }
        }
        return result.values.toList()
    }

    fun addContact(context: Context, name: String, phone: String, email: String): Boolean {
        return runCatching {
            val ops = ArrayList<ContentProviderOperation>()
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build()
            )
            if (phone.isNotBlank()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                        .build()
                )
            }
            if (email.isNotBlank()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                        .build()
                )
            }
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        }.getOrDefault(false)
    }

    fun getSmsMessages(context: Context): List<SmsMessageInfo> {
        val list = mutableListOf<SmsMessageInfo>()
        runCatching {
            val contactMap = mutableMapOf<String, String>()
            runCatching {
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    ),
                    null, null, null
                )?.use { cc ->
                    val nIdx = cc.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val pIdx = cc.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (cc.moveToNext()) {
                        val cname = cc.getString(nIdx) ?: continue
                        val cnum = cc.getString(pIdx)?.filter { it.isDigit() } ?: continue
                        if (cnum.length >= 6) {
                            contactMap[cnum.takeLast(9)] = cname
                        }
                    }
                }
            }

            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.THREAD_ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                    Telephony.Sms.TYPE
                ),
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                val threadIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
                val addrIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val typeIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)

                var count = 0
                while (cursor.moveToNext() && count < 200) {
                    val rawType = cursor.getInt(typeIdx)
                    val typeStr = when (rawType) {
                        Telephony.Sms.MESSAGE_TYPE_INBOX -> "Inbox"
                        Telephony.Sms.MESSAGE_TYPE_SENT -> "Sent"
                        Telephony.Sms.MESSAGE_TYPE_DRAFT -> "Draft"
                        else -> "Other"
                    }
                    val rawAddress = cursor.getString(addrIdx) ?: "Unknown"
                    val digits = rawAddress.filter { it.isDigit() }
                    val resolvedName = if (digits.length >= 6) contactMap[digits.takeLast(9)] ?: "" else ""
                    list.add(
                        SmsMessageInfo(
                            id = cursor.getString(idIdx) ?: "$count",
                            address = rawAddress,
                            body = cursor.getString(bodyIdx) ?: "",
                            date = cursor.getLong(dateIdx),
                            type = typeStr,
                            threadId = cursor.getLong(threadIdx),
                            contactName = resolvedName
                        )
                    )
                    count++
                }
            }
        }
        return list
    }

    fun sendSms(context: Context, phoneNumber: String, message: String): Boolean {
        if (phoneNumber.isBlank() || message.isBlank()) return false
        return runCatching {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            }
            true
        }.getOrDefault(false)
    }

    fun getCallLogs(context: Context): List<CallLogInfo> {
        val list = mutableListOf<CallLogInfo>()
        runCatching {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION
                ),
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                val numIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val nameIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                val typeIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val dateIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val durIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)

                var count = 0
                while (cursor.moveToNext() && count < 120) {
                    val typeStr = when (cursor.getInt(typeIdx)) {
                        CallLog.Calls.INCOMING_TYPE -> "Incoming"
                        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                        CallLog.Calls.MISSED_TYPE -> "Missed"
                        CallLog.Calls.REJECTED_TYPE -> "Rejected"
                        else -> "Other"
                    }
                    list.add(
                        CallLogInfo(
                            number = cursor.getString(numIdx) ?: "Unknown",
                            name = cursor.getString(nameIdx) ?: "",
                            type = typeStr,
                            date = cursor.getLong(dateIdx),
                            durationSec = cursor.getLong(durIdx)
                        )
                    )
                    count++
                }
            }
        }
        return list
    }

    @SuppressLint("MissingPermission")
    fun getPhoneNetworkInfo(context: Context): PhoneNetworkInfo {
        return runCatching {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val op = tm.networkOperatorName?.takeIf { it.isNotBlank() } ?: "No Carrier"
            val sim = when (tm.simState) {
                TelephonyManager.SIM_STATE_READY -> "Ready"
                TelephonyManager.SIM_STATE_ABSENT -> "No SIM"
                else -> "Unknown"
            }
            val pType = when (tm.phoneType) {
                TelephonyManager.PHONE_TYPE_GSM -> "GSM"
                TelephonyManager.PHONE_TYPE_CDMA -> "CDMA"
                TelephonyManager.PHONE_TYPE_SIP -> "SIP"
                else -> "None"
            }
            val nType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                when (tm.dataNetworkType) {
                    TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                    TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
                    TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
                    else -> "Mobile / Wi-Fi"
                }
            } else "Mobile"
            PhoneNetworkInfo(op, nType, sim, pType)
        }.getOrDefault(PhoneNetworkInfo("Unavailable", "Unknown", "Unknown", "Unknown"))
    }

    @SuppressLint("MissingPermission")
    fun makeCall(context: Context, number: String): Boolean {
        if (number.isBlank()) return false
        return runCatching {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun getCalendarEvents(context: Context): List<CalendarEventInfo> {
        val list = mutableListOf<CalendarEventInfo>()
        runCatching {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                arrayOf(
                    CalendarContract.Events._ID,
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.DTSTART,
                    CalendarContract.Events.DTEND,
                    CalendarContract.Events.EVENT_LOCATION,
                    CalendarContract.Events.DESCRIPTION
                ),
                null,
                null,
                "${CalendarContract.Events.DTSTART} DESC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
                val titleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                val startIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                val endIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
                val locIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION)
                val descIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)

                var count = 0
                while (cursor.moveToNext() && count < 100) {
                    list.add(
                        CalendarEventInfo(
                            id = cursor.getLong(idIdx),
                            title = cursor.getString(titleIdx) ?: "Untitled Event",
                            startMs = cursor.getLong(startIdx),
                            endMs = cursor.getLong(endIdx),
                            location = cursor.getString(locIdx) ?: "",
                            description = cursor.getString(descIdx) ?: ""
                        )
                    )
                    count++
                }
            }
        }
        return list
    }

    fun addCalendarEvent(context: Context, title: String, startMs: Long, endMs: Long, location: String, description: String): Boolean {
        return runCatching {
            var calId = 1L
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(CalendarContract.Calendars._ID),
                null, null, null
            )?.use { c ->
                if (c.moveToFirst()) {
                    calId = c.getLong(0)
                }
            }
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calId)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DTSTART, startMs)
                put(CalendarContract.Events.DTEND, endMs)
                put(CalendarContract.Events.EVENT_LOCATION, location)
                put(CalendarContract.Events.DESCRIPTION, description)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }
            context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) != null
        }.getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    fun getLocationInfo(context: Context): LocationInfo {
        return runCatching {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = lm.getProviders(true)
            var best: Location? = null
            for (provider in providers) {
                val loc = runCatching { lm.getLastKnownLocation(provider) }.getOrNull() ?: continue
                if (best == null || loc.accuracy < best.accuracy) {
                    best = loc
                }
            }
            if (best != null) {
                LocationInfo(
                    available = true,
                    latitude = best.latitude,
                    longitude = best.longitude,
                    accuracyMeters = best.accuracy,
                    altitudeMeters = best.altitude,
                    speedMps = best.speed,
                    provider = best.provider ?: "gps",
                    timestamp = best.time
                )
            } else {
                LocationInfo(false, 0.0, 0.0, 0f, 0.0, 0f, "No fix yet", 0L)
            }
        }.getOrDefault(LocationInfo(false, 0.0, 0.0, 0f, 0.0, 0f, "Unavailable", 0L))
    }

    @SuppressLint("MissingPermission")
    fun getBluetoothSummary(context: Context): BluetoothSummary {
        return runCatching {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bm?.adapter ?: return BluetoothSummary(false, false, "None", emptyList())
            val bonded = adapter.bondedDevices?.map { dev ->
                val bondStr = when (dev.bondState) {
                    BluetoothDevice.BOND_BONDED -> "Paired"
                    BluetoothDevice.BOND_BONDING -> "Pairing"
                    else -> "None"
                }
                BluetoothDeviceInfo(
                    name = dev.name ?: "Unnamed Device",
                    address = dev.address ?: "--",
                    bondState = bondStr
                )
            } ?: emptyList()

            BluetoothSummary(
                isSupported = true,
                isEnabled = adapter.isEnabled,
                adapterName = adapter.name ?: Build.MODEL,
                devices = bonded
            )
        }.getOrDefault(BluetoothSummary(false, false, "Permission Required", emptyList()))
    }

    fun getAppUsageStats(context: Context): List<AppUsageInfo> {
        if (!PermissionHelper.isUsageStatsGranted(context)) return emptyList()
        return runCatching {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val end = System.currentTimeMillis()
            val start = end - (24L * 60L * 60L * 1000L)
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end) ?: return emptyList()
            val pm = context.packageManager

            stats.filter { it.totalTimeInForeground > 0 }
                .groupBy { it.packageName }
                .map { (pkg, entries) ->
                    val totalMs = entries.sumOf { it.totalTimeInForeground }
                    val lastUsed = entries.maxOf { it.lastTimeUsed }
                    val label = runCatching {
                        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                    }.getOrDefault(pkg)
                    AppUsageInfo(
                        packageName = pkg,
                        appName = label,
                        totalMinutes = totalMs / 60000L,
                        lastUsedMs = lastUsed
                    )
                }
                .sortedByDescending { it.totalMinutes }
                .take(40)
        }.getOrDefault(emptyList())
    }
}
