package com.autobox.cluster.ui.theme

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.autobox.cluster.R

// Saira, static instances of the variable font (OFL, see OFL-Saira.txt)
val Saira = FontFamily(
    Font(R.font.saira_regular, FontWeight.Normal),
    Font(R.font.saira_medium, FontWeight.Medium),
    Font(R.font.saira_semibold, FontWeight.SemiBold),
    Font(R.font.saira_bold, FontWeight.Bold),
)

// Cluster display is always dark — no user preference, fixed automotive screen.
@Composable
private fun clusterColorScheme() = darkColorScheme(
    primary = colorResource(R.color.cluster_accent),
    background = colorResource(R.color.cluster_background),
    onBackground = colorResource(R.color.cluster_text),
    surface = colorResource(R.color.cluster_background),
    onSurface = colorResource(R.color.cluster_text),
    onSurfaceVariant = colorResource(R.color.cluster_text_secondary),
)

@Composable
fun ClusterTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = clusterColorScheme()) {
        ProvideTextStyle(
            TextStyle(
                fontFamily = Saira,
                color = colorResource(R.color.cluster_text),
                // Tabular figures keep changing numbers from jittering
                fontFeatureSettings = "tnum",
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
            content = content,
        )
    }
}
