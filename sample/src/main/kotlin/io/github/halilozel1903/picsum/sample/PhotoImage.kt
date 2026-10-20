package io.github.halilozel1903.picsum.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import kotlinx.coroutines.delay

/** How often a failed image is requested again before it counts as failed. */
private const val ImageRetries = 3

/**
 * A photo loaded with Coil from a Picsum image url (Picsum redirects to its CDN). A flat placeholder stands in while
 * it loads, unless [placeholder] is off (when an earlier image is shown underneath). A failed load is retried a few
 * times. Screenshot scenes wait until every photo on screen has loaded, and fail when one does not.
 */
@Composable
fun PhotoImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: Boolean = true,
    onLoaded: (String) -> Unit = {},
) {
    val monitor = LocalSceneMonitor.current
    var attempt by remember(url) { mutableIntStateOf(0) }
    var status by remember(url) { mutableStateOf(ImageStatus.Loading) }
    if (monitor != null) {
        val id = remember { Any() }
        DisposableEffect(monitor, id) { onDispose { monitor.image(id, null) } }
        // snapshotFlow starts with the current value, so a load that finished before this effect ran still counts.
        // A failure that will be retried still counts as loading.
        LaunchedEffect(monitor, id, url) {
            snapshotFlow {
                if (status == ImageStatus.Failed && attempt < ImageRetries) ImageStatus.Loading else status
            }.collect { monitor.image(id, it) }
        }
    }
    if (status == ImageStatus.Failed && attempt < ImageRetries) {
        LaunchedEffect(url, attempt) {
            delay(1_000L shl attempt)
            status = ImageStatus.Loading
            attempt++
        }
    }
    val background = if (placeholder && status != ImageStatus.Loaded) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
    Box(modifier.background(background)) {
        key(url, attempt) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
                onState = { state ->
                    status = when (state) {
                        is AsyncImagePainter.State.Success -> ImageStatus.Loaded.also { onLoaded(url) }
                        is AsyncImagePainter.State.Error -> ImageStatus.Failed
                        else -> ImageStatus.Loading
                    }
                },
            )
        }
    }
}
