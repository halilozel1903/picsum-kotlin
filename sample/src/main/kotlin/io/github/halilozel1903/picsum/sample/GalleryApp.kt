package io.github.halilozel1903.picsum.sample

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A photo opened from a screen, with the effects to start with. */
data class OpenPhoto(val id: String, val grayscale: Boolean = false, val blur: Int = 0)

/**
 * The whole gallery: a navigation rail with the photo grid and the authors view, and a photo's detail on top, all
 * loaded from the live Lorem Picsum API. A screenshot [scene] opens a fixed screen and reports to a [SceneMonitor]
 * once its data and every photo on screen have loaded.
 */
@Composable
fun GalleryApp(scene: Scene?, repository: GalleryRepository) {
    val monitor = remember(scene) { scene?.let { SceneMonitor(it) } }
    monitor?.Report()

    var tab by rememberSaveable { mutableStateOf(scene?.tab ?: Tab.Gallery) }
    var galleryPage by rememberSaveable { mutableIntStateOf(scene?.galleryPage ?: 1) }
    var selectedAuthor by rememberSaveable { mutableStateOf<String?>(null) }
    var open by remember {
        mutableStateOf<OpenPhoto?>(scene?.let { s -> s.photoId?.let { OpenPhoto(it, s.grayscale, s.blur) } })
    }

    CompositionLocalProvider(LocalSceneMonitor provides monitor) {
        val photo = open
        if (photo != null) {
            BackHandler { open = null }
            DetailScreen(
                open = photo,
                repository = repository,
                onBack = { open = null },
                modifier = Modifier.safeDrawingPadding(),
            )
        } else {
            Shell(
                tab = tab,
                onTab = { tab = it },
                repository = repository,
                galleryPage = galleryPage,
                onPageChange = { galleryPage = it },
                selectedAuthor = selectedAuthor,
                onSelectAuthor = { selectedAuthor = it },
                onOpen = { open = OpenPhoto(it) },
            )
        }
    }
}

/** The navigation rail and the current tab. */
@Composable
private fun Shell(
    tab: Tab,
    onTab: (Tab) -> Unit,
    repository: GalleryRepository,
    galleryPage: Int,
    onPageChange: (Int) -> Unit,
    selectedAuthor: String?,
    onSelectAuthor: (String) -> Unit,
    onOpen: (String) -> Unit,
) {
    Row(Modifier.fillMaxSize().safeDrawingPadding()) {
        NavigationRail(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            header = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Lorem\nPicsum",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(16.dp))
                }
            },
            modifier = Modifier.fillMaxHeight(),
        ) {
            Tab.entries.forEach { option ->
                NavigationRailItem(
                    selected = option == tab,
                    onClick = { onTab(option) },
                    icon = {
                        val color = if (option == tab) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        when (option) {
                            Tab.Gallery -> GridIcon(color, Modifier.size(24.dp))
                            Tab.Authors -> PeopleIcon(color, Modifier.size(24.dp))
                        }
                    },
                    label = { Text(option.label) },
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f).fillMaxHeight()) {
            when (tab) {
                Tab.Gallery -> GalleryScreen(
                    repository = repository,
                    page = galleryPage,
                    onPageChange = onPageChange,
                    onOpen = onOpen,
                )
                Tab.Authors -> AuthorsScreen(
                    repository = repository,
                    selected = selectedAuthor,
                    onSelect = onSelectAuthor,
                    onOpen = onOpen,
                )
            }
        }
    }
}
