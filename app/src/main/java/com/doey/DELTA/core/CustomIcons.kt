package com.doey.DELTA.core

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Iconos Lineales Minimalistas — Doey UI
 * Stroke 1.5dp, StrokeCap.Round, sin relleno
 */
object CustomIcons {
    private val S = SolidColor(Color(0xFF1A1A1A))
    private const val W = 1.5f

    // ── Navegación ─────────────────────────────────────────────────────────────

    val Menu = icon("Menu") {
        moveTo(3f, 6f); lineTo(21f, 6f)
        moveTo(3f, 12f); lineTo(21f, 12f)
        moveTo(3f, 18f); lineTo(15f, 18f)
    }

    val Home = icon("Home") {
        moveTo(3f, 9.5f); lineTo(12f, 2f); lineTo(21f, 9.5f)
        verticalLineTo(20f)
        arcTo(2f, 2f, 0f, false, true, 19f, 22f)
        horizontalLineTo(5f)
        arcTo(2f, 2f, 0f, false, true, 3f, 20f)
        close()
        moveTo(9f, 22f); verticalLineTo(13f); horizontalLineTo(15f); verticalLineTo(22f)
    }

    val Person = icon("Person") {
        moveTo(12f, 11f)
        arcTo(4f, 4f, 0f, true, false, 12f, 3f)
        arcTo(4f, 4f, 0f, false, false, 12f, 11f)
        moveTo(4f, 21f)
        verticalLineTo(19f)
        arcTo(6f, 6f, 0f, false, true, 10f, 13f)
        horizontalLineTo(14f)
        arcTo(6f, 6f, 0f, false, true, 20f, 19f)
        verticalLineTo(21f)
    }

    val Settings = icon("Settings") {
        moveTo(12f, 15f)
        arcTo(3f, 3f, 0f, true, false, 12f, 9f)
        arcTo(3f, 3f, 0f, false, false, 12f, 15f)
        moveTo(19.4f, 15f)
        arcTo(1.65f, 1.65f, 0f, false, false, 19.73f, 16.82f)
        lineTo(19.79f, 16.88f)
        arcTo(2f, 2f, 0f, false, true, 19.79f, 19.71f)
        arcTo(2f, 2f, 0f, false, true, 16.96f, 19.71f)
        lineTo(16.9f, 19.65f)
        arcTo(1.65f, 1.65f, 0f, false, false, 15.08f, 19.32f)
        arcTo(1.65f, 1.65f, 0f, false, false, 14.08f, 20.83f)
        verticalLineTo(21f)
        arcTo(2f, 2f, 0f, false, true, 10.08f, 21f)
        verticalLineTo(20.91f)
        arcTo(1.65f, 1.65f, 0f, false, false, 9f, 19.4f)
        arcTo(1.65f, 1.65f, 0f, false, false, 7.18f, 19.73f)
        lineTo(7.12f, 19.79f)
        arcTo(2f, 2f, 0f, false, true, 4.29f, 19.79f)
        arcTo(2f, 2f, 0f, false, true, 4.29f, 16.96f)
        lineTo(4.35f, 16.9f)
        arcTo(1.65f, 1.65f, 0f, false, false, 4.68f, 15.08f)
        arcTo(1.65f, 1.65f, 0f, false, false, 3.17f, 14.08f)
        horizontalLineTo(3f)
        arcTo(2f, 2f, 0f, false, true, 3f, 10.08f)
        horizontalLineTo(3.09f)
        arcTo(1.65f, 1.65f, 0f, false, false, 4.6f, 9f)
        arcTo(1.65f, 1.65f, 0f, false, false, 4.27f, 7.18f)
        lineTo(4.21f, 7.12f)
        arcTo(2f, 2f, 0f, false, true, 4.21f, 4.29f)
        arcTo(2f, 2f, 0f, false, true, 7.04f, 4.29f)
        lineTo(7.1f, 4.35f)
        arcTo(1.65f, 1.65f, 0f, false, false, 8.92f, 4.68f)
        arcTo(1.65f, 1.65f, 0f, false, false, 9.92f, 3.17f)
        verticalLineTo(3f)
        arcTo(2f, 2f, 0f, false, true, 13.92f, 3f)
        verticalLineTo(3.09f)
        arcTo(1.65f, 1.65f, 0f, false, false, 15f, 4.6f)
        arcTo(1.65f, 1.65f, 0f, false, false, 16.82f, 4.27f)
        lineTo(16.88f, 4.21f)
        arcTo(2f, 2f, 0f, false, true, 19.71f, 4.21f)
        arcTo(2f, 2f, 0f, false, true, 19.71f, 7.04f)
        lineTo(19.65f, 7.1f)
        arcTo(1.65f, 1.65f, 0f, false, false, 19.32f, 8.92f)
        arcTo(1.65f, 1.65f, 0f, false, false, 20.83f, 9.92f)
        horizontalLineTo(21f)
        arcTo(2f, 2f, 0f, false, true, 21f, 13.92f)
        horizontalLineTo(20.91f)
        arcTo(1.65f, 1.65f, 0f, false, false, 19.4f, 15f)
    }

    // ── Acciones ──────────────────────────────────────────────────────────────

    val Send = icon("Send") {
        moveTo(22f, 2f); lineTo(11f, 13f)
        moveTo(22f, 2f); lineTo(15f, 22f); lineTo(11f, 13f); lineTo(2f, 9f); close()
    }

    val Mic = icon("Mic") {
        moveTo(12f, 1f)
        arcTo(3f, 3f, 0f, false, false, 9f, 4f)
        verticalLineTo(11f)
        arcTo(3f, 3f, 0f, false, false, 15f, 11f)
        verticalLineTo(4f)
        arcTo(3f, 3f, 0f, false, false, 12f, 1f)
        moveTo(19f, 11f)
        arcTo(7f, 7f, 0f, false, true, 5f, 11f)
        moveTo(12f, 18f); verticalLineTo(22f)
    }

    val Search = icon("Search") {
        moveTo(11f, 19f)
        arcTo(8f, 8f, 0f, true, false, 11f, 3f)
        arcTo(8f, 8f, 0f, false, false, 11f, 19f)
        moveTo(21f, 21f); lineTo(16.65f, 16.65f)
    }

    val Add = icon("Add") {
        moveTo(12f, 5f); verticalLineTo(19f)
        moveTo(5f, 12f); horizontalLineTo(19f)
    }

