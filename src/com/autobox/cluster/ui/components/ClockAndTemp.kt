package com.autobox.cluster.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.autobox.cluster.R
import com.autobox.cluster.ui.theme.ClusterDimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/** Wall clock, updated on every minute boundary. */
@Composable
internal fun rememberClockText(): String {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000L - now % 60_000L)
        }
    }
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
    return remember(now, pattern) { SimpleDateFormat(pattern, Locale.getDefault()).format(Date(now)) }
}

/** Clock and outside temperature, baseline-aligned. */
@Composable
internal fun ClockAndTemp(clock: String, outsideTempC: Int?, timeSize: TextUnit, tempSize: TextUnit) {
    Row(horizontalArrangement = Arrangement.spacedBy(ClusterDimens.ClockAndTemp.Gap)) {
        ClusterText(clock, timeSize, Modifier.alignByBaseline(), weight = FontWeight.Medium)
        ClusterText(
            text = outsideTempC?.let { stringResource(R.string.outside_temp_format, it) }
                ?: stringResource(R.string.outside_temp_unknown),
            size = tempSize,
            modifier = Modifier.alignByBaseline(),
            color = colorResource(R.color.cluster_text_secondary),
        )
    }
}
