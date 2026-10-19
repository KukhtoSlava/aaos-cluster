package com.autobox.cluster.ui.components

import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import com.autobox.cluster.ui.theme.ClusterDimens

/** Places an element at absolute design coordinates inside a Box. */
internal fun Modifier.at(x: Dp, y: Dp): Modifier = offset(x, y)

/** A round-capped line with a soft glow, like the design's box-shadow: 0 0 14px. */
internal fun DrawScope.glowLine(color: Color, glow: Color, start: Offset, end: Offset, width: Float) {
    drawIntoCanvas { canvas ->
        val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toArgb()
            strokeWidth = width
            strokeCap = AndroidPaint.Cap.ROUND
            setShadowLayer(ClusterDimens.Glow.ShadowRadius.toPx(), 0f, 0f, glow.toArgb())
        }
        canvas.nativeCanvas.drawLine(start.x, start.y, end.x, end.y, paint)
    }
}