    val Close = icon("Close") {
        moveTo(18f, 6f); lineTo(6f, 18f)
        moveTo(6f, 6f); lineTo(18f, 18f)
    }

    val Check = icon("Check") {
        moveTo(20f, 6f); lineTo(9f, 17f); lineTo(4f, 12f)
    }

    val CheckCircle = icon("CheckCircle") {
        moveTo(22f, 11.08f)
        verticalLineTo(12f)
        arcTo(10f, 10f, 0f, true, true, 13.59f, 3.34f)
        moveTo(22f, 4f); lineTo(12f, 14.01f); lineTo(9f, 11.01f)
    }

    val Edit = icon("Edit") {
        moveTo(11f, 4f)
        horizontalLineTo(4f)
        arcTo(2f, 2f, 0f, false, false, 2f, 6f)
        verticalLineTo(20f)
        arcTo(2f, 2f, 0f, false, false, 4f, 22f)
        horizontalLineTo(18f)
        arcTo(2f, 2f, 0f, false, false, 20f, 20f)
        verticalLineTo(13f)
        moveTo(18.5f, 2.5f)
        arcTo(2.121f, 2.121f, 0f, false, true, 21.5f, 5.5f)
        lineTo(12f, 15f); lineTo(8f, 16f); lineTo(9f, 12f)
        close()
    }

    val Delete = icon("Delete") {
        moveTo(3f, 6f); horizontalLineTo(21f)
        moveTo(8f, 6f); verticalLineTo(4f); horizontalLineTo(16f); verticalLineTo(6f)
        moveTo(19f, 6f); lineTo(18f, 20f)
        arcTo(2f, 2f, 0f, false, true, 16f, 22f)
        horizontalLineTo(8f)
        arcTo(2f, 2f, 0f, false, true, 6f, 20f)
        lineTo(5f, 6f)
    }

    val Refresh = icon("Refresh") {
        moveTo(23f, 4f); verticalLineTo(10f); horizontalLineTo(17f)
        moveTo(1f, 20f); verticalLineTo(14f); horizontalLineTo(7f)
        moveTo(3.51f, 9f)
        arcTo(9f, 9f, 0f, false, true, 20.49f, 15f)
        moveTo(20.49f, 9f)
        arcTo(9f, 9f, 0f, false, false, 3.51f, 15f)
    }

    val Share = icon("Share") {
        moveTo(18f, 8f)
        arcTo(3f, 3f, 0f, true, false, 18f, 2f)
        arcTo(3f, 3f, 0f, false, false, 18f, 8f)
        moveTo(6f, 15f)
        arcTo(3f, 3f, 0f, true, false, 6f, 9f)
        arcTo(3f, 3f, 0f, false, false, 6f, 15f)
        moveTo(18f, 22f)
        arcTo(3f, 3f, 0f, true, false, 18f, 16f)
        arcTo(3f, 3f, 0f, false, false, 18f, 22f)
        moveTo(8.59f, 13.51f); lineTo(15.42f, 17.49f)
        moveTo(15.41f, 6.51f); lineTo(8.59f, 10.49f)
    }

    val Save = icon("Save") {
        moveTo(19f, 21f)
        horizontalLineTo(5f)
        arcTo(2f, 2f, 0f, false, true, 3f, 19f)
        verticalLineTo(5f)
        arcTo(2f, 2f, 0f, false, true, 5f, 3f)
        horizontalLineTo(16f)
        lineTo(21f, 8f)
        verticalLineTo(19f)
        arcTo(2f, 2f, 0f, false, true, 19f, 21f)
        moveTo(17f, 21f); verticalLineTo(13f); horizontalLineTo(7f); verticalLineTo(21f)
        moveTo(7f, 3f); verticalLineTo(8f); horizontalLineTo(15f)
    }

    val PlayArrow = icon("PlayArrow") {
        moveTo(5f, 3f); lineTo(19f, 12f); lineTo(5f, 21f); close()
    }

    val ExpandMore = icon("ExpandMore") {
        moveTo(6f, 9f); lineTo(12f, 15f); lineTo(18f, 9f)
    }

    val ExpandLess = icon("ExpandLess") {
        moveTo(18f, 15f); lineTo(12f, 9f); lineTo(6f, 15f)
    }

    val ArrowForward = icon("ArrowForward") {
        moveTo(5f, 12f); horizontalLineTo(19f)
        moveTo(12f, 5f); lineTo(19f, 12f); lineTo(12f, 19f)
    }

    val ArrowBack = icon("ArrowBack") {
        moveTo(19f, 12f); horizontalLineTo(5f)
        moveTo(12f, 19f); lineTo(5f, 12f); lineTo(12f, 5f)
    }

    // ── Contenido / Herramientas ──────────────────────────────────────────────

    val Star = icon("Star") {
        moveTo(12f, 2f); lineTo(15.09f, 8.26f); lineTo(22f, 9.27f)
        lineTo(17f, 14.14f); lineTo(18.18f, 21.02f)
        lineTo(12f, 17.77f); lineTo(5.82f, 21.02f)
        lineTo(7f, 14.14f); lineTo(2f, 9.27f); lineTo(8.91f, 8.26f); close()
    }

    val Clock = icon("Clock") {
        moveTo(12f, 22f)
        arcTo(10f, 10f, 0f, true, false, 12f, 2f)
        arcTo(10f, 10f, 0f, false, false, 12f, 22f)
        moveTo(12f, 6f); verticalLineTo(12f); lineTo(16f, 14f)
    }

    val Message = icon("Message") {
        moveTo(21f, 15f)
        arcTo(2f, 2f, 0f, false, true, 19f, 17f)
        horizontalLineTo(7f)
        lineTo(3f, 21f)
        verticalLineTo(5f)
        arcTo(2f, 2f, 0f, false, true, 5f, 3f)
        horizontalLineTo(19f)
        arcTo(2f, 2f, 0f, false, true, 21f, 5f)
        close()
    }

    val LibraryBooks = icon("LibraryBooks") {
        moveTo(4f, 7f); verticalLineTo(19f)
        arcTo(2f, 2f, 0f, false, false, 6f, 21f); horizontalLineTo(18f)
        moveTo(8f, 3f); horizontalLineTo(20f)
        arcTo(2f, 2f, 0f, false, true, 22f, 5f)
        verticalLineTo(15f)
        arcTo(2f, 2f, 0f, false, true, 20f, 17f)
        horizontalLineTo(8f)
        arcTo(2f, 2f, 0f, false, true, 6f, 15f)
        verticalLineTo(5f)
        arcTo(2f, 2f, 0f, false, true, 8f, 3f)
        moveTo(10f, 7f); horizontalLineTo(18f)
        moveTo(10f, 11f); horizontalLineTo(18f)
        moveTo(10f, 15f); horizontalLineTo(14f)
    }

