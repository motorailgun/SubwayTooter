package jp.juggler.subwaytooter.columnviewholder

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import jp.juggler.subwaytooter.*
import jp.juggler.subwaytooter.action.followFromAnotherAccount
import jp.juggler.subwaytooter.action.userProfileLocal
import jp.juggler.subwaytooter.actmain.nextPosition
import jp.juggler.subwaytooter.api.MisskeyAccountDetailMap
import jp.juggler.subwaytooter.api.TootApiResult
import jp.juggler.subwaytooter.api.entity.TootAccount
import jp.juggler.subwaytooter.api.entity.TootAccountRef
import jp.juggler.subwaytooter.api.entity.TootStatus
import jp.juggler.subwaytooter.api.runApiTask
import jp.juggler.subwaytooter.column.*
import jp.juggler.subwaytooter.dialog.showTextInputDialog
import jp.juggler.subwaytooter.emoji.EmojiMap
import jp.juggler.subwaytooter.itemviewholder.DlgContextMenu
import jp.juggler.subwaytooter.pref.PrefB
import jp.juggler.subwaytooter.span.EmojiImageSpan
import jp.juggler.subwaytooter.span.LinkInfo
import jp.juggler.subwaytooter.span.MyClickableSpan
import jp.juggler.subwaytooter.span.createSpan
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.subwaytooter.table.UserRelation
import jp.juggler.subwaytooter.table.daoAcctColor
import jp.juggler.subwaytooter.table.daoUserRelation
import jp.juggler.subwaytooter.util.*
import jp.juggler.subwaytooter.view.MyLinkMovementMethod
import jp.juggler.subwaytooter.view.MyNetworkImageView
import jp.juggler.subwaytooter.view.MyTextView
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.data.buildJsonObject
import jp.juggler.util.data.notEmpty
import jp.juggler.util.data.notZero
import jp.juggler.util.log.showToast
import jp.juggler.util.network.toPostRequestBuilder
import jp.juggler.util.ui.InputTypeEx
import jp.juggler.util.ui.attrColor
import jp.juggler.util.ui.dp
import jp.juggler.util.ui.getSpannedString
import jp.juggler.util.ui.setIconDrawableId
import jp.juggler.util.ui.vg

