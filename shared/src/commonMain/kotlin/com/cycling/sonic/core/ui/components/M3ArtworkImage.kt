package com.cycling.sonic.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

@Composable
fun M3ArtworkImage(
    artworkUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: Painter? = null,
    error: Painter? = null,
    fallback: (@Composable () -> Unit)? = null
) {
    if (artworkUrl != null) {
        AsyncImage(
            model = artworkUrl,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else if (fallback != null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) { fallback() }
    } else {
        Box(modifier = modifier.fillMaxSize())
    }
}