    val Lock = icon("Lock") {
        moveTo(19f, 11f); horizontalLineTo(5f)
        arcTo(2f, 2f, 0f, false, false, 3f, 13f)
        verticalLineTo(20f)
        arcTo(2f, 2f, 0f, false, false, 5f, 22f)
        horizontalLineTo(19f)
        arcTo(2f, 2f, 0f, false, false, 21f, 20f)
        verticalLineTo(13f)
        arcTo(2f, 2f, 0f, false, false, 19f, 11f)
        moveTo(7f, 11f); verticalLineTo(7f)
        arcTo(5f, 5f, 0f, false, true, 17f, 7f)
        verticalLineTo(11f)
    }

    val BugReport = icon("BugReport") {
        moveTo(12f, 9f)
        arcTo(4f, 4f, 0f, false, true, 16f, 13f)
        verticalLineTo(17f)
        arcTo(4f, 4f, 0f, false, true, 12f, 21f)
        arcTo(4f, 4f, 0f, false, true, 8f, 17f)
        verticalLineTo(13f)
        arcTo(4f, 4f, 0f, false, true, 12f, 9f)
        moveTo(12f, 9f); verticalLineTo(6f)
        moveTo(16f, 13f); horizontalLineTo(20f)
        moveTo(16f, 17f); horizontalLineTo(20f)
        moveTo(8f, 13f); horizontalLineTo(4f)
        moveTo(8f, 17f); horizontalLineTo(4f)
        moveTo(9f, 6f)
        lineTo(8f, 4f)
        moveTo(15f, 6f); lineTo(16f, 4f)
    }

    val AccountTree = icon("AccountTree") {
        moveTo(22f, 11f); verticalLineTo(3f); horizontalLineTo(15f); verticalLineTo(11f); close()
        moveTo(7f, 21f); verticalLineTo(13f); horizontalLineTo(2f); verticalLineTo(21f); close()
        moveTo(22f, 21f); verticalLineTo(13f); horizontalLineTo(15f); verticalLineTo(21f); close()
        moveTo(15f, 7f); horizontalLineTo(11f); verticalLineTo(17f)
        moveTo(11f, 17f); horizontalLineTo(7f)
    }

    val DirectionsCar = icon("DirectionsCar") {
        moveTo(5f, 17f)
        arcTo(1f, 1f, 0f, true, false, 5f, 15f)
        arcTo(1f, 1f, 0f, false, false, 5f, 17f)
        moveTo(19f, 17f)
        arcTo(1f, 1f, 0f, true, false, 19f, 15f)
        arcTo(1f, 1f, 0f, false, false, 19f, 17f)
        moveTo(3f, 13f); lineTo(5f, 7f); horizontalLineTo(19f); lineTo(21f, 13f)
        verticalLineTo(18f); horizontalLineTo(3f); close()
        moveTo(3f, 18f); verticalLineTo(20f); horizontalLineTo(5f); verticalLineTo(18f)
        moveTo(19f, 18f); verticalLineTo(20f); horizontalLineTo(21f); verticalLineTo(18f)
    }

    val Spa = icon("Spa") {
        moveTo(12f, 22f)
        curveTo(12f, 22f, 4f, 17f, 4f, 11f)
        curveTo(4f, 7f, 7.5f, 5f, 12f, 2f)
        curveTo(16.5f, 5f, 20f, 7f, 20f, 11f)
        curveTo(20f, 17f, 12f, 22f, 12f, 22f)
        moveTo(12f, 22f); verticalLineTo(11f)
    }

    val SmartToy = icon("SmartToy") {
        moveTo(12f, 8f); verticalLineTo(4f)
        moveTo(12f, 4f)
        arcTo(1f, 1f, 0f, true, true, 12f, 2f)
        arcTo(1f, 1f, 0f, false, true, 12f, 4f)
        moveTo(7f, 8f); horizontalLineTo(17f)
        arcTo(2f, 2f, 0f, false, true, 19f, 10f)
        verticalLineTo(18f)
        arcTo(2f, 2f, 0f, false, true, 17f, 20f)
        horizontalLineTo(7f)
        arcTo(2f, 2f, 0f, false, true, 5f, 18f)
        verticalLineTo(10f)
        arcTo(2f, 2f, 0f, false, true, 7f, 8f)
        moveTo(9.5f, 13.5f); horizontalLineTo(9.51f)
        moveTo(14.5f, 13.5f); horizontalLineTo(14.51f)
        moveTo(9f, 17f); horizontalLineTo(15f)
    }

    val NotificationsActive = icon("NotificationsActive") {
        moveTo(12f, 22f)
        arcTo(2f, 2f, 0f, false, false, 14f, 20f)
        horizontalLineTo(10f)
        arcTo(2f, 2f, 0f, false, false, 12f, 22f)
        moveTo(18f, 16f); verticalLineTo(11f)
        arcTo(6f, 6f, 0f, false, false, 6f, 11f)
        verticalLineTo(16f); lineTo(4f, 18f); verticalLineTo(19f); horizontalLineTo(20f); verticalLineTo(18f); close()
        moveTo(12f, 5f); verticalLineTo(2f)
        moveTo(4.22f, 4.93f); lineTo(5.64f, 6.35f)
        moveTo(18.36f, 6.35f); lineTo(19.78f, 4.93f)
    }

    // ── Sección Ajustes ───────────────────────────────────────────────────────

    val Psychology = icon("Psychology") {
        moveTo(13f, 3f)
        arcTo(9f, 9f, 0f, false, false, 4f, 12f)
        horizontalLineTo(1f); lineTo(4.89f, 15.89f); lineTo(4.96f, 16.03f); lineTo(9f, 12f)
        horizontalLineTo(6f)
        arcTo(7f, 7f, 0f, true, true, 13f, 19f)
        verticalLineTo(21f)
        arcTo(9f, 9f, 0f, false, false, 13f, 3f)
        moveTo(11f, 14f); verticalLineTo(10f); lineTo(15f, 12f); close()
    }

