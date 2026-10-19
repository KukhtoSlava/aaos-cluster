package com.autobox.cluster.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.autobox.cluster.R

@Composable
internal fun SpeedLimitSign(limitKmh: Int, size: Dp, ring: Dp, fontSize: TextUnit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.speed_limit, limitKmh)
    Box(
        modifier = modifier
            .size(size)
            .background(colorResource(R.color.cluster_limit_face), CircleShape)
            .border(ring, colorResource(R.color.cluster_limit_ring), CircleShape)
            .clearAndSetSemantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        ClusterText(
            text = limitKmh.toString(),
            size = fontSize,
            weight = FontWeight.Bold,
            color = colorResource(R.color.cluster_limit_text),
        )
    }
}
