package io.github.halilozel1903.picsum.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.picsum.model.Photo
import io.github.halilozel1903.picsum.model.PhotoPage

/** Width in pixels of the grid thumbnails; Picsum sizes them on the server, at each photo's aspect ratio. */
private const val ThumbnailWidth = 480

/**
 * One page of `/v2/list` in a staggered grid, each photo at its own aspect ratio with its photographer. The previous
 * and next buttons follow the `Link` header of the response, so they only show pages that exist.
 */
@Composable
fun GalleryScreen(
    repository: GalleryRepository,
    page: Int,
    onPageChange: (Int) -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var reload by remember { mutableIntStateOf(0) }
    val state by produceState<Load<PhotoPage>>(Load.Loading, page, reload) {
        value = Load.Loading
        value = load { repository.page(page) }
    }
    ReportLoad(state)

    Column(modifier.fillMaxSize()) {
        when (val current = state) {
            Load.Loading -> LoadingState()
            is Load.Failed -> ErrorState(current.message, onRetry = { reload++ })
            is Load.Ready -> {
                val result = current.value
                GalleryHeader(result, onPageChange)
                key(result.page) {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Adaptive(220.dp),
                        state = rememberLazyStaggeredGridState(),
                        contentPadding = PaddingValues(start = 16.dp, end = 24.dp, bottom = 24.dp),
                        verticalItemSpacing = 16.dp,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(result.photos, key = { it.id }) { photo ->
                            PhotoTile(photo, onClick = { onOpen(photo.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryHeader(page: PhotoPage, onPageChange: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 24.dp, top = 20.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Gallery", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Showing ${page.photos.size} photos from page ${page.page} of /v2/list, by the Unsplash photographers named under each",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PageButton(label = "Previous", right = false, target = page.previous?.page, onPageChange = onPageChange)
        Text(
            "Page ${page.page}",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        PageButton(label = "Next", right = true, target = page.next?.page, onPageChange = onPageChange)
    }
}

@Composable
private fun PageButton(label: String, right: Boolean, target: Int?, onPageChange: (Int) -> Unit) {
    FilledTonalButton(
        onClick = { target?.let(onPageChange) },
        enabled = target != null,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    ) {
        val color = if (target != null) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        }
        if (!right) ChevronIcon(color, right = false, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label)
        Spacer(Modifier.width(4.dp))
        if (right) ChevronIcon(color, right = true, modifier = Modifier.size(18.dp))
    }
}

/** A photo at its own aspect ratio, with its photographer (unless [showAuthor] is off), id and original size. */
@Composable
fun PhotoTile(photo: Photo, onClick: () -> Unit, modifier: Modifier = Modifier, showAuthor: Boolean = true) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick),
    ) {
        PhotoImage(
            url = photo.thumbnail(ThumbnailWidth).url,
            contentDescription = "Photo by ${photo.author}",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(photo.aspectRatio.toFloat()),
        )
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            if (showAuthor) {
                Text(
                    photo.author,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "#${photo.id} · ${photo.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