    val Speed = icon("Speed") {
        moveTo(12f, 2f)
        arcTo(10f, 10f, 0f, false, false, 2f, 12f)
        horizontalLineTo(4f)
        arcTo(8f, 8f, 0f, false, true, 12f, 4f)
        arcTo(8f, 8f, 0f, false, true, 20f, 12f)
        horizontalLineTo(22f)
        arcTo(10f, 10f, 0f, false, false, 12f, 2f)
        moveTo(12f, 7f)
        arcTo(5f, 5f, 0f, false, false, 7f, 12f)
        arcTo(5f, 5f, 0f, false, false, 12f, 17f)
        arcTo(5f, 5f, 0f, false, false, 17f, 12f)
        arcTo(5f, 5f, 0f, false, false, 12f, 7f)
        moveTo(12f, 9f); lineTo(15f, 12f)
    }

    val Savings = icon("Savings") {
        moveTo(11.8f, 10.9f)
        curveTo(9.53f, 10.31f, 8.8f, 9.7f, 8.8f, 8.75f)
        curveTo(8.8f, 7.66f, 9.81f, 6.9f, 11.5f, 6.9f)
        curveTo(13.28f, 6.9f, 13.94f, 7.75f, 14f, 9f)
        horizontalLineTo(16.21f)
        curveTo(16.14f, 7.28f, 15.09f, 5.7f, 13f, 5.19f)
        verticalLineTo(3f); horizontalLineTo(10f); verticalLineTo(5.16f)
        curveTo(8.06f, 5.58f, 6.5f, 6.84f, 6.5f, 8.77f)
        curveTo(6.5f, 11.08f, 8.41f, 12.23f, 11.2f, 12.9f)
        curveTo(13.7f, 13.5f, 14.2f, 14.38f, 14.2f, 15.31f)
        curveTo(14.2f, 16f, 13.71f, 17.1f, 11.5f, 17.1f)
        curveTo(9.44f, 17.1f, 8.63f, 16.18f, 8.5f, 15f)
        horizontalLineTo(6.3f)
        curveTo(6.44f, 17.19f, 8.08f, 18.42f, 10f, 18.83f)
        verticalLineTo(21f); horizontalLineTo(13f); verticalLineTo(18.85f)
        curveTo(14.95f, 18.48f, 16.5f, 17.35f, 16.5f, 15.3f)
        curveTo(16.5f, 12.46f, 14.07f, 11.49f, 11.8f, 10.9f)
    }

    val Storage = icon("Storage") {
        moveTo(2f, 20f); horizontalLineTo(22f); verticalLineTo(16f); horizontalLineTo(2f); close()
        moveTo(2f, 14f); horizontalLineTo(22f); verticalLineTo(10f); horizontalLineTo(2f); close()
        moveTo(2f, 8f); horizontalLineTo(22f); verticalLineTo(4f); horizontalLineTo(2f); close()
        moveTo(19f, 18f); horizontalLineTo(19.01f)
        moveTo(19f, 12f); horizontalLineTo(19.01f)
        moveTo(19f, 6f); horizontalLineTo(19.01f)
    }

    val Compress = icon("Compress") {
        moveTo(4f, 19f); verticalLineTo(15f); horizontalLineTo(8f)
        moveTo(4f, 19f); lineTo(9f, 14f)
        moveTo(20f, 5f); verticalLineTo(9f); horizontalLineTo(16f)
        moveTo(20f, 5f); lineTo(15f, 10f)
        moveTo(15f, 19f); verticalLineTo(15f); horizontalLineTo(19f)
        moveTo(15f, 19f); lineTo(20f, 14f)
        moveTo(9f, 5f); lineTo(4f, 10f)
        moveTo(4f, 5f); horizontalLineTo(8f); verticalLineTo(9f)
    }

    val Palette = icon("Palette") {
        moveTo(12f, 3f)
        arcTo(9f, 9f, 0f, false, false, 3f, 12f)
        arcTo(9f, 9f, 0f, false, false, 12f, 21f)
        curveTo(12.83f, 21f, 13.5f, 20.33f, 13.5f, 19.5f)
        curveTo(13.5f, 19.11f, 13.35f, 18.76f, 13.11f, 18.49f)
        curveTo(12.88f, 18.23f, 12.73f, 17.88f, 12.73f, 17.5f)
        curveTo(12.73f, 16.67f, 13.4f, 16f, 14.23f, 16f)
        horizontalLineTo(16f)
        arcTo(5f, 5f, 0f, false, false, 21f, 11f)
        arcTo(9f, 9f, 0f, false, false, 12f, 3f)
        moveTo(6.5f, 12f)
        arcTo(1.5f, 1.5f, 0f, true, true, 6.5f, 9f)
        arcTo(1.5f, 1.5f, 0f, false, true, 6.5f, 12f)
        moveTo(9.5f, 8f)
        arcTo(1.5f, 1.5f, 0f, true, true, 9.5f, 5f)
        arcTo(1.5f, 1.5f, 0f, false, true, 9.5f, 8f)
        moveTo(14.5f, 8f)
        arcTo(1.5f, 1.5f, 0f, true, true, 14.5f, 5f)
        arcTo(1.5f, 1.5f, 0f, false, true, 14.5f, 8f)
        moveTo(17.5f, 12f)
        arcTo(1.5f, 1.5f, 0f, true, true, 17.5f, 9f)
        arcTo(1.5f, 1.5f, 0f, false, true, 17.5f, 12f)
    }

    val Visibility = icon("Visibility") {
        moveTo(1f, 12f)
        curveTo(1f, 12f, 5f, 4f, 12f, 4f)
        curveTo(19f, 4f, 23f, 12f, 23f, 12f)
        curveTo(23f, 12f, 19f, 20f, 12f, 20f)
        curveTo(5f, 20f, 1f, 12f, 1f, 12f)
        moveTo(12f, 15f)
        arcTo(3f, 3f, 0f, true, false, 12f, 9f)
        arcTo(3f, 3f, 0f, false, false, 12f, 15f)
    }

    val PowerSettingsNew = icon("Power") {
        moveTo(12f, 2f); verticalLineTo(8f)
        moveTo(8.56f, 4.56f)
        arcTo(8f, 8f, 0f, true, false, 15.44f, 4.56f)
    }

