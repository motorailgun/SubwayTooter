package jp.juggler.subwaytooter.actmain

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import jp.juggler.subwaytooter.ActAbout
import jp.juggler.subwaytooter.ActAppSetting
import jp.juggler.subwaytooter.ActAppSetting.Companion.launchAppSetting
import jp.juggler.subwaytooter.ActFavMute
import jp.juggler.subwaytooter.ActHighlightWordList
import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.ActMutedApp
import jp.juggler.subwaytooter.ActMutedPseudoAccount
import jp.juggler.subwaytooter.ActMutedWord
import jp.juggler.subwaytooter.ActPushMessageList
import jp.juggler.subwaytooter.App1
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.action.accountAdd
import jp.juggler.subwaytooter.action.accountOpenSetting
import jp.juggler.subwaytooter.action.openColumnFromUrl
import jp.juggler.subwaytooter.action.openColumnList
import jp.juggler.subwaytooter.action.serverProfileDirectoryFromSideMenu
import jp.juggler.subwaytooter.action.timeline
import jp.juggler.subwaytooter.api.entity.TootStatus
import jp.juggler.subwaytooter.column.ColumnType
import jp.juggler.subwaytooter.compose.StThemedContent
import jp.juggler.subwaytooter.dialog.pickAccount
import jp.juggler.subwaytooter.pref.PrefB
import jp.juggler.subwaytooter.pref.PrefDevice.Companion.PUSH_DISTRIBUTOR_NONE
import jp.juggler.subwaytooter.pref.PrefS
import jp.juggler.subwaytooter.pref.prefDevice
import jp.juggler.subwaytooter.push.fcmHandler
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.subwaytooter.table.accountListCanSeeMyReactions
import jp.juggler.subwaytooter.ui.ossLicense.ActOSSLicense
import jp.juggler.subwaytooter.util.VersionString
import jp.juggler.subwaytooter.util.openBrowser
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.coroutine.launchIO
import jp.juggler.util.data.JsonObject
import jp.juggler.util.data.anyArrayOf
import jp.juggler.util.data.decodeJsonObject
import jp.juggler.util.data.decodeUTF8
import jp.juggler.util.data.notEmpty
import jp.juggler.util.getPackageInfoCompat
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class SideMenuAdapter(
    private val actMain: ActMain,
    @Suppress("unused") val handler: Handler,
    navigationView: ViewGroup,
    private val drawer: DrawerLayout,
) {

    companion object {
        private val log = LogCategory("SideMenuAdapter")

        private const val URL_APP_VERSION =
            "https://mastodon-msg.juggler.jp/appVersion/appVersion.json"
        private const val URL_GITHUB_RELEASES =
            "https://github.com/tateisu/SubwayTooter/releases"
        private const val URL_OLDER_DEVICES =
            "https://github.com/tateisu/SubwayTooter/discussions/192"

        var releaseInfo by mutableStateOf<JsonObject?>(null)
            private set

        // メインスレッドから呼ばれる
        private fun Context.checkVersion() {
            // releaseInfoが既にあり、更新時刻が十分に新しいなら情報を取得し直す必要はない
            releaseInfo?.string("updated_at")
                ?.let { TootStatus.parseTime(it) }
                ?.takeIf { it >= System.currentTimeMillis() - 86400000L }
                ?.let { return }

            // リリース情報を取得し直す
            launchIO {
                try {
                    val json = App1.getHttpCached(URL_APP_VERSION)
                        ?.decodeUTF8()
                        ?.decodeJsonObject()
                        ?: error("missing appVersion json")
                    withContext(Dispatchers.Main) {
                        releaseInfo = json
                    }
                } catch (ex: Throwable) {
                    log.e(ex, "checkVersion failed")
                }
            }
        }
    }

    private enum class ItemType(val id: Int) {
        IT_NORMAL(0),
        IT_GROUP_HEADER(1),
        IT_DIVIDER(2),
        IT_VERSION(3),
        IT_TIMEZONE(4),
        IT_NOTIFICATION_PERMISSION(5),
    }

    private class Item(
        // 項目の文字列リソース or 0: divider, 1: バージョン表記, 2: タイムゾーン
        val title: Int = 0,
        val icon: Int = 0,
        val action: ActMain.() -> Unit = {},
    ) {

        val itemType: ItemType
            get() = when {
                title == 0 -> ItemType.IT_DIVIDER
                title == 1 -> ItemType.IT_VERSION
                title == 2 -> ItemType.IT_TIMEZONE
                title == 3 -> ItemType.IT_NOTIFICATION_PERMISSION
                icon == 0 -> ItemType.IT_GROUP_HEADER
                else -> ItemType.IT_NORMAL
            }
    }

    /*
        no title => section divider
        else no icon => section header with title
        else => menu item with icon and title
    */
    private val originalList = listOf(

        Item(icon = R.drawable.ic_info_outline, title = 1),
        Item(icon = R.drawable.ic_info_outline, title = 2),
        Item(icon = R.drawable.ic_info_outline, title = 3),

        Item(),
        Item(title = R.string.account),

        Item(title = R.string.account_add, icon = R.drawable.ic_person_add) {
            accountAdd()
        },

        Item(icon = R.drawable.ic_settings, title = R.string.account_setting) {
            accountOpenSetting()
        },

        Item(icon = R.drawable.outline_delivery_dining_24, title = R.string.push_message_history) {
            startActivity(Intent(this, ActPushMessageList::class.java))
        },

        Item(),
        Item(title = R.string.column),

        Item(icon = R.drawable.ic_list_numbered, title = R.string.column_list) {
            openColumnList()
        },

        Item(icon = R.drawable.ic_close, title = R.string.close_all_columns) {
            closeColumnAll()
        },

        Item(icon = R.drawable.ic_paste, title = R.string.open_column_from_url) {
            openColumnFromUrl()
        },

        Item(icon = R.drawable.ic_home, title = R.string.home) {
            timeline(defaultInsertPosition, ColumnType.HOME)
        },

        Item(icon = R.drawable.ic_announcement, title = R.string.notifications) {
            timeline(defaultInsertPosition, ColumnType.NOTIFICATIONS)
        },

        Item(icon = R.drawable.ic_mail, title = R.string.direct_messages) {
            timeline(defaultInsertPosition, ColumnType.DIRECT_MESSAGES)
        },

        Item(icon = R.drawable.ic_share, title = R.string.misskey_hybrid_timeline_long) {
            timeline(defaultInsertPosition, ColumnType.MISSKEY_HYBRID)
        },

        Item(icon = R.drawable.ic_run, title = R.string.local_timeline) {
            timeline(defaultInsertPosition, ColumnType.LOCAL)
        },

        Item(icon = R.drawable.ic_bike, title = R.string.federate_timeline) {
            timeline(defaultInsertPosition, ColumnType.FEDERATE)
        },

        Item(icon = R.drawable.ic_list_list, title = R.string.lists) {
            timeline(defaultInsertPosition, ColumnType.LIST_LIST)
        },

        Item(icon = R.drawable.ic_satellite, title = R.string.antenna_list_misskey) {
            timeline(defaultInsertPosition, ColumnType.MISSKEY_ANTENNA_LIST)
        },

        Item(icon = R.drawable.ic_hashtag, title = R.string.followed_tags) {
            timeline(defaultInsertPosition, ColumnType.FOLLOWED_HASHTAGS)
        },

        Item(icon = R.drawable.ic_search, title = R.string.search) {
            timeline(defaultInsertPosition, ColumnType.SEARCH, args = anyArrayOf("", false))
        },

        Item(icon = R.drawable.ic_trend, title = R.string.trend_tag) {
            timeline(defaultInsertPosition, ColumnType.TREND_TAG)
        },
        Item(icon = R.drawable.ic_trend, title = R.string.trend_link) {
            timeline(defaultInsertPosition, ColumnType.TREND_LINK)
        },
        Item(icon = R.drawable.ic_trend, title = R.string.trend_post) {
            timeline(defaultInsertPosition, ColumnType.TREND_POST)
        },
        Item(icon = R.drawable.ic_star_outline, title = R.string.favourites) {
            timeline(defaultInsertPosition, ColumnType.FAVOURITES)
        },

        Item(icon = R.drawable.ic_bookmark, title = R.string.bookmarks) {
            timeline(defaultInsertPosition, ColumnType.BOOKMARKS)
        },
        Item(icon = R.drawable.ic_face, title = R.string.reactioned_posts) {
            launchAndShowError {
                accountListCanSeeMyReactions()?.let { list ->
                    if (list.isEmpty()) {
                        showToast(false, R.string.not_available_for_current_accounts)
                    } else {
                        val columnType = ColumnType.REACTIONS
                        pickAccount(
                            accountListArg = list.toMutableList(),
                            bAuto = true,
                            message = getString(
                                R.string.account_picker_add_timeline_of,
                                columnType.name1(applicationContext)
                            )
                        )?.let { addColumn(defaultInsertPosition, it, columnType) }
                    }
                }
            }
        },

        Item(icon = R.drawable.ic_account_box, title = R.string.profile) {
            timeline(defaultInsertPosition, ColumnType.PROFILE)
        },

        Item(icon = R.drawable.ic_follow_wait, title = R.string.follow_requests) {
            timeline(defaultInsertPosition, ColumnType.FOLLOW_REQUESTS)
        },

        Item(icon = R.drawable.ic_person_add, title = R.string.follow_suggestion) {
            timeline(defaultInsertPosition, ColumnType.FOLLOW_SUGGESTION)
        },

        Item(icon = R.drawable.ic_person_add, title = R.string.endorse_set) {
            timeline(defaultInsertPosition, ColumnType.ENDORSEMENT)
        },

        Item(icon = R.drawable.ic_person_add, title = R.string.profile_directory) {
            serverProfileDirectoryFromSideMenu()
        },

        Item(icon = R.drawable.ic_volume_off, title = R.string.muted_users) {
            timeline(defaultInsertPosition, ColumnType.MUTES)
        },

        Item(icon = R.drawable.ic_block, title = R.string.blocked_users) {
            timeline(defaultInsertPosition, ColumnType.BLOCKS)
        },

        Item(icon = R.drawable.ic_volume_off, title = R.string.keyword_filters) {
            timeline(defaultInsertPosition, ColumnType.KEYWORD_FILTER)
        },

        Item(icon = R.drawable.ic_cloud_off, title = R.string.blocked_domains) {
            timeline(defaultInsertPosition, ColumnType.DOMAIN_BLOCKS)
        },

        Item(icon = R.drawable.ic_timer, title = R.string.scheduled_status_list) {
            timeline(defaultInsertPosition, ColumnType.SCHEDULED_STATUS)
        },

        Item(icon = R.drawable.ic_repeat, title = R.string.agg_boosts) {
            timeline(defaultInsertPosition, ColumnType.AGG_BOOSTS)
        },

        Item(),
        Item(title = R.string.toot_search),

//        Item(icon = R.drawable.ic_search, title = R.string.mastodon_search_portal) {
//            addColumn(defaultInsertPosition, SavedAccount.na, ColumnType.SEARCH_MSP, "")
//        },
//        Item(icon = R.drawable.ic_search, title = R.string.tootsearch) {
//            addColumn(defaultInsertPosition, SavedAccount.na, ColumnType.SEARCH_TS, "")
//        },

        Item(icon = R.drawable.ic_search, title = R.string.notestock) {
            addColumn(
                defaultInsertPosition,
                SavedAccount.na,
                ColumnType.SEARCH_NOTESTOCK,
                params = arrayOf("")
            )
        },

        Item(),
        Item(title = R.string.setting),

        Item(icon = R.drawable.ic_settings, title = R.string.app_setting) {
            arAppSetting.launchAppSetting()
        },

        Item(icon = R.drawable.ic_settings, title = R.string.highlight_word) {
            startActivity(Intent(this, ActHighlightWordList::class.java))
        },

        Item(icon = R.drawable.ic_volume_off, title = R.string.muted_app) {
            startActivity(Intent(this, ActMutedApp::class.java))
        },

        Item(icon = R.drawable.ic_volume_off, title = R.string.muted_word) {
            startActivity(Intent(this, ActMutedWord::class.java))
        },

        Item(icon = R.drawable.ic_volume_off, title = R.string.fav_muted_user) {
            startActivity(Intent(this, ActFavMute::class.java))
        },

        Item(
            icon = R.drawable.ic_volume_off,
            title = R.string.muted_users_from_pseudo_account
        ) {
            startActivity(Intent(this, ActMutedPseudoAccount::class.java))
        },

        Item(icon = R.drawable.ic_info_outline, title = R.string.app_about) {

            arAbout.launch(
                Intent(this, ActAbout::class.java)
            )
        },

        Item(icon = R.drawable.ic_info_outline, title = R.string.oss_license) {
            startActivity(Intent(this, ActOSSLicense::class.java))
        },

        Item(icon = R.drawable.ic_hot_tub, title = R.string.app_exit) {
            finish()
        }
    )

    private var list by mutableStateOf(originalList)

    fun onActivityStart() {
        filterListItems()
    }

    private fun notificationActionRecommend(): Pair<Int, () -> Unit>? = when {
        // 通知権限がない場合の警告とアクション
        actMain.prNotification.spec.listNotGranded(actMain).isNotEmpty() ->
            Pair(R.string.notification_permission_not_granted) {
                actMain.prNotification.openAppSetting(actMain)
            }
        // プッシュ配送が選択されていない場合の警告とアクション
        (actMain.prefDevice.pushDistributor.isNullOrEmpty() && fcmHandler.noFcm(actMain)) ||
                actMain.prefDevice.pushDistributor == PUSH_DISTRIBUTOR_NONE ->
            Pair(R.string.notification_push_distributor_disabled) {
                actMain.selectPushDistributor()
            }

        else -> null
    }

    fun filterListItems() {
        log.i("filterListItems")
        list = originalList.filter {
            when (it.itemType) {
                ItemType.IT_NOTIFICATION_PERMISSION ->
                    notificationActionRecommend() != null

                ItemType.IT_NORMAL -> when (it.title) {
                    R.string.antenna_list_misskey,
                    R.string.misskey_hybrid_timeline_long,
                        -> PrefB.bpEnableDeprecatedSomething.value

                    else -> true
                }

                else -> true
            }
        }
    }

    private fun getTimeZoneString(context: Context): String {
        try {
            var tz = TimeZone.getDefault()
            val tzId = PrefS.spTimeZone.value
            if (tzId.isBlank()) {
                return tz.displayName + "(" + context.getString(R.string.device_timezone) + ")"
            }
            tz = TimeZone.getTimeZone(tzId)
            var offset = tz.rawOffset.toLong()
            return when (offset) {
                0L -> "(UTC\u00B100:00) ${tz.id} ${tz.displayName}"
                else -> {

                    val format = when {
                        offset > 0 -> "(UTC+%02d:%02d) %s %s"
                        else -> "(UTC-%02d:%02d) %s %s"
                    }

                    offset = abs(offset)

                    val hours = TimeUnit.MILLISECONDS.toHours(offset)
                    val minutes =
                        TimeUnit.MILLISECONDS.toMinutes(offset) - TimeUnit.HOURS.toMinutes(hours)

                    String.format(format, hours, minutes, tz.id, tz.displayName)
                }
            }
        } catch (ex: Throwable) {
            log.w(ex, "getTimeZoneString failed.")
            return "(incorrect TimeZone)"
        }
    }

    @Composable
    private fun SideMenuContent() {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(list) { item ->
                when (item.itemType) {
                    ItemType.IT_DIVIDER -> DividerItem()
                    ItemType.IT_GROUP_HEADER -> GroupHeaderItem(item)
                    ItemType.IT_NORMAL -> NormalItem(item)
                    ItemType.IT_VERSION -> VersionItem()
                    ItemType.IT_TIMEZONE -> TimezoneItem()
                    ItemType.IT_NOTIFICATION_PERMISSION -> NotificationPermissionItem(item)
                }
            }
        }
    }

    @Composable
    private fun DividerItem() {
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 3.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            thickness = 1.dp,
        )
    }

    @Composable
    private fun GroupHeaderItem(item: Item) {
        Text(
            text = stringResource(item.title),
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(
                start = 12.dp,
                top = 12.dp,
                end = 12.dp,
                bottom = 6.dp,
            ),
        )
    }

    @Composable
    private fun NormalItem(item: Item) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable {
                    item.action(actMain)
                    drawer.closeDrawer(GravityCompat.START)
                }
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .heightIn(min = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(item.icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(item.title),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }

    @Composable
    private fun VersionItem() {
        val currentVersion = remember {
            try {
                actMain.packageManager
                    .getPackageInfoCompat(actMain.packageName)?.versionName ?: "??"
            } catch (_: Throwable) {
                "??"
            }
        }

        val info = releaseInfo
        val newRelease = info?.jsonObject(
            if (PrefB.bpCheckBetaVersion.value) "beta" else "stable"
        )

        val newVersion =
            (newRelease?.string("name")?.notEmpty() ?: newRelease?.string("tag_name"))
                ?.replace("""(v(ersion)?)\s*""".toRegex(RegexOption.IGNORE_CASE), "")
                ?.trim()
                ?.notEmpty()
                ?.takeIf {
                    log.i("newVersion=$it, currentVersion=$currentVersion")
                    VersionString(it) > VersionString(currentVersion)
                }

        val releaseMinSdkVersion = newRelease?.int("minSdkVersion")
            ?: Build.VERSION.SDK_INT
        val releaseMinSdkVersionScheduled = newRelease?.int("minSdkVersionScheduled")
            ?: Build.VERSION.SDK_INT

        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.app_name_with_version,
                    stringResource(R.string.app_name),
                    currentVersion,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                lineHeight = (18 * 1.1).sp,
            )

            when {
                // 新しいバージョンがある、この端末にインストール可能である
                newVersion != null && Build.VERSION.SDK_INT >= releaseMinSdkVersion -> {
                    Text(
                        text = stringResource(R.string.new_version_available, newVersion),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 18.sp,
                        lineHeight = (18 * 1.1).sp,
                    )
                    newRelease?.string("html_url")?.let { url ->
                        Text(
                            text = stringResource(R.string.release_note_with_assets),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 18.sp,
                            lineHeight = (18 * 1.1).sp,
                            modifier = Modifier.clickable { actMain.openBrowser(url) },
                        )
                    }
                }

                // 通常時は更新履歴へのリンク
                else -> Text(
                    text = stringResource(R.string.release_note),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp,
                    lineHeight = (18 * 1.1).sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { actMain.openBrowser(URL_GITHUB_RELEASES) },
                )
            }

            // 端末のOSバージョンがサポートから外れる予定なら、リンクを追加する
            if (Build.VERSION.SDK_INT < releaseMinSdkVersionScheduled) {
                Text(
                    text = stringResource(R.string.old_devices_warning),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp,
                    lineHeight = (18 * 1.1).sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { actMain.openBrowser(URL_OLDER_DEVICES) },
                )
            }
        }
    }

    @Composable
    private fun TimezoneItem() {
        Text(
            text = getTimeZoneString(actMain),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }

    @Composable
    private fun NotificationPermissionItem(item: Item) {
        val action = notificationActionRecommend() ?: return
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable {
                    drawer.closeDrawer(GravityCompat.START)
                    notificationActionRecommend()?.second?.invoke()
                    filterListItems()
                }
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .heightIn(min = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(item.icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(action.first),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }

    init {
        actMain.applicationContext.checkVersion()
        filterListItems()

        ComposeView(actMain).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
            setContent {
                StThemedContent {
                    SideMenuContent()
                }
            }
            navigationView.addView(this)
        }
    }
}
