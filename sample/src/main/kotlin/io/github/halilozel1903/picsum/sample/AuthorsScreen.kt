package io.github.halilozel1903.picsum.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.picsum.model.AuthorPhotos

/**
 * The photographers of the first 200 photos (two pages of `/v2/list` followed through the `Link` header), most photos
 * first, and the photos of the selected one.
 */
@Composable
fun AuthorsScreen(
    repository: GalleryRepository,
    selected: String?,
    onSelect: (String) -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var reload by remember { mutableIntStateOf(0) }
    val state by produceState<Load<AuthorsIndex>>(Load.Loading, reload) {
        value = Load.Loading
        value = load { repository.authors() }
    }
    ReportLoad(state)

    when (val current = state) {
        Load.Loading -> LoadingState(modifier)
        is Load.Failed -> ErrorState(current.message, onRetry = { reload++ }, modifier = modifier)
        is Load.Ready -> {
            val index = current.value
            val author = index.authors.firstOrNull { it.author == selected } ?: index.authors.firstOrNull()
            if (author == null) {
                ErrorState("Picsum returned no photos.", onRetry = { reload++ }, modifier = modifier)
            } else {
                Row(modifier.fillMaxSize()) {
                    AuthorList(index, author, onSelect)
                    AuthorPhotosPane(author, onOpen, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AuthorList(index: AuthorsIndex, selected: AuthorPhotos, onSelect: (String) -> Unit) {
    Column(
        Modifier
            .width(340.dp)
            .fillMaxHeight()
            .padding(top = 20.dp),
    ) {
        Column(Modifier.padding(start = 8.dp, end = 16.dp, bottom = 12.dp)) {
            Text("Authors", style = MaterialTheme.typography.headlineSmall)
            Text(
                "${index.authors.size} photographers in the first ${index.photoCount} photos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(index.authors, key = { it.author }) { group ->
                AuthorRow(group, isSelected = group.author == selected.author, onClick = { onSelect(group.author) })
            }
        }
    }
}

@Composable
private fun AuthorRow(group: AuthorPhotos, isSelected: Boolean, onClick: () -> Unit) {
    val background = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    Row(
        Modifier
            .fillMaxWidth()
            .padding(end = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhotoImage(
            url = group.photos.first().image(112).url,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
        )
        Column(
            Modifier
                .weight(1f)
                .padding(start = 14.dp),
        ) {
            Text(group.author, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (group.count == 1) "1 photo" else "${group.count} photos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AuthorPhotosPane(author: AuthorPhotos, onOpen: (String) -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxHeight()) {
        Column(Modifier.padding(start = 16.dp, end = 24.dp, top = 20.dp, bottom = 16.dp)) {
            Text(author.author, style = MaterialTheme.typography.headlineSmall)
            Text(
                "${if (author.count == 1) "1 photo" else "${author.count} photos"} on Lorem Picsum, shared on Unsplash",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        key(author.author) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(200.dp),
                state = rememberLazyStaggeredGridState(),
                contentPadding = PaddingValues(start = 16.dp, end = 24.dp, bottom = 24.dp),
                verticalItemSpacing = 16.dp,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(author.photos, key = { it.id }) { photo ->
                    PhotoTile(photo, onClick = { onOpen(photo.id) }, showAuthor = false)
                }
            }
        }
    }
}