    val Extension = icon("Extension") {
        moveTo(20.5f, 11f); horizontalLineTo(19f); verticalLineTo(7f)
        arcTo(2f, 2f, 0f, false, false, 17f, 5f)
        horizontalLineTo(13f); verticalLineTo(3.5f)
        arcTo(2.5f, 2.5f, 0f, false, false, 8f, 3.5f)
        verticalLineTo(5f); horizontalLineTo(4f)
        arcTo(2f, 2f, 0f, false, false, 2f, 7f)
        verticalLineTo(11.5f); horizontalLineTo(3.5f)
        arcTo(2.5f, 2.5f, 0f, false, true, 3.5f, 16.5f)
        horizontalLineTo(2f); verticalLineTo(21f)
        arcTo(2f, 2f, 0f, false, false, 4f, 23f)
        horizontalLineTo(8.5f); verticalLineTo(21.5f)
        arcTo(2.5f, 2.5f, 0f, false, true, 13.5f, 21.5f)
        verticalLineTo(23f); horizontalLineTo(18f)
        arcTo(2f, 2f, 0f, false, false, 20f, 21f)
        verticalLineTo(17f); horizontalLineTo(21.5f)
        arcTo(2.5f, 2.5f, 0f, false, false, 21.5f, 11.5f)
    }

    val Alarm = icon("Alarm") {
        moveTo(12f, 22f)
        arcTo(8f, 8f, 0f, true, false, 12f, 6f)
        arcTo(8f, 8f, 0f, false, false, 12f, 22f)
        moveTo(12f, 10f); verticalLineTo(14f); lineTo(15f, 16f)
        moveTo(4f, 4f); lineTo(1f, 7f)
        moveTo(23f, 7f); lineTo(20f, 4f)
    }

    val AutoAwesome = icon("AutoAwesome") {
        moveTo(19f, 9f); lineTo(21f, 5f); lineTo(17f, 3f); lineTo(19f, 9f)
        moveTo(5f, 15f); lineTo(3f, 19f); lineTo(7f, 21f); lineTo(5f, 15f)
        moveTo(12.67f, 5.33f)
        lineTo(6.67f, 18.67f)
        moveTo(18.67f, 6f)
        lineTo(5.33f, 18f)
        moveTo(15f, 3f); verticalLineTo(7f)
        moveTo(3f, 9f); horizontalLineTo(7f)
        moveTo(17f, 17f); horizontalLineTo(21f)
        moveTo(9f, 17f); verticalLineTo(21f)
    }

    val Bolt = icon("Bolt") {
        moveTo(13f, 2f); lineTo(3f, 14f); horizontalLineTo(12f); lineTo(11f, 22f)
        lineTo(21f, 10f); horizontalLineTo(12f); close()
    }

    val Build = icon("Build") {
        moveTo(20.7f, 7.04f)
        curveTo(21.1f, 5.56f, 20.69f, 3.89f, 19.5f, 2.7f)
        curveTo(18.37f, 1.57f, 16.79f, 1.13f, 15.36f, 1.44f)
        lineTo(17.93f, 4f)
        lineTo(16.51f, 5.41f)
        lineTo(13.95f, 2.85f)
        curveTo(13.63f, 4.28f, 14.08f, 5.86f, 15.21f, 6.99f)
        curveTo(16.38f, 8.15f, 18.02f, 8.6f, 19.5f, 8.24f)
        lineTo(20.7f, 7.04f)
        moveTo(6.29f, 17.71f)
        lineTo(3f, 21f); lineTo(2f, 20f); lineTo(5.29f, 16.71f)
        moveTo(3.17f, 9.83f); lineTo(8f, 5f)
        curveTo(8.39f, 4.61f, 9.02f, 4.61f, 9.41f, 5f)
        lineTo(11.41f, 7f)
        lineTo(13f, 5.41f); lineTo(14.41f, 6.83f)
        lineTo(13f, 8.24f)
        lineTo(14f, 9.24f)
        lineTo(5.99f, 17.24f)
        lineTo(3.17f, 14.41f)
        curveTo(2.78f, 14.02f, 2.78f, 13.39f, 3.17f, 13f)
        lineTo(3.17f, 9.83f)
    }

    val Cloud = icon("Cloud") {
        moveTo(18f, 10f)
        horizontalLineTo(16.74f)
        arcTo(6f, 6f, 0f, false, false, 5f, 13f)
        arcTo(4f, 4f, 0f, false, false, 6f, 21f)
        horizontalLineTo(18f)
        arcTo(5f, 5f, 0f, false, false, 18f, 11f)
        verticalLineTo(10f)
    }

    val Navigation = icon("Navigation") {
        moveTo(12f, 2f); lineTo(19f, 21f); lineTo(12f, 17f); lineTo(5f, 21f); close()
    }

    val Chat = icon("Chat") {
        moveTo(21f, 15f)
        arcTo(2f, 2f, 0f, false, true, 19f, 17f)
        horizontalLineTo(7f)
        lineTo(3f, 21f)
        verticalLineTo(5f)
        arcTo(2f, 2f, 0f, false, true, 5f, 3f)
        horizontalLineTo(19f)
        arcTo(2f, 2f, 0f, false, true, 21f, 5f)
        close()
        moveTo(8f, 10f); horizontalLineTo(16f)
        moveTo(8f, 14f); horizontalLineTo(12f)
    }

    val Accessibility = icon("Accessibility") {
        moveTo(12f, 2f)
        arcTo(1f, 1f, 0f, true, false, 12f, 0f)
        arcTo(1f, 1f, 0f, false, false, 12f, 2f)
        moveTo(17f, 6f); horizontalLineTo(7f); lineTo(5f, 10f); lineTo(8f, 10f); lineTo(9f, 15f)
        lineTo(7f, 22f); lineTo(9f, 22f); lineTo(12f, 17f); lineTo(15f, 22f); lineTo(17f, 22f)
        lineTo(15f, 15f); lineTo(16f, 10f); lineTo(19f, 10f); close()
    }

    val EventNote = icon("EventNote") {
        moveTo(7f, 1f); verticalLineTo(3f)
        moveTo(17f, 1f); verticalLineTo(3f)
        moveTo(3f, 8f); horizontalLineTo(21f)
        moveTo(5f, 3f); horizontalLineTo(19f)
        arcTo(2f, 2f, 0f, false, true, 21f, 5f)
        verticalLineTo(21f)
        arcTo(2f, 2f, 0f, false, true, 19f, 23f)
        horizontalLineTo(5f)
        arcTo(2f, 2f, 0f, false, true, 3f, 21f)
        verticalLineTo(5f)
        arcTo(2f, 2f, 0f, false, true, 5f, 3f)
        moveTo(7f, 12f); horizontalLineTo(17f)
        moveTo(7f, 16f); horizontalLineTo(12f)
    }

