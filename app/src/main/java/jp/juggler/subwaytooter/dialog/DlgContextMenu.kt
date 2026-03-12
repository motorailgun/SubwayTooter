package jp.juggler.subwaytooter.dialog

import android.app.Dialog
import android.view.WindowManager
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.ActText
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.action.*
import jp.juggler.subwaytooter.actmain.nextPosition
import jp.juggler.subwaytooter.api.entity.*
import jp.juggler.subwaytooter.column.Column
import jp.juggler.subwaytooter.column.ColumnType
import jp.juggler.subwaytooter.compose.StExtendedColors
import jp.juggler.subwaytooter.compose.StThemedContent
import jp.juggler.subwaytooter.compose.StThemeEx
import jp.juggler.subwaytooter.pref.PrefB
import jp.juggler.subwaytooter.span.MyClickableSpan
import jp.juggler.subwaytooter.table.*
import jp.juggler.subwaytooter.util.*
import jp.juggler.util.data.*
import jp.juggler.util.ui.*

private data class LinkItem(val caption: String, val action: () -> Unit)

internal class DlgContextMenu(
    val activity: ActMain,
    private val column: Column,
    private val whoRef: TootAccountRef?,
    private val status: TootStatus?,
    private val notification: TootNotification? = null,
    private val contentTextView: TextView? = null,
) {
    private val accessInfo = column.accessInfo
    private val who = whoRef?.get()
    private val relation: UserRelation

    private val dialog: Dialog

    // Pre-computed link items from decoded_content spans
    private val linkItems: List<LinkItem>

    // Pre-computed visibility flags
    private val statusByMe: Boolean
    private val hasEditHistory: Boolean
    private val hasTranslateApp: Boolean
    private val canEdit: Boolean
    private val canPin: Boolean
    private val applicationName: String?
    private val bShowConversationMute: Boolean
    private val muted: Boolean
    private val accountListNonPseudo: List<SavedAccount>

    init {
        relation = when {
            who == null -> UserRelation()
            accessInfo.isPseudo -> daoUserRelation.loadPseudo(accessInfo.getFullAcct(who))
            else -> daoUserRelation.load(accessInfo.db_id, who.id)
        }

        statusByMe = status != null && accessInfo.isMe(status.account)

        hasEditHistory = status != null &&
                status.time_edited_at > 0L &&
                column.type != ColumnType.STATUS_HISTORY

        hasTranslateApp = CustomShare.hasTranslateApp(CustomShareTarget.Translate, activity)

        canEdit = statusByMe && (TootInstance.getCached(accessInfo)?.let {
            when {
                it.isMastodon && it.versionGE(TootInstance.VERSION_3_5_0_rc1) -> true
                it.pleromaFeatures?.contains("editing") == true -> true
                else -> false
            }
        } ?: false)

        canPin = status?.canPin(accessInfo) == true

        applicationName = status?.application?.name?.notEmpty()

        bShowConversationMute = when {
            status == null -> false
            accessInfo.isMe(status.account) -> true
            notification != null && NotificationType.Mention == notification.type -> true
            else -> false
        }

        muted = status?.muted == true

        accountListNonPseudo = daoSavedAccount.loadAccountList().filter { !it.isPseudo }

        linkItems = buildList {
            if (status != null && PrefB.bpLinksInContextMenu.value && contentTextView != null) {
                val dc = status.decoded_content
                for (span in dc.getSpans(0, dc.length, MyClickableSpan::class.java)) {
                    val caption = span.linkInfo.text
                    val displayText = when (caption.firstOrNull()) {
                        '@', '#' -> caption
                        else -> span.linkInfo.url
                    }
                    val tv = contentTextView
                    add(LinkItem(displayText) {
                        dialog.dismissSafe()
                        span.onClick(tv)
                    })
                }
            }
        }

        dialog = Dialog(activity).apply {
            setCancelable(true)
            setCanceledOnTouchOutside(true)
        }
        val composeView = ComposeView(activity).apply {
            setContent {
                StThemedContent {
                    ContextMenuContent()
                }
            }
        }
        dialog.setContentView(composeView)
    }

    fun show() {
        dialog.window?.let { w ->
            w.attributes = w.attributes.apply {
                width = (0.5f + 280f * activity.density).toInt()
                height = WindowManager.LayoutParams.WRAP_CONTENT
            }
        }
        dialog.show()
    }

    private fun dismissAndDo(action: ActMain.() -> Unit) {
        dialog.dismissSafe()
        activity.action()
    }

    private fun getUserApiHost(): Host =
        when (val whoHost = whoRef?.get()?.apiHost) {
            Host.UNKNOWN -> Host.parse(column.instanceUri)
            Host.EMPTY, null -> accessInfo.apiHost
            else -> whoHost
        }

    private fun getUserApDomain(): Host =
        when (val whoHost = whoRef?.get()?.apDomain) {
            Host.UNKNOWN -> Host.parse(column.instanceUri)
            Host.EMPTY, null -> accessInfo.apDomain
            else -> whoHost
        }

    // =========================================
    // Compose UI
    // =========================================

    @Composable
    private fun SectionLabel(textResId: Int) {
        Text(
            text = stringResource(textResId),
            modifier = Modifier.padding(horizontal = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
    }

    @Composable
    private fun MenuBtn(textResId: Int, action: ActMain.() -> Unit) {
        TextButton(
            onClick = { dismissAndDo(action) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(textResId),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
            )
        }
    }

    @Composable
    private fun MenuBtn(text: String, action: ActMain.() -> Unit) {
        TextButton(
            onClick = { dismissAndDo(action) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
            )
        }
    }

    @Composable
    private fun ExpandableSection(
        labelResId: Int,
        alwaysExpand: Boolean,
        content: @Composable () -> Unit,
    ) {
        var expanded by remember { mutableStateOf(alwaysExpand) }
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(labelResId),
                modifier = Modifier.fillMaxWidth(),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 24.dp)) {
                content()
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun ContextMenuContent() {
        val extColors = StThemeEx.colors

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                ) {
                    LinksSection()
                    StatusSection()
                    NotificationSection()
                    AccountSection(extColors)
                    InstanceSection()
                }
                HorizontalDivider()
                TextButton(
                    onClick = { dialog.cancel() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }

    @Composable
    private fun LinksSection() {
        if (linkItems.isEmpty()) return
        HorizontalDivider(modifier = Modifier.padding(top = 2.dp, bottom = 6.dp))
        linkItems.forEach { item ->
            TextButton(
                onClick = item.action,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = item.caption,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start,
                )
            }
        }
    }

    @Composable
    private fun StatusSection() {
        val status = this.status ?: return
        val alwaysExpand = PrefB.bpAlwaysExpandContextMenuItems.value

        SectionLabel(R.string.actions_for_status)

        // Icon bar
        Row(modifier = Modifier.fillMaxWidth()) {
            val iconTint = MaterialTheme.colorScheme.onSurface
            if (canEdit) {
                IconButton(onClick = { dismissAndDo { statusEdit(accessInfo, status) } }) {
                    Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.edit), tint = iconTint)
                }
            }
            if (hasTranslateApp) {
                IconButton(onClick = {
                    dismissAndDo {
                        CustomShare.invokeStatusText(CustomShareTarget.Translate, activity, accessInfo, status)
                    }
                }) {
                    Icon(painterResource(R.drawable.ic_translate), stringResource(R.string.translate), tint = iconTint)
                }
            }
            if (hasEditHistory) {
                IconButton(onClick = {
                    dismissAndDo { openStatusHistory(nextPosition(column), accessInfo, status) }
                }) {
                    Icon(painterResource(R.drawable.ic_history), stringResource(R.string.edit_history), tint = iconTint)
                }
            }
            Spacer(Modifier.weight(1f))
            if (statusByMe) {
                IconButton(onClick = { dismissAndDo { clickStatusDelete(accessInfo, status) } }) {
                    Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.delete), tint = iconTint)
                }
            }
        }

        if (hasEditHistory) {
            MenuBtn(
                activity.getString(R.string.edit_history) + "\n" +
                        TootStatus.formatTime(activity, status.time_edited_at, bAllowRelative = false)
            ) { openStatusHistory(nextPosition(column), accessInfo, status) }
        }
        MenuBtn(R.string.open_web_page) { openCustomTab(status.url) }
        MenuBtn(R.string.select_and_copy) { launchActText(ActText.createIntent(this, accessInfo, status)) }
        if (hasTranslateApp) {
            MenuBtn(R.string.translate) {
                CustomShare.invokeStatusText(CustomShareTarget.Translate, activity, accessInfo, status)
            }
        }
        MenuBtn(R.string.quote_url) { openPost(status.url?.notEmpty()) }
        MenuBtn(R.string.share_url_more) { shareText(status.url?.notEmpty()) }

        // Cross-account
        ExpandableSection(R.string.cross_account_actions, alwaysExpand) {
            MenuBtn(R.string.conversation_view) { conversationOtherInstance(nextPosition(column), status) }
            MenuBtn(R.string.reply) { replyFromAnotherAccount(accessInfo, status) }
            MenuBtn(R.string.boost) { boostFromAnotherAccount(accessInfo, status) }
            MenuBtn(R.string.favourite) { favouriteFromAnotherAccount(accessInfo, status) }
            MenuBtn(R.string.bookmark) { bookmarkFromAnotherAccount(accessInfo, status) }
            MenuBtn(R.string.reaction) { reactionFromAnotherAccount(accessInfo, status) }
            MenuBtn(R.string.quote) { quoteFromAnotherAccount(accessInfo, status) }
            if (status.reblogParent != null) {
                MenuBtn(R.string.quote_toot_bt) { quoteFromAnotherAccount(accessInfo, status.reblogParent) }
            }
        }

        // Around
        if (who != null) {
            ExpandableSection(R.string.around_this_toot, alwaysExpand) {
                MenuBtn(R.string.account_timeline) { clickAroundAccountTL(accessInfo, nextPosition(column), who, status) }
                MenuBtn(R.string.local_timeline) { clickAroundLTL(accessInfo, nextPosition(column), who, status) }
                MenuBtn(R.string.federate_timeline) { clickAroundFTL(accessInfo, nextPosition(column), who, status) }
            }
        }

        // By me
        if (statusByMe) {
            ExpandableSection(R.string.your_toot, alwaysExpand) {
                if (canEdit) {
                    MenuBtn(R.string.edit) { statusEdit(accessInfo, status) }
                }
                MenuBtn(R.string.redraft_and_delete) { statusRedraft(accessInfo, status) }
                if (canPin && status.pinned) {
                    MenuBtn(R.string.profile_unpin) { statusPin(accessInfo, status, false) }
                }
                if (canPin && !status.pinned) {
                    MenuBtn(R.string.profile_pin) { statusPin(accessInfo, status, true) }
                }
                MenuBtn(R.string.delete) { clickStatusDelete(accessInfo, status) }
            }
        }

        // Extra
        ExpandableSection(R.string.extra_actions, alwaysExpand) {
            MenuBtn(R.string.boosted_by) {
                clickBoostBy(nextPosition(column), accessInfo, status, ColumnType.BOOSTED_BY)
            }
            MenuBtn(R.string.favourited_by) {
                clickBoostBy(nextPosition(column), accessInfo, status, ColumnType.FAVOURITED_BY)
            }
            if (!accessInfo.isPseudo && !accessInfo.isMisskey) {
                MenuBtn(R.string.boost_with_visibility) { clickBoostWithVisibility(accessInfo, status) }
            }
            if (!statusByMe && applicationName != null) {
                MenuBtn(activity.getString(R.string.mute_app_of, applicationName)) {
                    appMute(status.application)
                }
            }
            if (bShowConversationMute) {
                MenuBtn(
                    if (muted) R.string.unmute_this_conversation
                    else R.string.mute_this_conversation
                ) { conversationMute(accessInfo, status) }
            }
            if (!(statusByMe || accessInfo.isPseudo)) {
                if (who != null) {
                    MenuBtn(R.string.report) { userReportForm(accessInfo, who, status) }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(top = 2.dp))
    }

    @Composable
    private fun NotificationSection() {
        if (notification == null) return

        SectionLabel(R.string.actions_for_notification)
        MenuBtn(R.string.delete_this_notification) { notificationDeleteOne(accessInfo, notification) }
        HorizontalDivider(modifier = Modifier.padding(top = 2.dp))
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun AccountSection(extColors: StExtendedColors) {
        val who = this.who ?: return
        val whoRef = this.whoRef ?: return
        val alwaysExpand = PrefB.bpAlwaysExpandContextMenuItems.value
        val pos = activity.nextPosition(column)

        SectionLabel(R.string.actions_for_user)

        // Account action bar: Follow, Mute, Block
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val colorNormal = MaterialTheme.colorScheme.onSurface

            // Follow button with followed-by indicator
            Box(modifier = Modifier.size(48.dp)) {
                val followIconRes = when {
                    relation.getRequested(who) -> R.drawable.ic_follow_wait
                    relation.getFollowing(who) -> R.drawable.ic_follow_cross
                    else -> R.drawable.ic_follow_plus
                }
                val followTint = when {
                    accessInfo.isPseudo -> colorNormal
                    relation.getRequested(who) -> extColors.buttonAccentFollowRequest
                    relation.getFollowing(who) -> extColors.buttonAccentFollow
                    else -> colorNormal
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .combinedClickable(
                            onClick = { dismissAndDo { clickFollow(pos, accessInfo, whoRef, relation) } },
                            onLongClick = { dismissAndDo { followFromAnotherAccount(pos, accessInfo, who) } },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(followIconRes),
                        contentDescription = stringResource(R.string.follow),
                        tint = followTint,
                    )
                }
                if (!accessInfo.isPseudo && relation.followed_by) {
                    Icon(
                        painterResource(R.drawable.ic_follow_dot),
                        contentDescription = null,
                        tint = extColors.buttonAccentFollow,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Mute button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .combinedClickable(
                        onClick = { dismissAndDo { clickMute(accessInfo, who, relation) } },
                        onLongClick = { activity.userMuteFromAnotherAccount(who, accessInfo) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_volume_off),
                    contentDescription = stringResource(R.string.mute),
                    tint = if (relation.muting) extColors.buttonAccentFollow else colorNormal,
                )
            }

            // Block button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .combinedClickable(
                        onClick = { dismissAndDo { clickBlock(accessInfo, who, relation) } },
                        onLongClick = { activity.userBlockFromAnotherAccount(who, accessInfo) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_block),
                    contentDescription = stringResource(R.string.block),
                    tint = if (relation.blocking) extColors.buttonAccentFollow else colorNormal,
                )
            }
        }

        if (!accessInfo.isPseudo) {
            MenuBtn(R.string.open_profile) { userProfileLocal(pos, accessInfo, who) }
        }
        MenuBtn(R.string.open_web_page) { openCustomTab(who.url) }
        MenuBtn(R.string.select_and_copy) { launchActText(ActText.createIntent(this, accessInfo, who)) }
        if (!accessInfo.isPseudo) {
            MenuBtn(R.string.send_message) { mention(accessInfo, who) }
        }
        MenuBtn(R.string.quote_url) { openPost(who.url?.notEmpty()) }
        MenuBtn(R.string.share_url_more) { shareText(who.url?.notEmpty()) }
        MenuBtn(R.string.quote_name) { quoteName(who) }

        if (column.type == ColumnType.FOLLOW_REQUESTS) {
            MenuBtn(R.string.follow_request_ok) { followRequestAuthorize(accessInfo, whoRef, true) }
            MenuBtn(R.string.follow_request_ng) { followRequestAuthorize(accessInfo, whoRef, false) }
        }
        MenuBtn(R.string.list_member_add_remove) {
            openDlgListMember(who, accessInfo.getFullAcct(who), accessInfo)
        }
        if (!(accessInfo.isPseudo || accessInfo.isMe(who))) {
            MenuBtn(R.string.report) { userReportForm(accessInfo, who) }
        }

        // Cross-account
        ExpandableSection(R.string.cross_account_actions, alwaysExpand) {
            MenuBtn(R.string.open_profile) { userProfileFromAnotherAccount(pos, accessInfo, who) }
            if (accountListNonPseudo.isNotEmpty()) {
                MenuBtn(R.string.follow) { followFromAnotherAccount(pos, accessInfo, who) }
                MenuBtn(R.string.send_message) { mentionFromAnotherAccount(accessInfo, who) }
            }
        }

        // Extra
        ExpandableSection(R.string.extra_actions, alwaysExpand) {
            if (!accessInfo.isPseudo && accessInfo.isMastodon && relation.following) {
                MenuBtn(
                    if (relation.notifying) R.string.stop_notify_posts_from_this_user
                    else R.string.notify_posts_from_this_user
                ) { clickStatusNotification(accessInfo, who, relation) }
            }
            MenuBtn(R.string.nickname_and_color_and_notification_sound) { clickNicknameCustomize(accessInfo, who) }
            MenuBtn(R.string.show_avatar_image) { openAvatarImage(who) }
            MenuBtn(R.string.qr_code) {
                activity.dialogQrCode(
                    message = whoRef.decoded_display_name,
                    url = who.getUserUrl()
                )
            }
            if (!accessInfo.isPseudo) {
                MenuBtn(R.string.notifications_from_acct) { clickNotificationFrom(pos, accessInfo, who) }
                MenuBtn(
                    if (relation.endorsed) R.string.endorse_unset else R.string.endorse_set
                ) { userEndorsement(accessInfo, who, !relation.endorsed) }
            }

            // Hide/show boost
            if (!accessInfo.isPseudo && relation.getFollowing(who) &&
                relation.following_reblogs != UserRelation.REBLOG_UNKNOWN
            ) {
                if (relation.following_reblogs == UserRelation.REBLOG_SHOW) {
                    MenuBtn(R.string.hide_boost_in_home) { userSetShowBoosts(accessInfo, who, false) }
                } else {
                    MenuBtn(R.string.show_boost_in_home) { userSetShowBoosts(accessInfo, who, true) }
                }
            }

            // Hide/show favourite
            if (daoFavMute.contains(accessInfo.getFullAcct(who))) {
                MenuBtn(R.string.show_favourite_notification_from_user) { clickShowFavourite(accessInfo, who) }
            } else {
                MenuBtn(R.string.hide_favourite_notification_from_user) { clickHideFavourite(accessInfo, who) }
            }

            if (column.type == ColumnType.FOLLOW_SUGGESTION) {
                MenuBtn(R.string.delete_suggestion) { userSuggestionDelete(accessInfo, who) }
            }

            MenuBtn(
                activity.getString(R.string.copy_account_id, who.id.toString())
            ) { who.id.toString().copyToClipboard(activity) }

            if (!accessInfo.isPseudo) {
                MenuBtn(R.string.open_in_admin_ui) {
                    openBrowser("https://${accessInfo.apiHost.ascii}/admin/accounts/${who.id}")
                }
                MenuBtn(
                    activity.getString(R.string.open_in_admin_ui) + " (instance)"
                ) {
                    openBrowser("https://${accessInfo.apiHost.ascii}/admin/instances/${who.apDomain.ascii}")
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(top = 2.dp))
    }

    @Composable
    private fun InstanceSection() {
        val whoApiHost = getUserApiHost()
        val whoApDomain = getUserApDomain()
        if (!whoApiHost.isValid) return

        Text(
            text = activity.getString(R.string.instance_actions_for, whoApDomain.pretty),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )

        MenuBtn(R.string.local_timeline) {
            who?.apiHost?.valid()?.let { timelineLocal(nextPosition(column), it) }
        }
        MenuBtn(R.string.instance_information) { serverInformation(nextPosition(column), whoApiHost) }
        MenuBtn(R.string.profile_directory) { serverProfileDirectoryFromInstanceInformation(column, whoApiHost) }

        if (!(accessInfo.isPseudo || accessInfo.matchHost(whoApiHost))) {
            if (who != null) {
                MenuBtn(R.string.block_domain) { clickDomainBlock(accessInfo, who) }
            }
        }

        if (PrefB.bpEnableDomainTimeline.value && !accessInfo.isPseudo && !accessInfo.isMisskey) {
            if (who != null) {
                MenuBtn(R.string.fedibird_domain_timeline) {
                    who.apiHost.valid()?.let { timelineDomain(nextPosition(column), accessInfo, it) }
                }
            }
        }
    }
}
