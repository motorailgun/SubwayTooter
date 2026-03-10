package jp.juggler.subwaytooter

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import jp.juggler.subwaytooter.compose.StScreen
import jp.juggler.subwaytooter.span.NetworkEmojiSpan
import jp.juggler.subwaytooter.util.EmojiSizeMode
import jp.juggler.subwaytooter.util.NetworkEmojiInvalidator
import jp.juggler.subwaytooter.util.getStColorTheme
import jp.juggler.subwaytooter.view.MyNetworkImageView
import jp.juggler.util.coroutine.AppDispatchers
import jp.juggler.util.coroutine.launchAndShowError
import kotlinx.coroutines.withContext

class ActGlideTest : ComponentActivity() {
    private var items = mutableStateOf<List<MyItem>>(emptyList())

    private val mainHandler by lazy {
        Handler(Looper.getMainLooper())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App1.setActivityTheme(this)
        val stColorScheme = getStColorTheme()

        setContent {
            StScreen(
                stColorScheme = stColorScheme,
                title = "Glide Test",
                onBack = { finish() },
            ) { contentPadding ->
                val list by items
                LazyColumn(
                    modifier = Modifier.padding(contentPadding),
                ) {
                    items(list.size) { index ->
                        val item = list[index]
                        GlideTestRow(item)
                    }
                }
            }
        }

        launchAndShowError { load() }
    }

    private suspend fun load() {
        items.value = withContext(AppDispatchers.IO) {
            buildList {
                repeat(300) {
                    arrayOf(
                        "gifAnime.gif",
                        "gif-anime-transparent.gif",
                        "jpeg.jpg",
                        "png.png",
                        "png-anime-gauge_charge.png",
                        "png-loading_blue.png",
                        "svg-anim1.svg",
                        "webp-anime-force.webp",
                        "webp-lossy-flag-off.webp",
                        "webp-lossy-flag-on.webp",
                        "webp-maker-no-flags.webp",
                        "webp-mixed-flag-on.webp",
                    ).map {
                        MyItem(name = it, url = "https://m1j.zzz.ac/tateisu/glideTest/$it")
                    }.let { addAll(it) }
                }
            }
        }
    }

    private class MyItem(
        val name: String,
        val url: String,
    )

    @Composable
    private fun GlideTestRow(item: MyItem) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Static image
            AndroidView(
                factory = { context ->
                    MyNetworkImageView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            (60 * context.resources.displayMetrics.density).toInt(),
                            (60 * context.resources.displayMetrics.density).toInt(),
                        )
                    }
                },
                update = { view ->
                    val density = view.context.resources.displayMetrics.density
                    val r = (8f * density)
                    view.setImageUrl(r, item.url, null)
                },
                modifier = Modifier.size(60.dp),
            )
            Spacer(Modifier.width(4.dp))
            // Animation image
            AndroidView(
                factory = { context ->
                    MyNetworkImageView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            (60 * context.resources.displayMetrics.density).toInt(),
                            (60 * context.resources.displayMetrics.density).toInt(),
                        )
                    }
                },
                update = { view ->
                    val density = view.context.resources.displayMetrics.density
                    val r = (8f * density)
                    view.setImageUrl(r, item.url, item.url)
                },
                modifier = Modifier.size(60.dp),
            )
            Spacer(Modifier.width(4.dp))
            // Name with emoji span
            AndroidView(
                factory = { context ->
                    TextView(context)
                },
                update = { view ->
                    val text = SpannableStringBuilder().apply {
                        val start = length
                        append("a")
                        val end = length
                        val span = NetworkEmojiSpan(
                            url = item.url,
                            scale = 2f,
                            sizeMode = EmojiSizeMode.Square,
                        )
                        setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        append(" ")
                        append(item.name)
                    }
                    val invalidator = NetworkEmojiInvalidator(mainHandler, view)
                    invalidator.text = text
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