    val Timer = icon("Timer") {
        moveTo(12f, 2f); horizontalLineTo(10f); verticalLineTo(4f); horizontalLineTo(14f); verticalLineTo(2f)
        moveTo(13f, 4f)
        arcTo(8f, 8f, 0f, true, false, 13f, 20f)
        arcTo(8f, 8f, 0f, false, false, 13f, 4f)
        moveTo(13f, 8f); verticalLineTo(13f); lineTo(15.5f, 15.5f)
        moveTo(20.24f, 5.06f); lineTo(18.24f, 3.06f); lineTo(19.66f, 4.48f)
    }

    val HourglassEmpty = icon("Hourglass") {
        moveTo(6f, 2f); horizontalLineTo(18f)
        moveTo(6f, 22f); horizontalLineTo(18f)
        moveTo(8f, 2f); verticalLineTo(6f); lineTo(12f, 12f); lineTo(8f, 18f); verticalLineTo(22f)
        moveTo(16f, 2f); verticalLineTo(6f); lineTo(12f, 12f); lineTo(16f, 18f); verticalLineTo(22f)
    }

    val Event = icon("Event") {
        moveTo(8f, 2f); verticalLineTo(4f)
        moveTo(16f, 2f); verticalLineTo(4f)
        moveTo(3f, 8f); horizontalLineTo(21f)
        moveTo(5f, 4f); horizontalLineTo(19f)
        arcTo(2f, 2f, 0f, false, true, 21f, 6f)
        verticalLineTo(20f)
        arcTo(2f, 2f, 0f, false, true, 19f, 22f)
        horizontalLineTo(5f)
        arcTo(2f, 2f, 0f, false, true, 3f, 20f)
        verticalLineTo(6f)
        arcTo(2f, 2f, 0f, false, true, 5f, 4f)
        moveTo(16f, 14f)
        arcTo(2f, 2f, 0f, true, false, 16f, 10f)
        arcTo(2f, 2f, 0f, false, false, 16f, 14f)
    }

    val PlayCircleFilled = icon("PlayCircle") {
        moveTo(12f, 22f)
        arcTo(10f, 10f, 0f, true, false, 12f, 2f)
        arcTo(10f, 10f, 0f, false, false, 12f, 22f)
        moveTo(10f, 8f); lineTo(16f, 12f); lineTo(10f, 16f); close()
    }

    val PauseCircleFilled = icon("PauseCircle") {
        moveTo(12f, 22f)
        arcTo(10f, 10f, 0f, true, false, 12f, 2f)
        arcTo(10f, 10f, 0f, false, false, 12f, 22f)
        moveTo(10f, 15f); verticalLineTo(9f)
        moveTo(14f, 15f); verticalLineTo(9f)
    }

    val VerticalAlignBottom = icon("AlignBottom") {
        moveTo(16f, 13f); lineTo(12f, 17f); lineTo(8f, 13f)
        moveTo(12f, 3f); verticalLineTo(17f)
        moveTo(3f, 21f); horizontalLineTo(21f)
    }

    val VerticalAlignCenter = icon("AlignCenter") {
        moveTo(8f, 15f); lineTo(12f, 19f); lineTo(16f, 15f)
        moveTo(8f, 9f); lineTo(12f, 5f); lineTo(16f, 9f)
        moveTo(3f, 12f); horizontalLineTo(21f)
    }

    val FileDownload = icon("Download") {
        moveTo(21f, 15f); verticalLineTo(19f)
        arcTo(2f, 2f, 0f, false, true, 19f, 21f)
        horizontalLineTo(5f)
        arcTo(2f, 2f, 0f, false, true, 3f, 19f)
        verticalLineTo(15f)
        moveTo(7f, 10f); lineTo(12f, 15f); lineTo(17f, 10f)
        moveTo(12f, 15f); verticalLineTo(3f)
    }

    val DeleteSweep = icon("DeleteSweep") {
        moveTo(15f, 16f); horizontalLineTo(19f); verticalLineTo(18f); horizontalLineTo(15f); close()
        moveTo(15f, 8f); horizontalLineTo(19f); verticalLineTo(10f); horizontalLineTo(15f); close()
        moveTo(15f, 12f); horizontalLineTo(19f); verticalLineTo(14f); horizontalLineTo(15f); close()
        moveTo(3f, 18f); lineTo(11f, 18f); lineTo(11f, 16f); lineTo(3f, 16f); close()
        moveTo(11.83f, 10f); lineTo(9.41f, 7.56f); lineTo(8f, 8.97f); lineTo(11.03f, 12f)
        lineTo(8f, 15.03f); lineTo(9.41f, 16.44f); lineTo(13.5f, 12.35f); close()
    }

    val Terminal = icon("Terminal") {
        moveTo(8f, 9f); lineTo(4f, 12f); lineTo(8f, 15f)
        moveTo(16f, 9f); lineTo(20f, 12f); lineTo(16f, 15f)
        moveTo(3f, 3f); horizontalLineTo(21f)
        arcTo(2f, 2f, 0f, false, true, 23f, 5f)
        verticalLineTo(19f)
        arcTo(2f, 2f, 0f, false, true, 21f, 21f)
        horizontalLineTo(3f)
        arcTo(2f, 2f, 0f, false, true, 1f, 19f)
        verticalLineTo(5f)
        arcTo(2f, 2f, 0f, false, true, 3f, 3f)
        moveTo(11f, 12f); horizontalLineTo(13f)
    }

    val Book = icon("Book") {
        moveTo(4f, 2f)
        horizontalLineTo(20f)
        arcTo(2f, 2f, 0f, false, true, 22f, 4f)
        verticalLineTo(20f)
        arcTo(2f, 2f, 0f, false, true, 20f, 22f)
        horizontalLineTo(4f)
        arcTo(2f, 2f, 0f, false, true, 2f, 20f)
        verticalLineTo(4f)
        arcTo(2f, 2f, 0f, false, true, 4f, 2f)
        moveTo(12f, 2f); verticalLineTo(22f)
        moveTo(7f, 7f); horizontalLineTo(9f)
        moveTo(7f, 11f); horizontalLineTo(9f)
        moveTo(7f, 15f); horizontalLineTo(9f)
        moveTo(15f, 7f); horizontalLineTo(17f)
        moveTo(15f, 11f); horizontalLineTo(17f)
        moveTo(15f, 15f); horizontalLineTo(17f)
    }

