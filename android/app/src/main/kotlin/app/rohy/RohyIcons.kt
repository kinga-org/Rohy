package app.rohy

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Stroke icons of the design board (24 × 24 viewport, 2 px stroke, tinted by Icon). */
object RohyIcons {
    val Send = icon("send") { line(22f, 2f, 11f, 13f); poly(true, 22f, 2f, 15f, 22f, 11f, 13f, 2f, 9f) }
    val History = icon("history") {
        path("M3 12a9 9 0 1 0 3-6.7"); poly(false, 3f, 4f, 3f, 9f, 8f, 9f); poly(false, 12f, 7f, 12f, 12f, 15f, 14f)
    }
    val Tile = icon("tile") { rect(3f, 3f, 7f, 7f, 2f); rect(14f, 3f, 7f, 7f, 2f); rect(3f, 14f, 7f, 7f, 2f); rect(14f, 14f, 7f, 7f, 2f) }
    val TextSelect = icon("text") { path("M4 7V5h16v2"); line(12f, 5f, 12f, 19f); line(9f, 19f, 15f, 19f) }
    val Share = icon("share") {
        circle(18f, 5f, 3f); circle(6f, 12f, 3f); circle(18f, 19f, 3f)
        line(8.6f, 13.5f, 15.4f, 17.5f); line(15.4f, 6.5f, 8.6f, 10.5f)
    }
    val Shield = icon("shield") { path("M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"); line(12f, 8f, 12f, 12f); line(12f, 16f, 12.01f, 16f) }
    val Computer = icon("computer") { rect(3f, 4f, 18f, 12f, 2f); line(2f, 20f, 22f, 20f) }
    val Phone = icon("phone") { rect(6f, 2f, 12f, 20f, 2.5f); line(11f, 18f, 13f, 18f) }
    val Scan = icon("scan") {
        path("M3 7V5a2 2 0 0 1 2-2h2"); path("M17 3h2a2 2 0 0 1 2 2v2")
        path("M21 17v2a2 2 0 0 1-2 2h-2"); path("M7 21H5a2 2 0 0 1-2-2v-2"); line(7f, 12f, 17f, 12f)
    }
    val Clipboard = icon("clipboard") {
        rect(8f, 2f, 8f, 4f, 1f); path("M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2")
    }
    val Back = icon("back") { line(19f, 12f, 5f, 12f); poly(false, 12f, 19f, 5f, 12f, 12f, 5f) }
    val Chevron = icon("chevron") { poly(false, 9f, 6f, 15f, 12f, 9f, 18f) }
    val WifiOff = icon("wifi-off") {
        line(2f, 2f, 22f, 22f); path("M8.5 16.5a5 5 0 0 1 7 0"); path("M2 8.8a15 15 0 0 1 4.2-2.7")
        path("M10.7 5.1A15 15 0 0 1 22 8.8"); path("M5 12.6a10 10 0 0 1 5.2-2.6"); path("M16.8 13.4A10 10 0 0 1 19 12.6")
        line(12f, 20f, 12.01f, 20f)
    }
    val Warning = icon("warning") {
        path("M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z")
        line(12f, 9f, 12f, 13f); line(12f, 17f, 12.01f, 17f)
    }
    val Check = icon("check") { poly(false, 5f, 12f, 10f, 17f, 19f, 7f) }
    val Exclamation = icon("exclamation") { line(12f, 7f, 12f, 13f); line(12f, 17f, 12.01f, 17f) }
    val Question = icon("question") { path("M9.1 9a3 3 0 0 1 5.8 1c0 2-3 3-3 3"); line(12f, 17f, 12.01f, 17f) }
    val Refresh = icon("refresh") { poly(false, 23f, 4f, 23f, 10f, 17f, 10f); path("M20.5 15a9 9 0 1 1-2.1-9.4L23 10") }
    val Sent = icon("sent") { line(12f, 19f, 12f, 5f); poly(false, 5f, 12f, 12f, 5f, 19f, 12f) }
    val Received = icon("received") { line(12f, 5f, 12f, 19f); poly(false, 19f, 12f, 12f, 19f, 5f, 12f) }

    /** The two links of the Rohy mark, drawn over its disc. */
    val Mark = icon("mark", viewport = 40f, stroke = 2.6f) { rect(7.2f, 15.6f, 13.6f, 8.8f, 4.4f); rect(19.2f, 15.6f, 13.6f, 8.8f, 4.4f) }

    /** SVG path data of each shape, so every icon reads like its board source. */
    class Shapes {
        val paths = mutableListOf<String>()
        fun path(d: String) { paths += d }
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) = path("M$x1 ${y1}L$x2 $y2")
        fun poly(closed: Boolean, vararg points: Float) {
            val pairs = points.toList().chunked(2).map { (x, y) -> "$x $y" }
            path("M" + pairs.joinToString("L") + if (closed) "Z" else "")
        }
        fun rect(x: Float, y: Float, w: Float, h: Float, r: Float) = path(
            "M${x + r} ${y}H${x + w - r}A$r $r 0 0 1 ${x + w} ${y + r}V${y + h - r}" +
                "A$r $r 0 0 1 ${x + w - r} ${y + h}H${x + r}A$r $r 0 0 1 $x ${y + h - r}" +
                "V${y + r}A$r $r 0 0 1 ${x + r} ${y}Z",
        )
        fun circle(cx: Float, cy: Float, r: Float) = path("M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0")
    }

    private fun icon(name: String, viewport: Float = 24f, stroke: Float = 2f, draw: Shapes.() -> Unit): ImageVector {
        val builder = ImageVector.Builder(name, 24.dp, 24.dp, viewport, viewport)
        Shapes().apply(draw).paths.forEach { d ->
            builder.addPath(
                pathData = addPathNodes(d),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = stroke,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
    }
}
