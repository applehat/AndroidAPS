package app.aaps.core.ui.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Icon for the Medtronic Flex / Instinct pump plugin.
 *
 * Simple line-art of a durable pump body with a top cartridge clip and a side button.
 * Monochrome so it follows the theme tint like the other plugin icons.
 */
val IcPluginFlex: ImageVector by lazy {
    ImageVector.Builder(
        name = "IcPluginFlex",
        defaultWidth = 48.dp,
        defaultHeight = 48.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Outer body (rounded rectangle)
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.4f, fill = null, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(6f, 5.5f)
            lineTo(18f, 5.5f)
            arcTo(2.5f, 2.5f, 0f, false, true, 20.5f, 8f)
            lineTo(20.5f, 17f)
            arcTo(2.5f, 2.5f, 0f, false, true, 18f, 19.5f)
            lineTo(6f, 19.5f)
            arcTo(2.5f, 2.5f, 0f, false, true, 3.5f, 17f)
            lineTo(3.5f, 8f)
            arcTo(2.5f, 2.5f, 0f, false, true, 6f, 5.5f)
            close()
        }
        // Screen / face panel
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.0f, fill = null, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(7f, 8.5f)
            lineTo(17f, 8.5f)
            lineTo(17f, 14.5f)
            lineTo(7f, 14.5f)
            close()
        }
        // Cartridge clip on top
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.2f, fill = null, strokeLineCap = StrokeCap.Round) {
            moveTo(9f, 5.5f)
            lineTo(9f, 3.5f)
            lineTo(15f, 3.5f)
            lineTo(15f, 5.5f)
        }
        // Side button
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 2.0f, fill = null, strokeLineCap = StrokeCap.Round) {
            moveTo(20.5f, 11f)
            lineTo(20.5f, 14f)
        }
    }.build()
}