    val Assistant = icon("Assistant") {
        moveTo(19f, 1f)
        horizontalLineTo(5f)
        arcTo(2f, 2f, 0f, false, false, 3f, 3f)
        verticalLineTo(15f)
        arcTo(2f, 2f, 0f, false, false, 5f, 17f)
        horizontalLineTo(9f)
        lineTo(12f, 20f); lineTo(15f, 17f); horizontalLineTo(19f)
        arcTo(2f, 2f, 0f, false, false, 21f, 15f)
        verticalLineTo(3f)
        arcTo(2f, 2f, 0f, false, false, 19f, 1f)
        moveTo(12f, 5f); lineTo(13.5f, 8.5f); lineTo(17f, 10f)
        lineTo(13.5f, 11.5f); lineTo(12f, 15f); lineTo(10.5f, 11.5f)
        lineTo(7f, 10f); lineTo(10.5f, 8.5f); close()
    }

    // Alias para Memorias
    val Place    = icon("Place") {
        moveTo(12f, 2f)
        arcTo(7f, 7f, 0f, false, false, 5f, 9f)
        curveTo(5f, 14.25f, 12f, 22f, 12f, 22f)
        curveTo(12f, 22f, 19f, 14.25f, 19f, 9f)
        arcTo(7f, 7f, 0f, false, false, 12f, 2f)
        moveTo(12f, 11.5f)
        arcTo(2.5f, 2.5f, 0f, true, false, 12f, 6.5f)
        arcTo(2.5f, 2.5f, 0f, false, false, 12f, 11.5f)
    }
    val Apps     = icon("Apps") {
        moveTo(4f, 4f); horizontalLineTo(8f); verticalLineTo(8f); horizontalLineTo(4f); close()
        moveTo(10f, 4f); horizontalLineTo(14f); verticalLineTo(8f); horizontalLineTo(10f); close()
        moveTo(16f, 4f); horizontalLineTo(20f); verticalLineTo(8f); horizontalLineTo(16f); close()
        moveTo(4f, 10f); horizontalLineTo(8f); verticalLineTo(14f); horizontalLineTo(4f); close()
        moveTo(10f, 10f); horizontalLineTo(14f); verticalLineTo(14f); horizontalLineTo(10f); close()
        moveTo(16f, 10f); horizontalLineTo(20f); verticalLineTo(14f); horizontalLineTo(16f); close()
        moveTo(4f, 16f); horizontalLineTo(8f); verticalLineTo(20f); horizontalLineTo(4f); close()
        moveTo(10f, 16f); horizontalLineTo(14f); verticalLineTo(20f); horizontalLineTo(10f); close()
        moveTo(16f, 16f); horizontalLineTo(20f); verticalLineTo(20f); horizontalLineTo(16f); close()
    }
    val Badge    = icon("Badge") {
        moveTo(12f, 10f)
        arcTo(3f, 3f, 0f, true, false, 12f, 4f)
        arcTo(3f, 3f, 0f, false, false, 12f, 10f)
        moveTo(12f, 12f)
        curveTo(8.5f, 12f, 6f, 14f, 6f, 16.5f)
        verticalLineTo(18f); horizontalLineTo(18f); verticalLineTo(16.5f)
        curveTo(18f, 14f, 15.5f, 12f, 12f, 12f)
        moveTo(2f, 2f); horizontalLineTo(22f)
        arcTo(2f, 2f, 0f, false, true, 24f, 4f)
        verticalLineTo(20f)
        arcTo(2f, 2f, 0f, false, true, 22f, 22f)
        horizontalLineTo(2f)
        arcTo(2f, 2f, 0f, false, true, 0f, 20f)
        verticalLineTo(4f)
        arcTo(2f, 2f, 0f, false, true, 2f, 2f)
    }
    val Tune     = icon("Tune") {
        moveTo(3f, 18f); horizontalLineTo(9f)
        moveTo(15f, 18f); horizontalLineTo(21f)
        moveTo(3f, 12f); horizontalLineTo(7f)
        moveTo(13f, 12f); horizontalLineTo(21f)
        moveTo(3f, 6f); horizontalLineTo(15f)
        moveTo(19f, 6f); horizontalLineTo(21f)
        moveTo(12f, 20f); verticalLineTo(16f)
        moveTo(10f, 14f)
        arcTo(2f, 2f, 0f, true, true, 10f, 10f)
        arcTo(2f, 2f, 0f, false, true, 10f, 14f)
        moveTo(18f, 8f)
        arcTo(2f, 2f, 0f, true, true, 18f, 4f)
        arcTo(2f, 2f, 0f, false, true, 18f, 8f)
        moveTo(6f, 22f)
        arcTo(2f, 2f, 0f, true, true, 6f, 18f)
        arcTo(2f, 2f, 0f, false, true, 6f, 22f)
    }
    val MoreHoriz = icon("MoreHoriz") {
        moveTo(12f, 13f)
        arcTo(1f, 1f, 0f, true, false, 12f, 11f)
        arcTo(1f, 1f, 0f, false, false, 12f, 13f)
        moveTo(19f, 13f)
        arcTo(1f, 1f, 0f, true, false, 19f, 11f)
        arcTo(1f, 1f, 0f, false, false, 19f, 13f)
        moveTo(5f, 13f)
        arcTo(1f, 1f, 0f, true, false, 5f, 11f)
        arcTo(1f, 1f, 0f, false, false, 5f, 13f)
    }
    val Restaurant = icon("Restaurant") {
        moveTo(11f, 9f)
        horizontalLineTo(9f)
        verticalLineTo(2f)
        horizontalLineTo(7f)
        verticalLineTo(9f)
        horizontalLineTo(5f)
        verticalLineTo(2f)
        horizontalLineTo(3f)
        verticalLineTo(9f)
        curveTo(3f, 11.12f, 4.66f, 12.84f, 6.75f, 12.97f)
        verticalLineTo(22f)
        horizontalLineTo(9.25f)
        verticalLineTo(12.97f)
        curveTo(11.34f, 12.84f, 13f, 11.12f, 13f, 9f)
        verticalLineTo(2f)
        horizontalLineTo(11f)
        close()
        moveTo(16f, 6.09f)
        verticalLineTo(2f)
        horizontalLineTo(14f)
        verticalLineTo(22f)
        horizontalLineTo(16.5f)
        verticalLineTo(14f)
        horizontalLineTo(19f)
        curveTo(20.1f, 14f, 21f, 13.1f, 21f, 12f)
        verticalLineTo(8f)
        curveTo(21f, 6.79f, 20.07f, 5.8f, 18.88f, 5.68f)
        lineTo(16f, 6.09f)
    }

