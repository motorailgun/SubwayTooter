package es.ariaontheplanet.quasar.actpost

import android.os.SystemClock
import es.ariaontheplanet.quasar.ActPost
import es.ariaontheplanet.quasar.api.ApiTask
import es.ariaontheplanet.quasar.api.TootApiResult
import es.ariaontheplanet.quasar.api.TootParser
import es.ariaontheplanet.quasar.api.entity.TootTag
import es.ariaontheplanet.quasar.api.runApiTask
import jp.juggler.util.coroutine.launchMain
import jp.juggler.util.data.JsonObject
import jp.juggler.util.data.wrapWeakReference
import jp.juggler.util.network.toPostRequestBuilder

class FeaturedTagCache(val list: List<TootTag>, val time: Long)

fun ActPost.updateFeaturedTags() {
    val account = account
    if (account == null || account.isPseudo) {
        return
    }

    val cache = featuredTagCache[account.acct.ascii]
    val now = SystemClock.elapsedRealtime()
    if (cache != null && now - cache.time <= 300000L) return

    // 同時に実行するタスクは1つまで
    if (jobFeaturedTag?.get()?.isActive != true) {
        jobFeaturedTag = launchMain {
            runApiTask(
                account,
                progressStyle = ApiTask.PROGRESS_NONE,
            ) { client ->
                if (account.isMisskey) {
                    client.request(
                        "/api/hashtags/trend",
                        JsonObject().toPostRequestBuilder()
                    )?.also { result ->
                        val list = TootTag.parseList(
                            TootParser(this@runApiTask, account),
                            result.jsonArray
                        )
                        featuredTagCache[account.acct.ascii] =
                            FeaturedTagCache(list, SystemClock.elapsedRealtime())
                    }
                } else {
                    val parser = TootParser(this@runApiTask, account)
                    val list = buildSet {
                        arrayOf(
                            "/api/v1/featured_tags",
                            "/api/v1/followed_tags",
                        ).forEach { path ->
                            client.request(path)?.also { result ->
                                addAll(TootTag.parseList(parser, result.jsonArray))
                            }
                        }
                    }
                    if (list.isNotEmpty()) {
                        featuredTagCache[account.acct.ascii] =
                            FeaturedTagCache(
                                list.sortedBy { it.name },
                                SystemClock.elapsedRealtime()
                            )
                    }
                }
                TootApiResult()
            }
            if (isFinishing || isDestroyed) return@launchMain
            updateFeaturedTags()
        }.wrapWeakReference
    }
}
