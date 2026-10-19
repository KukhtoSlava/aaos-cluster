package com.autobox.cluster.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Text in design units. [lineHeight] is a multiple of [size] (CSS line-height):
 * the box is exactly that tall with the line centred in it, overflowing it like
 * CSS does. TextStyle.lineHeight cannot do this: Compose does not shrink a line
 * below the font's natural height, which for Saira is 1.574 em.
 */
@Composable
internal fun ClusterText(
    text: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified,
    letterSpacing: TextUnit = 0.sp,
    lineHeight: Float? = null,
    textAlign: TextAlign? = null,
) {
    val box = if (lineHeight != null) {
        Modifier
            // ScaledCanvas keeps fontScale at 1, so 1.sp is 1.dp
            .height((size.value * lineHeight).dp)
            .wrapContentHeight(Alignment.CenterVertically, unbounded = true)
    } else {
        Modifier
    }
    Text(
        text = text,
        modifier = modifier.then(box),
        color = color,
        fontSize = size,
        fontWeight = weight,
        letterSpacing = letterSpacing,
        textAlign = textAlign,
        maxLines = 1,
    )
}