    val MusicNote = icon("MusicNote") {
        moveTo(9f, 18f)
        arcTo(3f, 3f, 0f, true, false, 9f, 12f)
        arcTo(3f, 3f, 0f, false, false, 9f, 18f)
        moveTo(9f, 15f); verticalLineTo(5f); lineTo(21f, 3f); verticalLineTo(13f)
        moveTo(18f, 16f)
        arcTo(3f, 3f, 0f, true, false, 18f, 10f)
        arcTo(3f, 3f, 0f, false, false, 18f, 16f)
    }

    val Code = icon("Code") {
        moveTo(16f, 18f); lineTo(22f, 12f); lineTo(16f, 6f)
        moveTo(8f, 6f); lineTo(2f, 12f); lineTo(8f, 18f)
    }

    val BatteryAlert = icon("BatteryAlert") {
        moveTo(15.67f, 4f)
        horizontalLineTo(14f)
        verticalLineTo(2f)
        horizontalLineTo(10f)
        verticalLineTo(4f)
        horizontalLineTo(8.33f)
        arcTo(1.33f, 1.33f, 0f, false, false, 7f, 5.33f)
        verticalLineTo(22.67f)
        arcTo(1.33f, 1.33f, 0f, false, false, 8.33f, 24f)
        horizontalLineTo(15.67f)
        arcTo(1.33f, 1.33f, 0f, false, false, 17f, 22.67f)
        verticalLineTo(5.33f)
        arcTo(1.33f, 1.33f, 0f, false, false, 15.67f, 4f)
        moveTo(12f, 20f)
        arcTo(1.5f, 1.5f, 0f, true, false, 12f, 17f)
        arcTo(1.5f, 1.5f, 0f, false, false, 12f, 20f)
        moveTo(12f, 15f); verticalLineTo(8f)
    }

    val FileDownload2 = FileDownload  // alias
    val CodeIcon      = Code
    val MusicNoteIcon = MusicNote

    // ── Iconos del sistema ─────────────────────────────────────────────────────
    val VolumeUp = icon("VolumeUp") {
        moveTo(3f, 9f); lineTo(7f, 9f); lineTo(12f, 5f); lineTo(12f, 19f); lineTo(7f, 15f); lineTo(3f, 15f); close()
        moveTo(16f, 9f); curveTo(17.5f, 10.5f, 17.5f, 13.5f, 16f, 15f)
        moveTo(19f, 7f); curveTo(21.7f, 9.7f, 21.7f, 14.3f, 19f, 17f)
    }
    val BrightnessHigh = icon("BrightnessHigh") {
        moveTo(12f, 2f); lineTo(12f, 4f)
        moveTo(12f, 20f); lineTo(12f, 22f)
        moveTo(4.22f, 4.22f); lineTo(5.64f, 5.64f)
        moveTo(18.36f, 18.36f); lineTo(19.78f, 19.78f)
        moveTo(2f, 12f); lineTo(4f, 12f)
        moveTo(20f, 12f); lineTo(22f, 12f)
        moveTo(4.22f, 19.78f); lineTo(5.64f, 18.36f)
        moveTo(18.36f, 5.64f); lineTo(19.78f, 4.22f)
        moveTo(12f, 8f); arcTo(4f, 4f, 0f, false, true, 12f, 16f); arcTo(4f, 4f, 0f, false, true, 12f, 8f)
    }
    val Wifi = icon("Wifi") {
        moveTo(5f, 12.55f); arcTo(11f, 11f, 0f, false, true, 19f, 12.55f)
        moveTo(1.42f, 9f); arcTo(16f, 16f, 0f, false, true, 22.58f, 9f)
        moveTo(8.53f, 16.11f); arcTo(6f, 6f, 0f, false, true, 15.47f, 16.11f)
        moveTo(12f, 20f); lineTo(12f, 20f); arcTo(0.5f, 0.5f, 0f, false, true, 12f, 20f)
    }
    val Bluetooth = icon("Bluetooth") {
        moveTo(6.5f, 6.5f); lineTo(17.5f, 17.5f); lineTo(12f, 23f); lineTo(12f, 1f); lineTo(17.5f, 6.5f); lineTo(6.5f, 17.5f)
    }
    val Battery = icon("Battery") {
        moveTo(17f, 5f); lineTo(7f, 5f); arcTo(2f, 2f, 0f, false, false, 5f, 7f); lineTo(5f, 19f); arcTo(2f, 2f, 0f, false, false, 7f, 21f); lineTo(17f, 21f); arcTo(2f, 2f, 0f, false, false, 19f, 19f); lineTo(19f, 7f); arcTo(2f, 2f, 0f, false, false, 17f, 5f)
        moveTo(17f, 4f); lineTo(17f, 3f); arcTo(1f, 1f, 0f, false, false, 16f, 2f); lineTo(8f, 2f); arcTo(1f, 1f, 0f, false, false, 7f, 3f); lineTo(7f, 4f)
    }
    val Language = icon("Language") {
        moveTo(12f, 2f); arcTo(10f, 10f, 0f, false, true, 12f, 22f); arcTo(10f, 10f, 0f, false, true, 12f, 2f)
        moveTo(2f, 12f); lineTo(22f, 12f)
        moveTo(12f, 2f); curveTo(14.5f, 7f, 14.5f, 17f, 12f, 22f)
        moveTo(12f, 2f); curveTo(9.5f, 7f, 9.5f, 17f, 12f, 22f)
    }

    // ── Helper builder ─────────────────────────────────────────────────────────

    private fun icon(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name           = name,
            defaultWidth   = 24.dp,
            defaultHeight  = 24.dp,
            viewportWidth  = 24f,
            viewportHeight = 24f
        ).path(
            stroke          = S,
            strokeLineWidth = W,
            strokeLineCap   = StrokeCap.Round,
            strokeLineJoin  = StrokeJoin.Round,
            fill            = SolidColor(Color.Transparent)
        ) { block() }.build()
}
