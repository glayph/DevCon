package com.example.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object PhoneDeckIcons {

    private fun icon(name: String, pathData: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                parsePath(pathData)
            }
        }.build()

    private fun androidx.compose.ui.graphics.vector.PathBuilder.parsePath(d: String) {
        val tokens = Regex("[A-Za-z]|[-+]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?")
            .findAll(d)
            .map { it.value }
            .toList()
        var i = 0
        var cmd = ' '
        while (i < tokens.size) {
            val t = tokens[i]
            if (t.length == 1 && t[0].isLetter()) {
                cmd = t[0]
                i++
                if (cmd == 'Z' || cmd == 'z') {
                    close()
                    continue
                }
            }
            when (cmd) {
                'M' -> {
                    moveTo(tokens[i++].toFloat(), tokens[i++].toFloat())
                    cmd = 'L'
                }
                'm' -> {
                    moveToRelative(tokens[i++].toFloat(), tokens[i++].toFloat())
                    cmd = 'l'
                }
                'L' -> lineTo(tokens[i++].toFloat(), tokens[i++].toFloat())
                'l' -> lineToRelative(tokens[i++].toFloat(), tokens[i++].toFloat())
                'H' -> horizontalLineTo(tokens[i++].toFloat())
                'h' -> horizontalLineToRelative(tokens[i++].toFloat())
                'V' -> verticalLineTo(tokens[i++].toFloat())
                'v' -> verticalLineToRelative(tokens[i++].toFloat())
                'C' -> curveTo(
                    tokens[i++].toFloat(), tokens[i++].toFloat(),
                    tokens[i++].toFloat(), tokens[i++].toFloat(),
                    tokens[i++].toFloat(), tokens[i++].toFloat()
                )
                'c' -> curveToRelative(
                    tokens[i++].toFloat(), tokens[i++].toFloat(),
                    tokens[i++].toFloat(), tokens[i++].toFloat(),
                    tokens[i++].toFloat(), tokens[i++].toFloat()
                )
                'S' -> reflectiveCurveTo(
                    tokens[i++].toFloat(), tokens[i++].toFloat(),
                    tokens[i++].toFloat(), tokens[i++].toFloat()
                )
                's' -> reflectiveCurveToRelative(
                    tokens[i++].toFloat(), tokens[i++].toFloat(),
                    tokens[i++].toFloat(), tokens[i++].toFloat()
                )
                'Q' -> quadTo(
                    tokens[i++].toFloat(), tokens[i++].toFloat(),
                    tokens[i++].toFloat(), tokens[i++].toFloat()
                )
                'q' -> quadToRelative(
                    tokens[i++].toFloat(), tokens[i++].toFloat(),
                    tokens[i++].toFloat(), tokens[i++].toFloat()
                )
                'T' -> reflectiveQuadTo(tokens[i++].toFloat(), tokens[i++].toFloat())
                't' -> reflectiveQuadToRelative(tokens[i++].toFloat(), tokens[i++].toFloat())
                'A' -> arcTo(
                    horizontalEllipseRadius = tokens[i++].toFloat(),
                    verticalEllipseRadius = tokens[i++].toFloat(),
                    theta = tokens[i++].toFloat(),
                    isMoreThanHalf = tokens[i++].toFloat() != 0f,
                    isPositiveArc = tokens[i++].toFloat() != 0f,
                    x1 = tokens[i++].toFloat(),
                    y1 = tokens[i++].toFloat()
                )
                'a' -> arcToRelative(
                    a = tokens[i++].toFloat(),
                    b = tokens[i++].toFloat(),
                    theta = tokens[i++].toFloat(),
                    isMoreThanHalf = tokens[i++].toFloat() != 0f,
                    isPositiveArc = tokens[i++].toFloat() != 0f,
                    dx1 = tokens[i++].toFloat(),
                    dy1 = tokens[i++].toFloat()
                )
                'Z', 'z' -> close()
                else -> i++
            }
        }
    }

    val ContentCopy: ImageVector by lazy {
        icon("ContentCopy", "M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1 .9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z")
    }
    val OpenInBrowser: ImageVector by lazy {
        icon("OpenInBrowser", "M19 4H5c-1.11 0-2 .9-2 2v12c0 1.1 .89 2 2 2h4v-2H5V8h14v10h-4v2h4c1.1 0 2-.9 2-2V6c0-1.1-.89-2-2-2zm-7 6l-4 4h3v6h2v-6h3l-4-4z")
    }
    val Folder: ImageVector by lazy {
        icon("Folder", "M10 4H4c-1.1 0-1.99 .9-1.99 2L2 18c0 1.1 .9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z")
    }
    val PermMedia: ImageVector by lazy {
        icon("PermMedia", "M2 6H0v5h.01L0 20c0 1.1 .9 2 2 2h18v-2H2V6zm20-2h-8l-2-2H6c-1.1 0-1.99 .9-1.99 2L4 16c0 1.1 .9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zM7 15l4.5-6 3.5 4.51 2.5-3.01L21 15H7z")
    }
    val Contacts: ImageVector by lazy {
        icon("Contacts", "M20 0H4v2h16V0zM4 24h16v-2H4v2zM20 4H4c-1.1 0-2 .9-2 2v12c0 1.1 .9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm-8 2.75c1.24 0 2.25 1.01 2.25 2.25s-1.01 2.25-2.25 2.25S9.75 10.24 9.75 9 10.76 6.75 12 6.75zM17 17H7v-1.5c0-1.67 3.33-2.5 5-2.5s5 .83 5 2.5V17z")
    }
    val Sms: ImageVector by lazy {
        icon("Sms", "M20 2H4c-1.1 0-1.99 .9-1.99 2L2 22l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM9 11H7V9h2v2zm4 0h-2V9h2v2zm4 0h-2V9h2v2z")
    }
    val Call: ImageVector by lazy {
        icon("Call", "M20.01 15.38c-1.23 0-2.42-.2-3.53-.56-.35-.12-.74-.03-1.01 .24l-1.57 1.97c-2.83-1.35-5.48-3.9-6.89-6.83l1.95-1.66c.27-.28 .35-.67 .24-1.02-.37-1.11-.56-2.3-.56-3.53 0-.54-.45-.99-.99-.99H4.19C3.65 3 3 3.24 3 3.99 3 13.28 10.73 21 20.01 21c.71 0 .99-.63 .99-1.18v-3.45c0-.54-.45-.99-.99-.99z")
    }
    val CalendarToday: ImageVector by lazy {
        icon("CalendarToday", "M20 3h-1V1h-2v2H7V1H5v2H4c-1.1 0-2 .9-2 2v16c0 1.1 .9 2 2 2h16c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 18H4V8h16v13z")
    }
    val LocationOn: ImageVector by lazy {
        icon("LocationOn", "M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z")
    }
    val CameraAlt: ImageVector by lazy {
        icon("CameraAlt", "M12 15.2c1.77 0 3.2-1.43 3.2-3.2S13.77 8.8 12 8.8 8.8 10.23 8.8 12s1.43 3.2 3.2 3.2zM9 2L7.17 4H4c-1.1 0-2 .9-2 2v12c0 1.1 .9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2h-3.17L15 2H9zm3 15c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5z")
    }
    val Mic: ImageVector by lazy {
        icon("Mic", "M12 14c1.66 0 2.99-1.34 2.99-3L15 5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5.3-3c0 3-2.54 5.1-5.3 5.1S6.7 14 6.7 11H5c0 3.41 2.72 6.23 6 6.72V21h2v-3.28c3.28-.48 6-3.3 6-6.72h-1.7z")
    }
    val Bluetooth: ImageVector by lazy {
        icon("Bluetooth", "M17.71 7.71L12 2h-1v7.59L6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 11 14.41V22h1l5.71-5.71-4.3-4.29 4.3-4.29zM13 5.83l1.88 1.88L13 9.59V5.83zm1.88 10.46L13 18.17v-3.76l1.88 1.88z")
    }
    val NotificationsActive: ImageVector by lazy {
        icon("NotificationsActive", "M7.58 4.08L6.15 2.65C3.75 4.48 2.17 7.3 2.03 10.5h2c.15-2.65 1.51-4.97 3.55-6.42zm12.39 6.42h2c-.15-3.2-1.73-6.02-4.12-7.85l-1.42 1.43c2.02 1.45 3.39 3.77 3.54 6.42zM18 11c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5 .67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2v-5zm-6 11c.14 0 .27-.01 .4-.04 .65-.14 1.18-.58 1.44-1.18 .1-.24 .15-.5 .15-.78h-4c.01 1.1 .9 2 2.01 2z")
    }
    val BatteryChargingFull: ImageVector by lazy {
        icon("BatteryChargingFull", "M15.67 4H14V2h-4v2H8.33C7.6 4 7 4.6 7 5.33v15.33C7 21.4 7.6 22 8.33 22h7.33c.74 0 1.34-.6 1.34-1.33V5.33C17 4.6 16.4 4 15.67 4zM11 20v-5.5H9L13 7v5.5h2L11 20z")
    }
    val Layers: ImageVector by lazy {
        icon("Layers", "M11.99 18.54l-7.37-5.73L3 14.07l9 7 9-7-1.63-1.27-7.38 5.74zM12 16l7.36-5.73L21 9l-9-7-9 7 1.63 1.27L12 16z")
    }
    val Settings: ImageVector by lazy {
        icon("Settings", "M19.14 12.94c.04-.3 .06-.61 .06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14 .23-.41 .12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39 .96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43 .17-.47 .41l-.36 2.54c-.59 .24-1.13 .57-1.62 .94l-2.39-.96c-.22-.08-.47 0-.59 .22L2.74 8.87c-.12 .21-.08 .47 .12 .61l2.03 1.58c-.05 .3-.09 .63-.09 .94s.02 .64 .07 .94l-2.03 1.58c-.18 .14-.23 .41-.12 .61l1.92 3.32c.12 .22 .37 .29 .59 .22l2.39-.96c.5 .38 1.03 .7 1.62 .94l.36 2.54c.05 .24 .24 .41 .48 .41h3.84c.24 0 .44-.17 .47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39 .96c.22 .08 .47 0 .59-.22l1.92-3.32c.12-.22 .07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z")
    }
    val QueryStats: ImageVector by lazy {
        icon("QueryStats", "M19.88 18.47c.44-.7 .7-1.51 .7-2.39 0-2.49-2.01-4.5-4.5-4.5s-4.5 2.01-4.5 4.5 2.01 4.5 4.5 4.5c.88 0 1.69-.26 2.39-.7L21.58 23 23 21.58l-3.12-3.11zm-3.8 .11c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5zm-.36-8.5c-.74-.02-1.45 .18-2.1 .49l-.8-3.61L9.5 13 7.58 6.38 3 12v-3l4.75-6.11 2.74 6.23L13.5 3l2.22 7.08z")
    }
    val SystemUpdateAlt: ImageVector by lazy {
        icon("SystemUpdateAlt", "M12 16.5l4-4h-3v-9h-2v9H8l4 4zm9-13h-6v1.99h6v14.03H3V5.49h6V3.5H3c-1.1 0-2 .9-2 2v14c0 1.1 .9 2 2 2h18c1.1 0 2-.9 2-2v-14c0-1.1-.9-2-2-2z")
    }
    val AccessibilityNew: ImageVector by lazy {
        icon("AccessibilityNew", "M20.5 6c-2.61 .7-5.67 1-8.5 1s-5.89-.3-8.5-1L3 8c1.86 .5 4 .83 6 1v13h2v-6h2v6h2V9c2-.17 4.14-.5 6-1l-.5-2zM12 6c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2z")
    }
    val Cast: ImageVector by lazy {
        icon("Cast", "M21 3H3c-1.1 0-2 .9-2 2v3h2V5h18v14h-7v2h7c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM1 18v3h3c0-1.66-1.34-3-3-3zm0-4v2c2.76 0 5 2.24 5 5h2c0-3.87-3.13-7-7-7zm0-4v2c4.97 0 9 4.03 9 9h2c0-6.08-4.93-11-11-11z")
    }
    val Close: ImageVector by lazy {
        icon("Close", "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z")
    }
    val Refresh: ImageVector by lazy {
        icon("Refresh", "M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14 .69 4.22 1.78L13 11h7V4l-2.35 2.35z")
    }
    val Visibility: ImageVector by lazy {
        icon("Visibility", "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z")
    }
    val VisibilityOff: ImageVector by lazy {
        icon("VisibilityOff", "M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.82l2.92 2.92C21.07 15.62 22 13.93 23 12c-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2.27 1.72L1 2.99l3.09 3.09C2.64 7.17 1.46 9.41 1 12c1.73 4.39 6 7.5 11 7.5 1.61 0 3.14-.32 4.54-.9l3.17 3.17L21.98 20 2.27 1.72zM12 17c-2.76 0-5-2.24-5-5 0-.44.06-.86.17-1.26l1.55 1.55c-.01.07-.02.14-.02.21 0 1.66 1.34 3 3 3 .07 0 .14-.01.21-.02l1.55 1.55c-.4.11-.82.17-1.26.17z")
    }
}