internal class ViewHolderHeaderProfile(
    override val activity: ActMain,
    parent: ViewGroup,
) : ViewHolderHeaderBase(
    LinearLayout(parent.context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        orientation = LinearLayout.VERTICAL
        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        val pad = context.dp(12)
        setPaddingRelative(pad, 0, pad, 0)
    }
), View.OnClickListener, View.OnLongClickListener {

    companion object {
        private fun SpannableStringBuilder.appendSpan(text: String, span: Any) {
            val start = length
            append(text)
            setSpan(
                span,
                start,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private var whoRef: TootAccountRef? = null
    private var movedRef: TootAccountRef? = null

    private lateinit var tvMoved: TextView
    private lateinit var llMoved: LinearLayout
    private lateinit var ivMoved: MyNetworkImageView
    private lateinit var tvMovedName: TextView
    private lateinit var tvMovedAcct: TextView
    private lateinit var btnMoved: ImageButton
    private lateinit var ivMovedBy: ImageView
    private lateinit var tvCreated: TextView
    private lateinit var ivBackground: MyNetworkImageView
    private lateinit var llProfile: LinearLayout
    private lateinit var ivAvatar: MyNetworkImageView
    private lateinit var tvDisplayName: TextView
    private lateinit var tvAcct: TextView
    private lateinit var tvNote: MyTextView
    private lateinit var tvMisskeyExtra: MyTextView
    private lateinit var btnFollow: ImageButton
    private lateinit var ivFollowedBy: ImageView
    private lateinit var llFields: LinearLayout
    private lateinit var tvFeaturedTags: TextView
    private lateinit var tvLastStatusAt: TextView
    private lateinit var tvPersonalNotes: TextView
    private lateinit var btnPersonalNotesEdit: ImageButton
    private lateinit var tvRemoteProfileWarning: TextView
    private lateinit var btnStatusCount: Button
    private lateinit var btnFollowing: Button
    private lateinit var btnFollowers: Button
    private lateinit var btnMore: ImageButton

    private val nameInvalidator1: NetworkEmojiInvalidator
    private val noteInvalidator: NetworkEmojiInvalidator
    private val movedCaptionInvalidator: NetworkEmojiInvalidator
    private val movedNameInvalidator: NetworkEmojiInvalidator

    private val density: Float

    private var colorTextContent = 0

    private var relation: UserRelation? = null

    init {
        val root = itemView as LinearLayout
        root.tag = this
        val holder = this
        val ctx = activity

        density = ctx.resources.displayMetrics.density

        // tvMoved
        tvMoved = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                val m12 = ctx.dp(12)
                setMargins(m12, ctx.dp(6), m12, ctx.dp(3))
            }
            compoundDrawablePadding = ctx.dp(4)
            gravity = Gravity.CENTER
        }
        root.addView(tvMoved)

        // llMoved (horizontal)
        llMoved = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                val m12 = ctx.dp(12)
                setMargins(m12, 0, m12, 0)
            }
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.btn_bg_transparent_round6dp)
            gravity = Gravity.CENTER_VERTICAL
        }

        ivMoved = MyNetworkImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(ctx.dp(48), ctx.dp(40)).apply {
                marginEnd = ctx.dp(4)
            }
            scaleType = ImageView.ScaleType.FIT_END
        }
        llMoved.addView(ivMoved)

        val llMovedInfo = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            orientation = LinearLayout.VERTICAL
        }

        tvMovedName = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        llMovedInfo.addView(tvMovedName)

        tvMovedAcct = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val p4 = ctx.dp(4)
            setPaddingRelative(p4, 0, p4, 0)
            textSize = 12f
        }
        llMovedInfo.addView(tvMovedAcct)

        llMoved.addView(llMovedInfo)

        val flMovedFollow = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(ctx.dp(40), ctx.dp(40)).apply {
                marginStart = ctx.dp(4)
            }
        }
        btnMoved = ImageButton(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundResource(R.drawable.btn_bg_transparent_round6dp)
            contentDescription = ctx.getString(R.string.follow)
            scaleType = ImageView.ScaleType.CENTER
        }
        flMovedFollow.addView(btnMoved)

        ivMovedBy = ImageView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER
            setImageResource(R.drawable.ic_follow_dot)
        }
        flMovedFollow.addView(ivMovedBy)

        llMoved.addView(flMovedFollow)
        root.addView(llMoved)

        // tvCreated
        tvCreated = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.END
            textSize = 12f
        }
        root.addView(tvCreated)

        // FrameLayout for background + profile + follow button
        val flBgProfile = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        ivBackground = MyNetworkImageView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            measureProfileBg = true
        }
        flBgProfile.addView(ivBackground)

        llProfile = LinearLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_HORIZONTAL
            orientation = LinearLayout.VERTICAL
            val p12 = ctx.dp(12)
            setPadding(p12, p12, p12, p12)
        }

        ivAvatar = MyNetworkImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(ctx.dp(128), ctx.dp(128)).apply {
                topMargin = ctx.dp(20)
                gravity = Gravity.CENTER_HORIZONTAL
            }
            setBackgroundResource(R.drawable.btn_bg_transparent_round6dp)
        }
        llProfile.addView(ivAvatar)

        tvDisplayName = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = ctx.dp(8) }
            gravity = Gravity.CENTER
            textSize = 20f
        }
        llProfile.addView(tvDisplayName)

        tvAcct = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            compoundDrawablePadding = ctx.dp(4)
            gravity = Gravity.CENTER
            setTextColor(ctx.attrColor(R.attr.colorLink))
        }
        llProfile.addView(tvAcct)

        tvNote = MyTextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_HORIZONTAL
            movementMethod = MyLinkMovementMethod
        }
        llProfile.addView(tvNote)

        tvMisskeyExtra = MyTextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_HORIZONTAL
        }
        llProfile.addView(tvMisskeyExtra)

        flBgProfile.addView(llProfile)

        // Follow button overlay
        val flFollow = FrameLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(ctx.dp(40), ctx.dp(40)).apply {
                marginStart = ctx.dp(12)
                topMargin = ctx.dp(12)
            }
        }
        btnFollow = ImageButton(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundResource(R.drawable.btn_bg_transparent_round6dp)
            contentDescription = ctx.getString(R.string.follow)
            scaleType = ImageView.ScaleType.CENTER
        }
        flFollow.addView(btnFollow)

        ivFollowedBy = ImageView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER
            setImageResource(R.drawable.ic_follow_dot)
        }
        flFollow.addView(ivFollowedBy)
        flBgProfile.addView(flFollow)

        root.addView(flBgProfile)

        // llFields
        llFields = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = ctx.dp(3) }
            orientation = LinearLayout.VERTICAL
        }
        root.addView(llFields)

        // tvFeaturedTags
        tvFeaturedTags = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = ctx.dp(3) }
        }
        root.addView(tvFeaturedTags)

        // tvLastStatusAt
        tvLastStatusAt = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = ctx.dp(3) }
            gravity = Gravity.CENTER
            textSize = 12f
        }
        root.addView(tvLastStatusAt)

        // Personal notes label
        TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = ctx.dp(8) }
            gravity = Gravity.START
            text = ctx.getString(R.string.personal_notes)
        }.also { root.addView(it) }

        // Personal notes row
        val llNotesRow = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.START or Gravity.TOP
            orientation = LinearLayout.HORIZONTAL
        }

        tvPersonalNotes = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.START
            val p12 = ctx.dp(12)
            setPadding(p12, p12, p12, p12)
            text = ctx.getString(R.string.personal_notes)
        }
        llNotesRow.addView(tvPersonalNotes)

        btnPersonalNotesEdit = ImageButton(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(ctx.dp(40), ctx.dp(40)).apply {
                marginStart = ctx.dp(4)
            }
            setBackgroundResource(R.drawable.btn_bg_transparent_round6dp)
            contentDescription = ctx.getString(R.string.edit)
            setImageResource(R.drawable.ic_edit)
        }
        llNotesRow.addView(btnPersonalNotesEdit)
        root.addView(llNotesRow)

        // tvRemoteProfileWarning
        tvRemoteProfileWarning = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = ctx.dp(3) }
            setBackgroundResource(R.drawable.btn_bg_transparent_round6dp)
            gravity = Gravity.CENTER
            val p12 = ctx.dp(12)
            setPadding(p12, p12, p12, p12)
            text = ctx.getString(R.string.remote_profile_warning)
        }
        root.addView(tvRemoteProfileWarning)

        // Bottom button row
        val llButtons = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = ctx.dp(3) }
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
        }

        btnStatusCount = Button(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setBackgroundResource(R.drawable.btn_bg_transparent_round6dp)
        }
        llButtons.addView(btnStatusCount)

        btnFollowing = Button(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setBackgroundResource(R.drawable.btn_bg_transparent_round6dp)
        }
        llButtons.addView(btnFollowing)

        btnFollowers = Button(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setBackgroundResource(R.drawable.btn_bg_transparent_round6dp)
        }
        llButtons.addView(btnFollowers)

        btnMore = ImageButton(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundResource(R.drawable.btn_bg_transparent_round6dp)
            contentDescription = ctx.getString(R.string.more)
            minimumWidth = ctx.dp(48)
            val p4 = ctx.dp(4)
            setPaddingRelative(p4, 0, p4, 0)
            setImageResource(R.drawable.ic_more)
        }
        llButtons.addView(btnMore)

        root.addView(llButtons)

        // Set click listeners
        for (v in arrayOf(
            ivBackground,
            btnFollowing,
            btnFollowers,
            btnStatusCount,
            btnMore,
            btnFollow,
            tvRemoteProfileWarning,
            btnPersonalNotesEdit,
            btnMoved,
            llMoved,
        )) {
            v.setOnClickListener(holder)
        }

        btnMoved.setOnLongClickListener(holder)
        btnFollow.setOnLongClickListener(holder)

        // Initialize invalidators
        nameInvalidator1 = NetworkEmojiInvalidator(activity.handler, tvDisplayName)
        noteInvalidator = NetworkEmojiInvalidator(activity.handler, tvNote)
        movedCaptionInvalidator = NetworkEmojiInvalidator(activity.handler, tvMoved)
        movedNameInvalidator = NetworkEmojiInvalidator(activity.handler, tvMovedName)
    }

    override fun getAccount(): TootAccountRef? = whoRef

    override fun onViewRecycled() {
    }

    //	fun updateRelativeTime() {
    //		val who = whoRef?.get()
    //		if(who != null) {
    //			tvCreated.text = TootStatus.formatTime(tvCreated.context, who.time_created_at, true)
    //		}
    //	}

    override fun bindData(column: Column) {
        super.bindData(column)
        bindFonts()
        bindColors()
        llMoved.visibility = View.GONE
        tvMoved.visibility = View.GONE
        llFields.visibility = View.GONE
        llFields.removeAllViews()
        val whoRef = column.whoAccount
        this.whoRef = whoRef
        when (val who = whoRef?.get()) {
            null -> bindAccountNull()
            else -> bindAccount(who, whoRef)
        }
    }

    // カラム設定から戻った際に呼ばれる
    override fun showColor() {
        llProfile.setBackgroundColor(
            when (val c = column.columnBgColor) {
                0 -> activity.attrColor(R.attr.colorProfileBackgroundMask)
                else -> -0x40000000 or (0x00ffffff and c)
            }
        )
    }

    // bind時に呼ばれる
    private fun bindColors() {
        val contentColor = column.getContentColor()
        this.colorTextContent = contentColor

        tvPersonalNotes.setTextColor(contentColor)
        tvMoved.setTextColor(contentColor)
        tvMovedName.setTextColor(contentColor)
        tvDisplayName.setTextColor(contentColor)
        tvNote.setTextColor(contentColor)
        tvRemoteProfileWarning.setTextColor(contentColor)
        btnStatusCount.setTextColor(contentColor)
        btnFollowing.setTextColor(contentColor)
        btnFollowers.setTextColor(contentColor)
        tvFeaturedTags.setTextColor(contentColor)

        setIconDrawableId(
            activity,
            btnMore,
            R.drawable.ic_more,
            color = contentColor,
            alphaMultiplier = stylerBoostAlpha
        )

        setIconDrawableId(
            activity,
            btnPersonalNotesEdit,
            R.drawable.ic_edit,
            color = contentColor,
            alphaMultiplier = stylerBoostAlpha
        )

        val acctColor = column.getAcctColor()
        tvCreated.setTextColor(acctColor)
        tvMovedAcct.setTextColor(acctColor)
        tvLastStatusAt.setTextColor(acctColor)

        showColor()
    }

    private fun bindFonts() {
        var f: Float

        ActMain.timelineFontSizeSp.takeIf { it.isFinite() }?.let {
            tvMovedName.textSize = it
            tvMoved.textSize = it
            tvPersonalNotes.textSize = it
            tvFeaturedTags.textSize = it
        }

        f = activity.acctFontSizeSp
        if (!f.isNaN()) {
            tvMovedAcct.textSize = f
            tvCreated.textSize = f
            tvLastStatusAt.textSize = f
        }

        ActMain.timelineSpacing?.let {
            tvMovedName.setLineSpacing(0f, it)
            tvMoved.setLineSpacing(0f, it)
        }
    }

    private fun bindAccountNull() {
        relation = null
        nameInvalidator1.register(null)
        noteInvalidator.register(null)
        tvCreated.text = ""
        tvLastStatusAt.vg(false)
        tvFeaturedTags.vg(false)
        ivBackground.setImageDrawable(null)
        ivAvatar.setImageDrawable(null)

        tvAcct.text = "@"

        nameInvalidator1.text = ""

        noteInvalidator.text = ""
        tvMisskeyExtra.text = ""

        btnStatusCount.text = activity.getString(R.string.statuses) + "\n" + "?"
        btnFollowing.text = activity.getString(R.string.following) + "\n" + "?"
        btnFollowers.text = activity.getString(R.string.followers) + "\n" + "?"

        btnFollow.setImageDrawable(null)
        tvRemoteProfileWarning.visibility = View.GONE
    }

    private fun bindAccount(who: TootAccount, whoRef: TootAccountRef) {
        val whoDetail = MisskeyAccountDetailMap.get(accessInfo, who.id)
        val relation = daoUserRelation.load(accessInfo.db_id, who.id)
        this.relation = relation

        tvCreated.text =
            TootStatus.formatTime(tvCreated.context, (whoDetail ?: who).time_created_at, true)

        who.setAccountExtra(
            accessInfo,
            NetworkEmojiInvalidator(activity.handler, tvLastStatusAt),
            fromProfileHeader = true
        )

        val featuredTagsText = formatFeaturedTags()
        tvFeaturedTags.vg(featuredTagsText != null)?.let {
            it.text = featuredTagsText!!
            it.movementMethod = MyLinkMovementMethod
        }

        ivBackground.setImageUrl(0f, accessInfo.supplyBaseUrl(who.header_static))

        ivAvatar.setImageUrl(
            calcIconRound(ivAvatar.layoutParams),
            accessInfo.supplyBaseUrl(who.avatar_static),
            accessInfo.supplyBaseUrl(who.avatar)
        )

        val name = whoDetail?.decodeDisplayName(activity) ?: whoRef.decoded_display_name
        nameInvalidator1.text = name

        tvRemoteProfileWarning.vg(column.accessInfo.isRemoteUser(who))

        tvAcct.text = encodeAcctText(who, whoDetail)

        val note = whoRef.decoded_note
        noteInvalidator.text = note

        tvMisskeyExtra.text = encodeMisskeyExtra(whoDetail)
        tvMisskeyExtra.vg(tvMisskeyExtra.text.isNotEmpty())

        btnStatusCount.text =
            "${activity.getString(R.string.statuses)}\n${
                whoDetail?.statuses_count ?: who.statuses_count
            }"

        val hideFollowCount = PrefB.bpHideFollowCount.value

        var caption = activity.getString(R.string.following)
        btnFollowing.text = when {
            hideFollowCount -> caption
            else -> "${caption}\n${whoDetail?.following_count ?: who.following_count}"
        }

        caption = activity.getString(R.string.followers)
        btnFollowers.text = when {
            hideFollowCount -> caption
            else -> "${caption}\n${whoDetail?.followers_count ?: who.followers_count}"
        }

        setFollowIcon(
            activity,
            btnFollow,
            ivFollowedBy,
            relation,
            who,
            colorTextContent,
            alphaMultiplier = stylerBoostAlpha
        )

        tvPersonalNotes.text = relation.note ?: ""

        showMoved(who, who.movedRef)

        (whoDetail?.fields ?: who.fields)?.notEmpty()?.let { showFields(who, it) }
    }

    private fun showMoved(who: TootAccount, movedRef: TootAccountRef?) {
        if (movedRef == null) return
        this.movedRef = movedRef
        val moved = movedRef.get()

        llMoved.visibility = View.VISIBLE
        tvMoved.visibility = View.VISIBLE

        val caption = activity.getSpannedString(
            R.string.account_moved_to,
            who.decodeDisplayName(activity),
        )

        movedCaptionInvalidator.text = caption

        ivMoved.layoutParams.width = activity.avatarIconSize
        ivMoved.setImageUrl(
            calcIconRound(ivMoved.layoutParams),
            accessInfo.supplyBaseUrl(moved.avatar_static)
        )

        movedNameInvalidator.text = movedRef.decoded_display_name

        setAcct(tvMovedAcct, accessInfo, moved)

        val relation = daoUserRelation.load(accessInfo.db_id, moved.id)
        setFollowIcon(
            activity,
            btnMoved,
            ivMovedBy,
            relation,
            moved,
            colorTextContent,
            alphaMultiplier = stylerBoostAlpha
        )
    }

    override fun onClick(v: View) {
        when (v) {
            ivBackground, tvRemoteProfileWarning ->
                activity.openCustomTab(whoRef?.get()?.url)

            btnFollowing -> {
                column.profileTab = ProfileTab.Following
                reloadBySettingChange()
            }

            btnFollowers -> {
                column.profileTab = ProfileTab.Followers
                reloadBySettingChange()
            }

            btnStatusCount -> {
                column.profileTab = ProfileTab.Status
                reloadBySettingChange()
            }

            btnMore -> whoRef?.let { whoRef ->
                DlgContextMenu(activity, column, whoRef, null, null, null).show()
            }

            btnFollow -> whoRef?.let { whoRef ->
                DlgContextMenu(activity, column, whoRef, null, null, null).show()
            }

            btnMoved -> movedRef?.let { movedRef ->
                DlgContextMenu(activity, column, movedRef, null, null, null).show()
            }

            llMoved -> movedRef?.let { movedRef ->
                if (accessInfo.isPseudo) {
                    DlgContextMenu(activity, column, movedRef, null, null, null).show()
                } else {
                    activity.userProfileLocal(
                        activity.nextPosition(column),
                        accessInfo,
                        movedRef.get()
                    )
                }
            }

            btnPersonalNotesEdit -> whoRef?.let { whoRef ->
                val who = whoRef.get()
                val relation = this.relation
                val lastColumn = column
                activity.launchAndShowError {
                    activity.showTextInputDialog(
                        title = daoAcctColor.getStringWithNickname(
                            activity,
                            R.string.personal_notes_of,
                            who.acct
                        ),
                        inputType = InputTypeEx.textMultiLine,
                        initialText = relation?.note ?: "",
                        allowEmpty = true,
                        onEmptyText = {},
                    ) { text ->
                        val result = activity.runApiTask(column.accessInfo) { client ->
                            when {
                                accessInfo.isPseudo ->
                                    TootApiResult("Personal notes is not supported on pseudo account.")

                                accessInfo.isMisskey ->
                                    TootApiResult("Personal notes is not supported on Misskey account.")

                                else ->
                                    client.request(
                                        "/api/v1/accounts/${who.id}/note",
                                        buildJsonObject {
                                            put("comment", text)
                                        }.toPostRequestBuilder()
                                    )
                            }
                        }
                        result ?: return@showTextInputDialog true
                        when (val error = result.error) {
                            null -> {
                                relation?.note = text
                                if (lastColumn == column) bindData(column)
                                true
                            }

                            else -> {
                                activity.showToast(true, error)
                                false
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onLongClick(v: View): Boolean {
        when (v) {
            btnFollow -> {
                activity.followFromAnotherAccount(
                    activity.nextPosition(column),
                    accessInfo,
                    whoRef?.get()
                )
                return true
            }

            btnMoved -> {
                activity.followFromAnotherAccount(
                    activity.nextPosition(column),
                    accessInfo,
                    movedRef?.get()
                )
                return true
            }
        }

        return false
    }

    private fun setAcct(tv: TextView, accessInfo: SavedAccount, who: TootAccount) {
        val ac = daoAcctColor.load(accessInfo, who)
        tv.text = when {
            daoAcctColor.hasNickname(ac) -> ac.nickname
            PrefB.bpShortAcctLocalUser.value -> "@${who.acct.pretty}"
            else -> "@${ac.nickname}"
        }

        tv.setTextColor(ac.colorFg.notZero() ?: column.getAcctColor())

        tv.setBackgroundColor(ac.colorBg) // may 0
        tv.setPaddingRelative(activity.acctPadLr, 0, activity.acctPadLr, 0)
    }

    private fun formatFeaturedTags() = column.whoFeaturedTags?.notEmpty()?.let { tagList ->
        SpannableStringBuilder().apply {
            append(activity.getString(R.string.featured_hashtags))
            append(":")
            tagList.forEach { tag ->
                append(" ")
                val tagWithSharp = "#" + tag.name
                val start = length
                append(tagWithSharp)
                val end = length
                tag.url?.notEmpty()?.let { url ->
                    val span = MyClickableSpan(
                        LinkInfo(url = url, tag = tag.name, caption = tagWithSharp)
                    )
                    setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
    }

    private fun encodeAcctText(who: TootAccount, whoDetail: TootAccount?) =
        SpannableStringBuilder().apply {
            append("@")
            append(accessInfo.getFullAcct(who).pretty)
            if (whoDetail?.locked ?: who.locked) {
                append(" ")
                val emoji = EmojiMap.shortNameMap["lock"]
                when {
                    emoji == null ->
                        append("locked")

                    PrefB.bpUseTwemoji.value ->
                        appendSpan("locked", emoji.createSpan(activity))

                    else ->
                        append(emoji.unifiedCode)
                }
            }

            if (who.bot) {
                append(" ")
                val emoji = EmojiMap.shortNameMap["robot_face"]
                when {
                    emoji == null ->
                        append("bot")

                    PrefB.bpUseTwemoji.value ->
                        appendSpan("bot", emoji.createSpan(activity))

                    else ->
                        append(emoji.unifiedCode)
                }
            }

            if (who.suspended) {
                append(" ")
                val emoji = EmojiMap.shortNameMap["cross_mark"]
                when {
                    emoji == null ->
                        append("suspended")

                    PrefB.bpUseTwemoji.value ->
                        appendSpan("suspended", emoji.createSpan(activity))

                    else ->
                        append(emoji.unifiedCode)
                }
            }
        }

    private fun encodeMisskeyExtra(whoDetail: TootAccount?) = SpannableStringBuilder().apply {
        var s = whoDetail?.location
        if (s?.isNotEmpty() == true) {
            if (isNotEmpty()) append('\n')
            appendSpan(
                activity.getString(R.string.location),
                EmojiImageSpan(
                    activity,
                    R.drawable.ic_location,
                    useColorShader = true
                )
            )
            append(' ')
            append(s)
        }
        s = whoDetail?.birthday
        if (s?.isNotEmpty() == true) {
            if (isNotEmpty()) append('\n')
            appendSpan(
                activity.getString(R.string.birthday),
                EmojiImageSpan(
                    activity,
                    R.drawable.ic_cake,
                    useColorShader = true
                )
            )
            append(' ')
            append(s)
        }
    }

    private fun showFields(who: TootAccount, fields: List<TootAccount.Field>) {
        llFields.visibility = View.VISIBLE

        // fieldsのnameにはカスタム絵文字が適用されるようになった
        // https://github.com/tootsuite/mastodon/pull/11350
        // fieldsのvalueはMisskeyならMFM、MastodonならHTML
        val fieldDecodeOptions = DecodeOptions(
            context = activity,
            decodeEmoji = true,
            linkHelper = accessInfo,
            short = true,
            emojiMapCustom = who.custom_emojis,
            emojiMapProfile = who.profile_emojis,
            authorDomain = who,
            emojiSizeMode = accessInfo.emojiSizeMode(),
            enlargeCustomEmoji = DecodeOptions.emojiScaleUserName,
            enlargeEmoji = DecodeOptions.emojiScaleUserName,
        )

        val nameTypeface = ActMain.timelineFontBold
        val valueTypeface = ActMain.timelineFont

        for (item in fields) {

            //
            val nameView = MyTextView(activity)
            val nameLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val nameText = fieldDecodeOptions.decodeEmoji(item.name)

            nameLp.topMargin = (density * 6f).toInt()
            nameView.layoutParams = nameLp
            nameView.setTextColor(colorTextContent)
            nameView.typeface = nameTypeface
            nameView.movementMethod = MyLinkMovementMethod
            llFields.addView(nameView)

            val nameInvalidator = NetworkEmojiInvalidator(activity.handler, nameView)
            nameInvalidator.text = nameText

            // 値の方はHTMLエンコードされている
            val valueView = MyTextView(activity)
            val valueLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            val valueText = fieldDecodeOptions.decodeHTML(item.value)
            if (item.verified_at > 0L) {
                valueText.append('\n')

                val start = valueText.length
                valueText.append(activity.getString(R.string.verified_at))
                valueText.append(": ")
                valueText.append(TootStatus.formatTime(activity, item.verified_at, false))
                val end = valueText.length

                val linkFgColor = Color.BLACK or 0x7fbc99

                valueText.setSpan(
                    ForegroundColorSpan(linkFgColor),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            valueLp.startMargin = (density * 32f).toInt()
            valueView.layoutParams = valueLp
            valueView.setTextColor(colorTextContent)
            valueView.typeface = valueTypeface
            valueView.movementMethod = MyLinkMovementMethod

            val valueInvalidator = NetworkEmojiInvalidator(activity.handler, valueView)
            valueInvalidator.text = valueText

            if (item.verified_at > 0L) {
                val linkBgColor = 0x337fbc99

                valueView.setBackgroundColor(linkBgColor)
            }

            llFields.addView(valueView)
        }
    }
}
