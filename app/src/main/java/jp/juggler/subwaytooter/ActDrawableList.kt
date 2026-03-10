package jp.juggler.subwaytooter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import jp.juggler.subwaytooter.compose.StScreen
import jp.juggler.subwaytooter.util.getStColorTheme
import jp.juggler.util.coroutine.AppDispatchers
import jp.juggler.util.log.LogCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActDrawableList : ComponentActivity() {

    companion object {
        private val log = LogCategory("ActDrawableList")
    }

    private class MyItem(val id: Int, val name: String)

    private val drawableList = mutableStateListOf<MyItem>()
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App1.setActivityTheme(this)
        setContent {
            StScreen(
                title = "Drawables",
                onBack = { finish() },
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    items(drawableList) { item ->
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                painter = painterResource(item.id),
                                contentDescription = item.name,
                                modifier = Modifier.size(48.dp),
                            )
                            Text(
                                text = item.name,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp),
                            )
                        }
                    }
                }
            }
        }
        load()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun load() = scope.launch {
        try {
            val rePackageSpec = """.+/""".toRegex()
            val reSkipName =
                """^(m3_|abc_|avd_|btn_checkbox_|btn_radio_|googleg_|ic_keyboard_arrow_|ic_menu_arrow_|notification_|common_|emj_|cpv_|design_|exo_|mtrl_|ic_mtrl_)"""
                    .toRegex()
            val list = withContext(AppDispatchers.IO) {
                R.drawable::class.java.fields
                    .mapNotNull {
                        val id = it.get(null) as? Int ?: return@mapNotNull null
                        val name = resources.getResourceName(id).replaceFirst(rePackageSpec, "")
                        if (reSkipName.containsMatchIn(name)) return@mapNotNull null
                        MyItem(id, name)
                    }
                    .toMutableList()
                    .apply { sortBy { it.name } }
            }
            drawableList.clear()
            drawableList.addAll(list)
        } catch (ex: Throwable) {
            log.e(ex, "load failed.")
        }
    }
}
