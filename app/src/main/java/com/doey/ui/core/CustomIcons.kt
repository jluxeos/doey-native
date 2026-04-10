package com.doey.ui.core

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Sistema de Iconos Lineales Minimalistas para Doey
 * Reemplaza los iconos de Material Design con un estilo propio.
 */
object CustomIcons {
    private val DefaultColor = Color(0xFF1A1A1A)
    private const val DefaultStrokeWidth = 1.5f

    val Menu = ImageVector.Builder(
        name = "Menu",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(DefaultColor),
        strokeLineWidth = DefaultStrokeWidth,
        strokeLineCap = StrokeCap.Round
    ) {
        moveTo(4f, 7f)
        lineTo(20f, 7f)
        moveTo(4f, 12f)
        lineTo(20f, 12f)
        moveTo(4f, 17f)
        lineTo(14f, 17f)
    }.build()

    val Person = ImageVector.Builder(
        name = "Person",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(DefaultColor),
        strokeLineWidth = DefaultStrokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 11f)
        arcTo(4f, 4f, 0f, true, false, 12f, 3f)
        arcTo(4f, 4f, 0f, false, false, 12f, 11f)
        close()
        moveTo(6f, 21f)
        verticalLineTo(19f)
        arcTo(4f, 4f, 0f, false, true, 10f, 15f)
        horizontalLineTo(14f)
        arcTo(4f, 4f, 0f, false, true, 18f, 19f)
        verticalLineTo(21f)
    }.build()

    val Home = ImageVector.Builder(
        name = "Home",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(DefaultColor),
        strokeLineWidth = DefaultStrokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(3f, 9f)
        lineTo(12f, 2f)
        lineTo(21f, 9f)
        verticalLineTo(20f)
        arcTo(2f, 2f, 0f, false, true, 19f, 22f)
        horizontalLineTo(5f)
        arcTo(2f, 2f, 0f, false, true, 3f, 20f)
        close()
        moveTo(9f, 22f)
        verticalLineTo(12f)
        horizontalLineTo(15f)
        verticalLineTo(22f)
    }.build()

    val Star = ImageVector.Builder(
        name = "Star",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(DefaultColor),
        strokeLineWidth = DefaultStrokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 2f)
        lineTo(15f, 8.5f)
        lineTo(22f, 9.5f)
        lineTo(17f, 14.5f)
        lineTo(18.5f, 21.5f)
        lineTo(12f, 18.5f)
        lineTo(5.5f, 21.5f)
        lineTo(7f, 14.5f)
        lineTo(2f, 9.5f)
        lineTo(9f, 8.5f)
        close()
    }.build()

    val Clock = ImageVector.Builder(
        name = "Clock",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(DefaultColor),
        strokeLineWidth = DefaultStrokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 12f)
        moveTo(12f, 21f)
        arcTo(9f, 9f, 0f, true, true, 12f, 3f)
        arcTo(9f, 9f, 0f, false, true, 12f, 21f)
        close()
        moveTo(12f, 7f)
        verticalLineTo(12f)
        lineTo(15f, 15f)
    }.build()

    val Message = ImageVector.Builder(
        name = "Message",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(DefaultColor),
        strokeLineWidth = DefaultStrokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(21f, 15f)
        arcTo(2f, 2f, 0f, false, true, 19f, 17f)
        horizontalLineTo(7f)
        lineTo(3f, 21f)
        verticalLineTo(5f)
        arcTo(2f, 2f, 0f, false, true, 5f, 3f)
        horizontalLineTo(19f)
        arcTo(2f, 2f, 0f, false, true, 21f, 5f)
        close()
    }.build()

    val Settings = ImageVector.Builder(
        name = "Settings",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(DefaultColor),
        strokeLineWidth = DefaultStrokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 15f)
        arcTo(3f, 3f, 0f, true, false, 12f, 9f)
        arcTo(3f, 3f, 0f, false, false, 12f, 15f)
        close()
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
    }.build()

    val Send = ImageVector.Builder(
        name = "Send",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(DefaultColor),
        strokeLineWidth = DefaultStrokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(22f, 2f)
        lineTo(11f, 13f)
        moveTo(22f, 2f)
        lineTo(15f, 22f)
        lineTo(11f, 13f)
        lineTo(2f, 9f)
        lineTo(22f, 2f)
        close()
    }.build()

    val Mic = ImageVector.Builder(
        name = "Mic",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(DefaultColor),
        strokeLineWidth = DefaultStrokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 1f)
        arcTo(3f, 3f, 0f, false, false, 9f, 4f)
        verticalLineTo(11f)
        arcTo(3f, 3f, 0f, false, false, 15f, 11f)
        verticalLineTo(4f)
        arcTo(3f, 3f, 0f, false, false, 12f, 1f)
        close()
        moveTo(19f, 11f)
        arcTo(7f, 7f, 0f, false, true, 5f, 11f)
        moveTo(12f, 18f)
        verticalLineTo(22f)
    }.build()

    val SmartToy = ImageVector.Builder(
        name = "SmartToy",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(DefaultColor),
        strokeLineWidth = DefaultStrokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 8f)
        verticalLineTo(4f)
        moveTo(12f, 4f)
        arcTo(1f, 1f, 0f, true, true, 12f, 2f)
        arcTo(1f, 1f, 0f, false, true, 12f, 4f)
        close()
        moveTo(7f, 8f)
        horizontalLineTo(17f)
        arcTo(2f, 2f, 0f, false, true, 19f, 10f)
        verticalLineTo(18f)
        arcTo(2f, 2f, 0f, false, true, 17f, 20f)
        horizontalLineTo(7f)
        arcTo(2f, 2f, 0f, false, true, 5f, 18f)
        verticalLineTo(10f)
        arcTo(2f, 2f, 0f, false, true, 7f, 8f)
        close()
        moveTo(9f, 13f)
        horizontalLineTo(9.01f)
        moveTo(15f, 13f)
        horizontalLineTo(15.01f)
        moveTo(8f, 17f)
        horizontalLineTo(16f)
    }.build()

    val LibraryBooks = ImageVector.Builder(
        name = "LibraryBooks",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(DefaultColor),
        strokeLineWidth = DefaultStrokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(4f, 7f)
        verticalLineTo(19f)
        arcTo(2f, 2f, 0f, false, false, 6f, 21f)
        horizontalLineTo(18f)
        moveTo(8f, 3f)
        horizontalLineTo(20f)
        arcTo(2f, 2f, 0f, false, true, 22f, 5f)
        verticalLineTo(15f)
        arcTo(2f, 2f, 0f, false, true, 20f, 17f)
        horizontalLineTo(8f)
        arcTo(2f, 2f, 0f, false, true, 6f, 15f)
        verticalLineTo(5f)
        arcTo(2f, 2f, 0f, false, true, 8f, 3f)
        close()
        moveTo(10f, 7f)
        horizontalLineTo(18f)
        moveTo(10f, 11f)
        horizontalLineTo(18f)
        moveTo(10f, 15f)
        horizontalLineTo(14f)
    }.build()

    val Lock = ImageVector.Builder(
        name = "Lock",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(DefaultColor),
        strokeLineWidth = DefaultStrokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(19f, 11f)
        horizontalLineTo(5f)
        arcTo(2f, 2f, 0f, false, false, 3f, 13f)
        verticalLineTo(19f)
        arcTo(2f, 2f, 0f, false, false, 5f, 21f)
        horizontalLineTo(19f)
        arcTo(2f, 2f, 0f, false, false, 21f, 19f)
        verticalLineTo(13f)
        arcTo(2f, 2f, 0f, false, false, 19f, 11f)
        close()
        moveTo(7f, 11f)
        verticalLineTo(7f)
        arcTo(5f, 5f, 0f, false, true, 17f, 7f)
        verticalLineTo(11f)
    }.build()

    val BugReport = ImageVector.Builder(
        name = "BugReport",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(DefaultColor),
        strokeLineWidth = DefaultStrokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 7f)
        verticalLineTo(20f)
        moveTo(12f, 7f)
        arcTo(4f, 4f, 0f, false, true, 16f, 11f)
        verticalLineTo(16f)
        arcTo(4f, 4f, 0f, false, true, 12f, 20f)
        arcTo(4f, 4f, 0f, false, true, 8f, 16f)
        verticalLineTo(11f)
        arcTo(4f, 4f, 0f, false, true, 12f, 7f)
        close()
        moveTo(12f, 7f)
        lineTo(12f, 5f)
        moveTo(16f, 11f)
        lineTo(19f, 9f)
        moveTo(16f, 16f)
        lineTo(19f, 18f)
        moveTo(8f, 11f)
        lineTo(5f, 9f)
        moveTo(8f, 16f)
        lineTo(5f, 18f)
    }.build()

    val AccountTree = ImageVector.Builder(
        name = "AccountTree",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(DefaultColor),
        strokeLineWidth = DefaultStrokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(22f, 11f)
        verticalLineTo(3f)
        horizontalLineTo(15f)
        verticalLineTo(11f)
        horizontalLineTo(22f)
        close()
        moveTo(7f, 21f)
        verticalLineTo(13f)
        horizontalLineTo(2f)
        verticalLineTo(21f)
        horizontalLineTo(7f)
        close()
        moveTo(22f, 21f)
        verticalLineTo(13f)
        horizontalLineTo(15f)
        verticalLineTo(21f)
        horizontalLineTo(22f)
        close()
        moveTo(15f, 7f)
        horizontalLineTo(11f)
        verticalLineTo(17f)
        horizontalLineTo(15f)
        moveTo(11f, 17f)
        horizontalLineTo(7f)
    }.build()

    val DirectionsCar = ImageVector.Builder(
        name = "DirectionsCar",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(DefaultColor),
        strokeLineWidth = DefaultStrokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(18.92f, 6.01f)
        curveTo(18.72f, 5.42f, 18.16f, 5f, 17.5f, 5f)
        horizontalLineTo(6.5f)
        curveTo(5.84f, 5f, 5.29f, 5.42f, 5.08f, 6.01f)
        lineTo(3f, 12f)
        verticalLineTo(20f)
        arcTo(1f, 1f, 0f, false, false, 4f, 21f)
        horizontalLineTo(5f)
        arcTo(1f, 1f, 0f, false, false, 6f, 20f)
        verticalLineTo(19f)
        horizontalLineTo(18f)
        verticalLineTo(20f)
        arcTo(1f, 1f, 0f, false, false, 19f, 21f)
        horizontalLineTo(20f)
        arcTo(1f, 1f, 0f, false, false, 21f, 20f)
        verticalLineTo(12f)
        lineTo(18.92f, 6.01f)
        close()
        moveTo(6.5f, 7f)
        horizontalLineTo(17.5f)
        lineTo(18.83f, 11f)
        horizontalLineTo(5.17f)
        lineTo(6.5f, 7f)
        close()
        moveTo(7.5f, 16f)
        arcTo(1.5f, 1.5f, 0f, true, true, 7.5f, 13f)
        arcTo(1.5f, 1.5f, 0f, false, true, 7.5f, 16f)
        close()
        moveTo(16.5f, 16f)
        arcTo(1.5f, 1.5f, 0f, true, true, 16.5f, 13f)
        arcTo(1.5f, 1.5f, 0f, false, true, 16.5f, 16f)
        close()
    }.build()

    val Spa = ImageVector.Builder(
        name = "Spa",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(DefaultColor),
        strokeLineWidth = DefaultStrokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 22f)
        curveTo(12f, 22f, 4f, 18f, 4f, 12f)
        curveTo(4f, 8f, 7f, 5f, 12f, 2f)
        curveTo(17f, 5f, 20f, 8f, 20f, 12f)
        curveTo(20f, 18f, 12f, 22f, 12f, 22f)
        close()
        moveTo(12f, 22f)
        verticalLineTo(12f)
    }.build()

    val NotificationsActive = ImageVector.Builder(
        name = "NotificationsActive",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(DefaultColor),
        strokeLineWidth = DefaultStrokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 22f)
        arcTo(2f, 2f, 0f, false, false, 14f, 20f)
        horizontalLineTo(10f)
        arcTo(2f, 2f, 0f, false, false, 12f, 22f)
        close()
        moveTo(18f, 16f)
        verticalLineTo(11f)
        arcTo(6f, 6f, 0f, false, false, 12f, 5f)
        arcTo(6f, 6f, 0f, false, false, 6f, 11f)
        verticalLineTo(16f)
        lineTo(4f, 18f)
        verticalLineTo(19f)
        horizontalLineTo(20f)
        verticalLineTo(18f)
        lineTo(18f, 16f)
        close()
        moveTo(12f, 5f)
        verticalLineTo(2f)
    }.build()
}
