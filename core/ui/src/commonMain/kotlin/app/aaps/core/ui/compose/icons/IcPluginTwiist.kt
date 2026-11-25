package app.aaps.core.ui.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Icon for the Sequel twiist pump plugin.
 *
 * Line-art of the screenless round pump body: outer housing ring, inner face, the cartridge tab and
 * tubing exit at the top, and the bold edge button at the lower left. Monochrome like the other
 * plugin icons so it follows the theme tint.
 *
 * Viewport 24x24, body centred at (12, 13), outer radius 9.2.
 */
val IcPluginTwiist: ImageVector by lazy {
    ImageVector.Builder(
        name = "IcPluginTwiist",
        defaultWidth = 48.dp,
        defaultHeight = 48.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Outer housing
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.4f, fill = null) {
            moveTo(21.2f, 13f)
            arcTo(9.2f, 9.2f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 2.8f, y1 = 13f)
            arcTo(9.2f, 9.2f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 21.2f, y1 = 13f)
            close()
        }
        // Inner face
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.0f, fill = null) {
            moveTo(18.4f, 13f)
            arcTo(6.4f, 6.4f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 5.6f, y1 = 13f)
            arcTo(6.4f, 6.4f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 18.4f, y1 = 13f)
            close()
        }
        // Cartridge tab on top
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.2f, fill = null, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(7.4f, 5.03f)
            curveTo(8.4f, 2.9f, 15.6f, 2.9f, 16.6f, 5.03f)
        }
        // Tubing exit
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 0.9f, fill = null, strokeLineCap = StrokeCap.Round) {
            moveTo(15.2f, 3.4f)
            curveTo(16.0f, 2.2f, 17.4f, 1.5f, 19.2f, 1.1f)
        }
        // Edge button (lower left)
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 2.6f, fill = null, strokeLineCap = StrokeCap.Round) {
            moveTo(8.11f, 21.34f)
            arcTo(9.2f, 9.2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 4.95f, y1 = 18.91f)
        }
    }.build()
}
