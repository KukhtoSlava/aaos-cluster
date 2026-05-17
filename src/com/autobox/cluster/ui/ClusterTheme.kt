package com.autobox.cluster.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontFamily
import com.autobox.cluster.R

// Cluster display is always dark — no user preference, fixed automotive screen.
@Composable
private fun clusterColorScheme() = darkColorScheme(
    primary = colorResource(R.color.cluster_primary),
    onPrimary = colorResource(R.color.cluster_on_primary),
    secondary = colorResource(R.color.cluster_secondary),
    onSecondary = colorResource(R.color.cluster_on_secondary),
    tertiary = colorResource(R.color.cluster_tertiary),
    onTertiary = colorResource(R.color.cluster_on_tertiary),
    error = colorResource(R.color.cluster_error),
    onError = colorResource(R.color.cluster_on_error),
    background = colorResource(R.color.cluster_background),
    onBackground = colorResource(R.color.cluster_on_background),
    surface = colorResource(R.color.cluster_surface),
    onSurface = colorResource(R.color.cluster_on_surface),
    onSurfaceVariant = colorResource(R.color.cluster_on_surface_variant),
    outlineVariant = colorResource(R.color.cluster_outline_variant),
)

@Composable
fun ClusterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = clusterColorScheme(),
        typography = MaterialTheme.typography.copy(
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            titleMedium = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
            labelMedium = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
        ),
        content = content,
    )
}
