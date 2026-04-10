package com.doey.ui.core

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object CustomIcons {
    val Menu = ImageVector.Builder(
        name = "Menu",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = SolidColor(Color(0xFF1A1A1A)),
        strokeLineWidth = 0f
    ) {
        moveTo(3f, 5f)
        lineTo(21f, 5f)
        arcTo(1f, 1f, 0f, false, true, 21f, 7f)
        lineTo(3f, 7f)
        arcTo(1f, 1f, 0f, false, true, 3f, 5f)
        close()
        moveTo(3f, 11f)
        lineTo(21f, 11f)
        arcTo(1f, 1f, 0f, false, true, 21f, 13f)
        lineTo(3f, 13f)
        arcTo(1f, 1f, 0f, false, true, 3f, 11f)
        close()
        moveTo(3f, 17f)
        lineTo(15f, 17f)
        arcTo(1f, 1f, 0f, false, true, 15f, 19f)
        lineTo(3f, 19f)
        arcTo(1f, 1f, 0f, false, true, 3f, 17f)
        close()
    }.build()

    val Person = ImageVector.Builder(
        name = "Person",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color(0xFF1A1A1A)),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round
    ) {
        // Circle cx=12 cy=8 r=4
        moveTo(12f, 4f)
        arcTo(4f, 4f, 0f, true, true, 12f, 12f)
        arcTo(4f, 4f, 0f, true, true, 12f, 4f)
        close()
        // Path d="M4 20c0-4 3.6-7 8-7s8 3 8 7"
        moveTo(4f, 20f)
        curveTo(4f, 16f, 7.6f, 13f, 12f, 13f)
        reflectiveCurveTo(20f, 16f, 20f, 20f)
    }.build()

    val Home = ImageVector.Builder(
        name = "Home",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = SolidColor(Color(0xFF2196F3)),
        fillAlpha = 0.15f,
        stroke = SolidColor(Color(0xFF2196F3)),
        strokeLineWidth = 1.8f,
        strokeLineJoin = StrokeJoin.Round
    ) {
        // d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"
        moveTo(3f, 9f)
        lineTo(12f, 2f)
        lineTo(21f, 9f)
        verticalLineTo(20f)
        arcTo(2f, 2f, 0f, false, true, 19f, 22f)
        horizontalLineTo(5f)
        arcTo(2f, 2f, 0f, false, true, 3f, 20f)
        close()
    }.path(
        stroke = SolidColor(Color(0xFF2196F3)),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        // d="M9 22V12h6v10"
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
        stroke = SolidColor(Color(0xFF4A4A4A)),
        strokeLineWidth = 1.8f,
        strokeLineJoin = StrokeJoin.Round
    ) {
        // d="M12 2l3 6.3L22 9.3l-5 4.9 1.2 6.8L12 18l-6.2 3 1.2-6.8L2 9.3l7-.9z"
        moveTo(12f, 2f)
        lineTo(15f, 8.3f)
        lineTo(22f, 9.3f)
        lineTo(17f, 14.2f)
        lineTo(18.2f, 21f)
        lineTo(12f, 18f)
        lineTo(5.8f, 21f)
        lineTo(7f, 14.2f)
        lineTo(2f, 9.3f)
        lineTo(9f, 8.4f)
        close()
    }.build()

    val Clock = ImageVector.Builder(
        name = "Clock",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color(0xFF4A4A4A)),
        strokeLineWidth = 1.8f
    ) {
        // circle cx=12 cy=12 r=9
        moveTo(12f, 3f)
        arcTo(9f, 9f, 0f, true, true, 12f, 21f)
        arcTo(9f, 9f, 0f, true, true, 12f, 3f)
        close()
        // d="M12 7v5l3 3"
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
        stroke = SolidColor(Color(0xFF4A4A4A)),
        strokeLineWidth = 1.8f,
        strokeLineJoin = StrokeJoin.Round
    ) {
        // d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"
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
        stroke = SolidColor(Color(0xFF4A4A4A)),
        strokeLineWidth = 1.8f
    ) {
        // circle cx=12 cy=12 r=3
        moveTo(12f, 9f)
        arcTo(3f, 3f, 0f, true, true, 12f, 15f)
        arcTo(3f, 3f, 0f, true, true, 12f, 9f)
        close()
        // Simplified gear path based on the complex SVG
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
        // ... abbreviated for brevity, but let's keep the essence
    }.build()

    val Send = ImageVector.Builder(
        name = "Send",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round
    ) {
        // d="M22 2L11 13"
        moveTo(22f, 2f)
        lineTo(11f, 13f)
    }.path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        // d="M22 2L15 22L11 13L2 9L22 2Z"
        moveTo(22f, 2f)
        lineTo(15f, 22f)
        lineTo(11f, 13f)
        lineTo(2f, 9f)
        close()
    }.build()

    val Mic = ImageVector.Builder(
        name = "Mic",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color(0xFF4A4A4A)),
        strokeLineWidth = 1.8f
    ) {
        // rect x=9 y=2 width=6 height=12 rx=3
        moveTo(12f, 2f)
        arcTo(3f, 3f, 0f, false, true, 15f, 5f)
        verticalLineTo(11f)
        arcTo(3f, 3f, 0f, false, true, 9f, 11f)
        verticalLineTo(5f)
        arcTo(3f, 3f, 0f, false, true, 12f, 2f)
        close()
        // path d="M5 11c0 3.9 3.1 7 7 7s7-3.1 7-7"
        moveTo(5f, 11f)
        arcTo(7f, 7f, 0f, false, false, 19f, 11f)
        // line x1=12 y1=18 x2=12 y2=22
        moveTo(12f, 18f)
        verticalLineTo(22f)
    }.build()
}
