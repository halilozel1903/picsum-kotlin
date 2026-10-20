package io.github.halilozel1903.picsum.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.OkHttpClient

/**
 * A tablet photo gallery built on picsum-kotlin. It loads the live Lorem Picsum API and the photos themselves with
 * Coil. `scripts/screenshots.sh` starts it with `--es scene <scene>` to open a fixed screen, loaded live like a normal
 * launch:
 *
 * - `tablet-gallery`: page 3 of the photo list in a staggered grid
 * - `tablet-detail`: photo 1015 with its author, original size and the wallpaper size picker
 * - `tablet-effects`: photo 870 with grayscale and blur 4 applied
 * - `tablet-authors`: the first 200 photos grouped by photographer
 *
 * Nothing is bundled or faked: when Picsum cannot be reached, the screen shows the error and a scene fails.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val scene = Scene.from(intent.getStringExtra(EXTRA_SCENE))
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components {
                    add(OkHttpNetworkFetcherFactory(callFactory = { imageClient }))
                }
                .build()
        }
        setContent {
            GalleryTheme(dark = isSystemInDarkTheme()) {
                // The Surface makes text default to onBackground, so it stays readable in dark mode.
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    GalleryApp(scene = scene, repository = GalleryRepository.shared)
                }
            }
        }
    }

    companion object {
        const val EXTRA_SCENE = "scene"

        private val imageClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", "picsum-kotlin-sample/1.0.0 (+https://github.com/halilozel1903/picsum-kotlin)")
                            .build(),
                    )
                }
                .build()
        }
    }
}
