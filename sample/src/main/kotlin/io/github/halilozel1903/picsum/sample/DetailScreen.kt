package io.github.halilozel1903.picsum.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.halilozel1903.picsum.ImageFormat
import io.github.halilozel1903.picsum.Picsum
import io.github.halilozel1903.picsum.PicsumImage
import io.github.halilozel1903.picsum.PhotoSize
import io.github.halilozel1903.picsum.WallpaperSize
import io.github.halilozel1903.picsum.model.Photo
import kotlin.math.roundToInt

/** The largest preview the detail screen asks Picsum for, per side. */
private const val PreviewLimit = 1600

/**
 * One photo from `/id/{id}/info`: a large preview on the left and, on the right, the photographer, the original size,
 * live grayscale, blur and format controls and a wallpaper size picker. Every control rebuilds the Picsum url, and the
 * preview loads the new url while the previous image stays on screen.
 */
@Composable
fun DetailScreen(open: OpenPhoto, repository: GalleryRepository, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var reload by remember { mutableIntStateOf(0) }
    val state by produceState<Load<Photo>>(Load.Loading, open.id, reload) {
        value = Load.Loading
        value = load { repository.photo(open.id) }
    }
    ReportLoad(state)

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.padding(start = 8.dp, top = 8.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                BackIcon(MaterialTheme.colorScheme.onSurface, Modifier.size(22.dp))
            }
            Text("Photo #${open.id}", style = MaterialTheme.typography.titleMedium)
        }
        when (val current = state) {
            Load.Loading -> LoadingState()
            is Load.Failed -> ErrorState(current.message, onRetry = { reload++ })
            is Load.Ready -> DetailContent(current.value, open)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(photo: Photo, initial: OpenPhoto) {
    var grayscale by rememberSaveable(photo.id) { mutableStateOf(initial.grayscale) }
    var blur by rememberSaveable(photo.id) { mutableIntStateOf(initial.blur) }
    var format by rememberSaveable(photo.id) { mutableStateOf(ImageFormat.Default) }
    // -1 is the original size; otherwise an index into options.
    var wallpaperIndex by rememberSaveable(photo.id) { mutableIntStateOf(-1) }

    val metrics = LocalContext.current.resources.displayMetrics
    val options = remember(metrics.widthPixels, metrics.heightPixels) {
        buildList<WallpaperSize> {
            add(WallpaperSize.forScreen(metrics.widthPixels, metrics.heightPixels))
            addAll(WallpaperSize.presets)
        }
    }
    val wallpaper = options.getOrNull(wallpaperIndex)
    val target: PhotoSize = wallpaper?.picsumSize ?: photo.size.limitedTo(Picsum.MAX_DIMENSION)
    val style: (PicsumImage) -> PicsumImage = { it.grayscale(grayscale).blur(blur).format(format) }
    val fullUrl = style(photo.image(target)).url

    Row(
        Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            val density = LocalDensity.current
            val box = with(density) {
                PhotoSize(maxWidth.roundToPx().coerceAtLeast(1), maxHeight.roundToPx().coerceAtLeast(1))
            }
            // The preview has the target's aspect ratio (the wallpaper crop, or the whole photo) and fills the box.
            val shown = target.fitInside(box, allowUpscale = true)
            val previewUrl = style(photo.image(shown.limitedTo(PreviewLimit))).url
            val (w, h) = with(density) { shown.width.toDp() to shown.height.toDp() }
            Preview(
                url = previewUrl,
                description = "Photo by ${photo.author}",
                modifier = Modifier
                    .size(w, h)
                    .clip(RoundedCornerShape(20.dp)),
            )
        }
        Column(
            Modifier
                .width(400.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AuthorBlock(photo)
            Section("Details") {
                InfoRow("Original size", "${photo.size} px")
                InfoRow("Aspect ratio", ratioLabel(photo.width, photo.height))
                InfoRow(
                    "Orientation",
                    when {
                        photo.isLandscape -> "Landscape"
                        photo.isPortrait -> "Portrait"
                        else -> "Square"
                    },
                )
            }
            Section("Effects") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Grayscale", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = grayscale, onCheckedChange = { grayscale = it })
                }
                Text(
                    if (blur == 0) "Blur off" else "Blur $blur of ${Picsum.MAX_BLUR}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Slider(
                    value = blur.toFloat(),
                    onValueChange = { blur = it.roundToInt() },
                    valueRange = 0f..Picsum.MAX_BLUR.toFloat(),
                    steps = Picsum.MAX_BLUR - 1,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Format", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    listOf(ImageFormat.Default to "JPEG", ImageFormat.Jpg to ".jpg", ImageFormat.Webp to "WebP").forEach { (option, label) ->
                        FilterChip(selected = format == option, onClick = { format = option }, label = { Text(label) })
                    }
                }
            }
            Section("Wallpaper size") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    FilterChip(selected = wallpaperIndex == -1, onClick = { wallpaperIndex = -1 }, label = { Text("Original") })
                    options.forEachIndexed { index, option ->
                        FilterChip(
                            selected = wallpaperIndex == index,
                            onClick = { wallpaperIndex = index },
                            label = { Text(option.name) },
                        )
                    }
                }
                Text(
                    if (wallpaper == null) {
                        "The whole photo at ${target} px" +
                            if (target != photo.size) ", scaled down to Picsum's 5000 px limit" else ""
                    } else {
                        "${wallpaper.width} x ${wallpaper.height} px, cropped around the center" +
                            if (wallpaper.picsumSize != wallpaper.size) " (served at ${wallpaper.picsumSize})" else ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Section("Image URL") {
                SelectionContainer {
                    Text(
                        fullUrl,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

/** The preview: the new url loads on top while the last loaded image stays underneath. */
@Composable
private fun Preview(url: String, description: String, modifier: Modifier) {
    var lastLoaded by remember { mutableStateOf<String?>(null) }
    Box(modifier) {
        val previous = lastLoaded
        if (previous != null && previous != url) {
            AsyncImage(
                model = previous,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        PhotoImage(
            url = url,
            contentDescription = description,
            modifier = Modifier.fillMaxSize(),
            placeholder = previous == null,
            onLoaded = { lastLoaded = it },
        )
    }
}

@Composable
private fun AuthorBlock(photo: Photo) {
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Photo by", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(photo.author, style = MaterialTheme.typography.headlineMedium)
        Text(
            "Lorem Picsum photo #${photo.id}, from Unsplash",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        OutlinedButton(onClick = { runCatching { uriHandler.openUri(photo.url) } }) {
            Text("Open on Unsplash")
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

/** `"3:2"` when the sides reduce to small numbers, otherwise `"1.50:1"`. */
private fun ratioLabel(width: Int, height: Int): String {
    tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
    val d = gcd(width, height)
    val (w, h) = width / d to height / d
    return if (w <= 32 && h <= 32) "$w:$h" else "%.2f:1".format(java.util.Locale.US, width.toDouble() / height)
}
