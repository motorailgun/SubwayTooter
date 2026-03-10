package jp.juggler.subwaytooter.itemviewholder

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.ActText
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.action.*
import jp.juggler.subwaytooter.actmain.nextPosition
import jp.juggler.subwaytooter.api.entity.*
import jp.juggler.subwaytooter.column.Column
import jp.juggler.subwaytooter.column.ColumnType
import jp.juggler.subwaytooter.dialog.dialogQrCode
import jp.juggler.subwaytooter.dialog.openDlgListMember
import jp.juggler.subwaytooter.pref.PrefB
import jp.juggler.subwaytooter.pref.PrefI
import jp.juggler.subwaytooter.span.MyClickableSpan
import jp.juggler.subwaytooter.table.*
import jp.juggler.subwaytooter.util.*
import jp.juggler.util.data.*
import jp.juggler.util.ui.*
import java.util.*

@SuppressLint("InflateParams")
internal class DlgContextMenu(
    val activity: ActMain,
    private val column: Column,
    private val whoRef: TootAccountRef?,
    private val status: TootStatus?,
    private val notification: TootNotification? = null,
    private val contentTextView: TextView? = null,
) : View.OnClickListener, View.OnLongClickListener {

//    companion object {
//        private val log = LogCategory("DlgContextMenu")
//    }

    private val accessInfo = column.accessInfo
    private val relation: UserRelation

    private val dialog: Dialog

    private lateinit var root: View

    // Links section
    private lateinit var llLinks: LinearLayout

    // Status section
    private lateinit var llStatus: LinearLayout
    private lateinit var btnStatusEdit2: ImageButton
    private lateinit var btnStatusTranslate2: ImageButton
    private lateinit var btnStatusHistory2: ImageButton
    private lateinit var btnStatusDelete2: ImageButton
    private lateinit var btnStatusHistory: Button
    private lateinit var btnStatusWebPage: Button
    private lateinit var btnText: Button
    private lateinit var btnTranslate: Button
    private lateinit var btnQuoteUrlStatus: Button
    private lateinit var btnShareUrlStatus: Button

    // Status cross-account
    private lateinit var btnGroupStatusCrossAccount: Button
    private lateinit var llGroupStatusCrossAccount: LinearLayout
    private lateinit var btnConversationAnotherAccount: Button
    private lateinit var btnReplyAnotherAccount: Button
    private lateinit var btnBoostAnotherAccount: Button
    private lateinit var btnFavouriteAnotherAccount: Button
    private lateinit var btnBookmarkAnotherAccount: Button
    private lateinit var btnReactionAnotherAccount: Button
    private lateinit var btnQuoteAnotherAccount: Button
    private lateinit var btnQuoteTootBT: Button

    // Status around
    private lateinit var btnGroupStatusAround: Button
    private lateinit var llGroupStatusAround: LinearLayout
    private lateinit var btnAroundAccountTL: Button
    private lateinit var btnAroundLTL: Button
    private lateinit var btnAroundFTL: Button

    // Status by me
    private lateinit var btnGroupStatusByMe: Button
    private lateinit var llGroupStatusByMe: LinearLayout
    private lateinit var btnStatusEdit: Button
    private lateinit var btnRedraft: Button
    private lateinit var btnProfilePin: Button
    private lateinit var btnProfileUnpin: Button
    private lateinit var btnDelete: Button

    // Status extra
    private lateinit var btnGroupStatusExtra: Button
    private lateinit var llGroupStatusExtra: LinearLayout
    private lateinit var btnBoostedBy: Button
    private lateinit var btnFavouritedBy: Button
    private lateinit var btnBoostWithVisibility: Button
    private lateinit var btnMuteApp: Button
    private lateinit var btnConversationMute: Button
    private lateinit var btnReportStatus: Button

    // Notification section
    private lateinit var llNotification: LinearLayout
    private lateinit var btnNotificationDelete: Button

    // Account action bar
    private lateinit var llAccountActionBar: LinearLayout
    private lateinit var btnFollow: ImageButton
    private lateinit var ivFollowedBy: ImageView
    private lateinit var btnMute: ImageButton
    private lateinit var btnBlock: ImageButton

    // Account buttons
    private lateinit var btnProfile: Button
    private lateinit var btnAccountWebPage: Button
    private lateinit var btnAccountText: Button
    private lateinit var btnSendMessage: Button
    private lateinit var btnQuoteUrlAccount: Button
    private lateinit var btnShareUrlAccount: Button
    private lateinit var btnQuoteName: Button
    private lateinit var btnFollowRequestOK: Button
    private lateinit var btnFollowRequestNG: Button
    private lateinit var btnListMemberAddRemove: Button
    private lateinit var btnReportUser: Button

    // User cross-account
    private lateinit var btnGroupUserCrossAccount: Button
    private lateinit var llGroupUserCrossAccount: LinearLayout
    private lateinit var btnOpenProfileFromAnotherAccount: Button
    private lateinit var btnFollowFromAnotherAccount: Button
    private lateinit var btnSendMessageFromAnotherAccount: Button

    // User extra
    private lateinit var btnGroupUserExtra: Button
    private lateinit var llGroupUserExtra: LinearLayout
    private lateinit var btnStatusNotification: Button
    private lateinit var btnNickname: Button
    private lateinit var btnAvatarImage: Button
    private lateinit var btnAccountQrCode: Button
    private lateinit var btnNotificationFrom: Button
    private lateinit var btnEndorse: Button
    private lateinit var btnHideBoost: Button
    private lateinit var btnShowBoost: Button
    private lateinit var btnHideFavourite: Button
    private lateinit var btnShowFavourite: Button
    private lateinit var btnDeleteSuggestion: Button
    private lateinit var btnCopyAccountId: Button
    private lateinit var btnOpenAccountInAdminWebUi: Button
    private lateinit var btnOpenInstanceInAdminWebUi: Button

    // Instance section
    private lateinit var llInstance: LinearLayout
    private lateinit var tvInstanceActions: TextView
    private lateinit var btnOpenTimeline: Button
    private lateinit var btnInstanceInformation: Button
    private lateinit var btnProfileDirectory: Button
    private lateinit var btnDomainBlock: Button
    private lateinit var btnDomainTimeline: Button

    // Cancel
    private lateinit var btnCancel: Button

    private fun LinearLayout.addDivider(marginTop: Int = 2, marginBottom: Int = 0): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                context.dp(1)
            ).apply {
                topMargin = context.dp(marginTop)
                bottomMargin = context.dp(marginBottom)
            }
            setBackgroundColor(context.attrColor(R.attr.colorSettingDivider))
        }.also { addView(it) }
    }

    private fun LinearLayout.addSectionLabel(textResId: Int): TextView {
        return TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val pad = context.dp(8)
            setPaddingRelative(pad, 0, pad, 0)
            setText(textResId)
            setTextColor(context.attrColor(R.attr.colorTimeSmall))
            textSize = 12f
        }.also { addView(it) }
    }

    private fun LinearLayout.addTextButton(textResId: Int = 0, text: String? = null): Button {
        return Button(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            background = ContextCompat.getDrawable(context, R.drawable.btn_bg_transparent_round6dp)
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            val h = context.dp(32)
            minHeight = h
            minimumHeight = h
            val padLr = context.dp(8)
            val padTb = context.dp(4)
            setPaddingRelative(padLr, padTb, padLr, padTb)
            isAllCaps = false
            if (textResId != 0) setText(textResId)
            if (text != null) this.text = text
        }.also { addView(it) }
    }

    private fun LinearLayout.addImageButton(iconResId: Int, contentDescResId: Int): ImageButton {
        return ImageButton(context).apply {
            val size = context.dp(40)
            val margin = context.dp(4)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(margin, margin, margin, margin)
            }
            background = ContextCompat.getDrawable(context, R.drawable.btn_bg_transparent_round6dp)
            contentDescription = context.getString(contentDescResId)
            setImageResource(iconResId)
        }.also { addView(it) }
    }

    private fun LinearLayout.addExpandLabel(textResId: Int): Button {
        return Button(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            background = ContextCompat.getDrawable(context, R.drawable.btn_bg_transparent_round6dp)
            compoundDrawablePadding = context.dp(4)
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            val h = context.dp(40)
            minHeight = h
            minimumHeight = h
            val padTb = context.dp(4)
            val padLr = context.dp(8)
            setPaddingRelative(padLr, padTb, padLr, padTb)
            isAllCaps = false
            setTextColor(context.attrColor(R.attr.colorTimeSmall))
            textSize = 12f
            setText(textResId)
        }.also { addView(it) }
    }

    private fun LinearLayout.addExpandGroup(init: LinearLayout.() -> Unit): LinearLayout {
        return LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPaddingRelative(context.dp(24), 0, 0, 0)
            visibility = View.GONE
            init()
        }.also { addView(it) }
    }

    private fun createViews(): View {
        return LinearLayout(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL

            // ScrollView
            addView(ScrollView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
                isScrollbarFadingEnabled = false
                setFadingEdgeLength(context.dp(20))
                isFillViewport = true
                isVerticalFadingEdgeEnabled = true

                // Content column
                addView(LinearLayout(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                    orientation = LinearLayout.VERTICAL
                    val pad = context.dp(12)
                    setPadding(pad, pad, pad, pad)

                    // === Links section ===
                    llLinks = LinearLayout(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        orientation = LinearLayout.VERTICAL
                        addDivider(marginTop = 2, marginBottom = 6)
                    }.also { addView(it) }

                    // === Status section ===
                    llStatus = LinearLayout(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        orientation = LinearLayout.VERTICAL

                        addSectionLabel(R.string.actions_for_status)

                        // Image button bar
                        addView(LinearLayout(context).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            orientation = LinearLayout.HORIZONTAL
                            isBaselineAligned = false

                            btnStatusEdit2 = addImageButton(R.drawable.ic_edit, R.string.edit)
                            btnStatusTranslate2 = addImageButton(R.drawable.ic_translate, R.string.translate)
                            btnStatusHistory2 = addImageButton(R.drawable.ic_history, R.string.edit_history)

                            // spacer
                            addView(View(context).apply {
                                layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                            })

                            btnStatusDelete2 = addImageButton(R.drawable.ic_delete, R.string.delete)
                        })

                        btnStatusHistory = addTextButton(R.string.edit_history)
                        btnStatusWebPage = addTextButton(R.string.open_web_page)
                        btnText = addTextButton(R.string.select_and_copy)
                        btnTranslate = addTextButton(R.string.translate)
                        btnQuoteUrlStatus = addTextButton(R.string.quote_url)
                        btnShareUrlStatus = addTextButton(R.string.share_url_more)

                        btnGroupStatusCrossAccount = addExpandLabel(R.string.cross_account_actions)
                        llGroupStatusCrossAccount = addExpandGroup {
                            btnConversationAnotherAccount = addTextButton(R.string.conversation_view)
                            btnReplyAnotherAccount = addTextButton(R.string.reply)
                            btnBoostAnotherAccount = addTextButton(R.string.boost)
                            btnFavouriteAnotherAccount = addTextButton(R.string.favourite)
                            btnBookmarkAnotherAccount = addTextButton(R.string.bookmark)
                            btnReactionAnotherAccount = addTextButton(R.string.reaction)
                            btnQuoteAnotherAccount = addTextButton(R.string.quote)
                            btnQuoteTootBT = addTextButton(R.string.quote_toot_bt)
                        }

                        btnGroupStatusAround = addExpandLabel(R.string.around_this_toot)
                        llGroupStatusAround = addExpandGroup {
                            btnAroundAccountTL = addTextButton(R.string.account_timeline)
                            btnAroundLTL = addTextButton(R.string.local_timeline)
                            btnAroundFTL = addTextButton(R.string.federate_timeline)
                        }

                        btnGroupStatusByMe = addExpandLabel(R.string.your_toot)
                        llGroupStatusByMe = addExpandGroup {
                            btnStatusEdit = addTextButton(R.string.edit)
                            btnRedraft = addTextButton(R.string.redraft_and_delete)
                            btnProfilePin = addTextButton(R.string.profile_pin)
                            btnProfileUnpin = addTextButton(R.string.profile_unpin)
                            btnDelete = addTextButton(R.string.delete)
                        }

                        btnGroupStatusExtra = addExpandLabel(R.string.extra_actions)
                        llGroupStatusExtra = addExpandGroup {
                            btnBoostedBy = addTextButton(R.string.boosted_by)
                            btnFavouritedBy = addTextButton(R.string.favourited_by)
                            btnBoostWithVisibility = addTextButton(R.string.boost_with_visibility)
                            btnMuteApp = addTextButton() // text set dynamically
                            btnConversationMute = addTextButton(R.string.mute_this_conversation)
                            btnReportStatus = addTextButton(R.string.report)
                        }

                        addDivider(marginTop = 2)
                    }.also { addView(it) }

                    // === Notification section ===
                    llNotification = LinearLayout(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = context.dp(6)
                        }
                        orientation = LinearLayout.VERTICAL

                        addSectionLabel(R.string.actions_for_notification)
                        btnNotificationDelete = addTextButton(R.string.delete_this_notification)
                        addDivider(marginTop = 2)
                    }.also { addView(it) }

                    // === Account section ===
                    addView(LinearLayout(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = context.dp(6)
                        }
                        orientation = LinearLayout.VERTICAL

                        addSectionLabel(R.string.actions_for_user)

                        // Account action bar
                        llAccountActionBar = LinearLayout(context).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            orientation = LinearLayout.HORIZONTAL

                            // Follow button in FrameLayout
                            addView(FrameLayout(context).apply {
                                val size = context.dp(40)
                                val margin = context.dp(4)
                                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                                    setMargins(margin, margin, margin, margin)
                                }

                                btnFollow = ImageButton(context).apply {
                                    layoutParams = FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        FrameLayout.LayoutParams.MATCH_PARENT
                                    )
                                    background = ContextCompat.getDrawable(context, R.drawable.btn_bg_transparent_round6dp)
                                    contentDescription = context.getString(R.string.follow)
                                    scaleType = ImageView.ScaleType.CENTER
                                }.also { addView(it) }

                                ivFollowedBy = ImageView(context).apply {
                                    layoutParams = FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        FrameLayout.LayoutParams.MATCH_PARENT
                                    )
                                    scaleType = ImageView.ScaleType.CENTER
                                    setImageResource(R.drawable.ic_follow_dot)
                                    imageTintList = ColorStateList.valueOf(context.attrColor(R.attr.colorButtonAccentFollow))
                                }.also { addView(it) }
                            })

                            // spacer
                            addView(View(context).apply {
                                layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                            })

                            btnMute = addImageButton(R.drawable.ic_volume_off, R.string.mute)
                            btnBlock = addImageButton(R.drawable.ic_block, R.string.block)
                        }.also { addView(it) }

                        btnProfile = addTextButton(R.string.open_profile)
                        btnAccountWebPage = addTextButton(R.string.open_web_page)
                        btnAccountText = addTextButton(R.string.select_and_copy)
                        btnSendMessage = addTextButton(R.string.send_message)
                        btnQuoteUrlAccount = addTextButton(R.string.quote_url)
                        btnShareUrlAccount = addTextButton(R.string.share_url_more)
                        btnQuoteName = addTextButton(R.string.quote_name)
                        btnFollowRequestOK = addTextButton(R.string.follow_request_ok)
                        btnFollowRequestNG = addTextButton(R.string.follow_request_ng)
                        btnListMemberAddRemove = addTextButton(R.string.list_member_add_remove)
                        btnReportUser = addTextButton(R.string.report)

                        btnGroupUserCrossAccount = addExpandLabel(R.string.cross_account_actions)
                        llGroupUserCrossAccount = addExpandGroup {
                            btnOpenProfileFromAnotherAccount = addTextButton(R.string.open_profile)
                            btnFollowFromAnotherAccount = addTextButton(R.string.follow)
                            btnSendMessageFromAnotherAccount = addTextButton(R.string.send_message)
                        }

                        btnGroupUserExtra = addExpandLabel(R.string.extra_actions)
                        llGroupUserExtra = addExpandGroup {
                            btnStatusNotification = addTextButton() // text set dynamically
                            btnNickname = addTextButton(R.string.nickname_and_color_and_notification_sound)
                            btnAvatarImage = addTextButton(R.string.show_avatar_image)
                            btnAccountQrCode = addTextButton(R.string.qr_code)
                            btnNotificationFrom = addTextButton(R.string.notifications_from_acct)
                            btnEndorse = addTextButton() // text set dynamically
                            btnHideBoost = addTextButton(R.string.hide_boost_in_home)
                            btnShowBoost = addTextButton(R.string.show_boost_in_home)
                            btnHideFavourite = addTextButton(R.string.hide_favourite_notification_from_user)
                            btnShowFavourite = addTextButton(R.string.show_favourite_notification_from_user)
                            btnDeleteSuggestion = addTextButton(R.string.delete_suggestion)
                            btnCopyAccountId = addTextButton() // text set dynamically
                            btnOpenAccountInAdminWebUi = addTextButton(R.string.open_in_admin_ui)
                            btnOpenInstanceInAdminWebUi = addTextButton(R.string.open_in_admin_ui)
                        }

                        addDivider(marginTop = 2)
                    })

                    // === Instance section ===
                    llInstance = LinearLayout(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = context.dp(6)
                            bottomMargin = context.dp(6)
                        }
                        orientation = LinearLayout.VERTICAL

                        tvInstanceActions = TextView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            val pad = context.dp(8)
                            setPaddingRelative(pad, 0, pad, 0)
                            setTextColor(context.attrColor(R.attr.colorTimeSmall))
                            textSize = 12f
                        }.also { addView(it) }

                        btnOpenTimeline = addTextButton(R.string.local_timeline)
                        btnInstanceInformation = addTextButton(R.string.instance_information)
                        btnProfileDirectory = addTextButton(R.string.profile_directory)
                        btnDomainBlock = addTextButton(R.string.block_domain)
                        btnDomainTimeline = addTextButton(R.string.fedibird_domain_timeline)
                    }.also { addView(it) }
                })
            })

            // Bottom divider
            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    context.dp(1)
                )
                setBackgroundColor(context.attrColor(R.attr.colorSettingDivider))
            })

            // Cancel button
            btnCancel = Button(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                background = ContextCompat.getDrawable(context, R.drawable.btn_bg_transparent_round6dp)
                setText(R.string.cancel)
                isAllCaps = false
            }.also { addView(it) }
        }.also { root = it }
    }

    init {
        val columnType = column.type

        val who = whoRef?.get()
        val status = this.status

        this.relation = when {
            who == null -> UserRelation()
            accessInfo.isPseudo -> daoUserRelation.loadPseudo(accessInfo.getFullAcct(who))
            else -> daoUserRelation.load(accessInfo.db_id, who.id)
        }

        this.dialog = Dialog(activity)
        dialog.setContentView(createViews())
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        root.scan { v ->
            when (v) {
                is Button -> v.setOnClickListener(this)
                is ImageButton -> v.setOnClickListener(this)
            }
        }

        arrayOf(
            btnBlock,
            btnFollow,
            btnMute,
            btnProfile,
            btnQuoteAnotherAccount,
            btnQuoteTootBT,
            btnSendMessage,
        ).forEach { it.setOnLongClickListener(this) }

        val accountList = daoSavedAccount.loadAccountList()

        val accountListNonPseudo = ArrayList<SavedAccount>()
        for (a in accountList) {
            if (!a.isPseudo) {
                accountListNonPseudo.add(a)
                //				if( a.host.equalsIgnoreCase( access_info.host ) ){
                //					account_list_non_pseudo_same_instance.add( a );
                //				}
            }
        }

        if (status == null) {
            llStatus.visibility = View.GONE
            llLinks.visibility = View.GONE
        } else {
            val statusByMe = accessInfo.isMe(status.account)

            if (PrefB.bpLinksInContextMenu.value && contentTextView != null) {

                var insPos = 0

                fun addLinkButton(span: MyClickableSpan, caption: String) {
                    val b = AppCompatButton(activity)
                    val lp = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    b.layoutParams = lp
                    b.background =
                        ContextCompat.getDrawable(activity, R.drawable.btn_bg_transparent_round6dp)
                    b.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    b.minHeight = (activity.density * 32f + 0.5f).toInt()
                    b.minimumHeight = (activity.density * 32f + 0.5f).toInt()
                    val padLr = (activity.density * 8f + 0.5f).toInt()
                    val padTb = (activity.density * 4f + 0.5f).toInt()
                    b.setPaddingRelative(padLr, padTb, padLr, padTb)
                    b.text = caption
                    b.isAllCaps = false
                    b.setOnClickListener {
                        dialog.dismissSafe()
                        span.onClick(contentTextView)
                    }
                    llLinks.addView(b, insPos++)
                }

                val dc = status.decoded_content
                for (span in dc.getSpans(0, dc.length, MyClickableSpan::class.java)) {
                    val caption = span.linkInfo.text
                    when (caption.firstOrNull()) {
                        '@', '#' -> addLinkButton(span, caption)
                        else -> addLinkButton(span, span.linkInfo.url)
                    }
                }
            }

            val hasEditHistory =
                status.time_edited_at > 0L && columnType != ColumnType.STATUS_HISTORY

            btnStatusHistory2.vg(hasEditHistory)
            btnStatusHistory.vg(hasEditHistory)
                ?.text = activity.getString(R.string.edit_history) + "\n" +
                    TootStatus.formatTime(activity, status.time_edited_at, bAllowRelative = false)

            llLinks.vg(llLinks.childCount > 1)

            val hasTranslateApp = CustomShare.hasTranslateApp(
                CustomShareTarget.Translate,
                activity,
            )

            btnStatusTranslate2.vg(hasTranslateApp)
            btnTranslate.vg(hasTranslateApp)

            val canEdit = statusByMe && (TootInstance.getCached(column.accessInfo)
                ?.let {
                    when {
                        it.isMastodon && it.versionGE(TootInstance.VERSION_3_5_0_rc1) -> true
                        it.pleromaFeatures?.contains("editing") == true -> true
                        else -> false
                    }
                } ?: false)

            btnStatusEdit2.vg(canEdit)
            btnStatusEdit.vg(canEdit)

            btnStatusDelete2.vg(statusByMe)
            btnGroupStatusByMe.vg(statusByMe)

            btnQuoteTootBT.vg(status.reblogParent != null)

            btnBoostWithVisibility.vg(!accessInfo.isPseudo && !accessInfo.isMisskey)

            btnReportStatus.vg(!(statusByMe || accessInfo.isPseudo))

            val applicationName = status.application?.name
            if (statusByMe || applicationName == null || applicationName.isEmpty()) {
                btnMuteApp.visibility = View.GONE
            } else {
                btnMuteApp.text = activity.getString(R.string.mute_app_of, applicationName)
            }

            val canPin = status.canPin(accessInfo)
            btnProfileUnpin.vg(canPin && status.pinned)
            btnProfilePin.vg(canPin && !status.pinned)
        }

        val bShowConversationMute = when {
            status == null -> false
            accessInfo.isMe(status.account) -> true
            notification != null && NotificationType.Mention == notification.type -> true
            else -> false
        }

        val muted = status?.muted == true
        btnConversationMute.vg(bShowConversationMute)
            ?.setText(
                when {
                    muted -> R.string.unmute_this_conversation
                    else -> R.string.mute_this_conversation
                }
            )

        llNotification.vg(notification != null)

        val colorButtonAccent =
            PrefI.ipButtonFollowingColor.value.notZero()
                ?: activity.attrColor(R.attr.colorButtonAccentFollow)

        val colorButtonFollowRequest =
            PrefI.ipButtonFollowRequestColor.value.notZero()
                ?: activity.attrColor(R.attr.colorButtonAccentFollowRequest)

        val colorButtonNormal =
            activity.attrColor(R.attr.colorTextContent)

        fun showRelation(relation: UserRelation) {

            // 被フォロー状態
            // Styler.setFollowIconとは異なり細かい状態を表示しない
            ivFollowedBy.vg(relation.followed_by)

            // フォロー状態
            // Styler.setFollowIconとは異なりミュートやブロックを表示しない
            btnFollow.setImageResource(
                when {
                    relation.getRequested(who) -> R.drawable.ic_follow_wait
                    relation.getFollowing(who) -> R.drawable.ic_follow_cross
                    else -> R.drawable.ic_follow_plus
                }
            )

            arrayOf(
                btnStatusEdit2,
                btnStatusHistory2,
                btnStatusTranslate2,
                btnStatusDelete2,
            ).forEach {
                it.imageTintList = ColorStateList.valueOf(colorButtonNormal)
            }

            btnFollow.imageTintList = ColorStateList.valueOf(
                when {
                    relation.getRequested(who) -> colorButtonFollowRequest
                    relation.getFollowing(who) -> colorButtonAccent
                    else -> colorButtonNormal
                }
            )

            // ミュート状態
            btnMute.imageTintList = ColorStateList.valueOf(
                when (relation.muting) {
                    true -> colorButtonAccent
                    else -> colorButtonNormal
                }
            )

            // ブロック状態
            btnBlock.imageTintList = ColorStateList.valueOf(
                when (relation.blocking) {
                    true -> colorButtonAccent
                    else -> colorButtonNormal
                }
            )
        }

        if (accessInfo.isPseudo) {
            // 疑似アカミュートができたのでアカウントアクションを表示する
            showRelation(relation)
            llAccountActionBar.visibility = View.VISIBLE
            ivFollowedBy.vg(false)
            btnFollow.setImageResource(R.drawable.ic_follow_plus)
            btnFollow.imageTintList =
                ColorStateList.valueOf(colorButtonNormal)

            btnNotificationFrom.visibility = View.GONE
        } else {
            showRelation(relation)
        }

        val whoApiHost = getUserApiHost()
        val whoApDomain = getUserApDomain()

        llInstance
            .vg(whoApiHost.isValid)
            ?.let {
                tvInstanceActions.text =
                    activity.getString(R.string.instance_actions_for, whoApDomain.pretty)

                // 疑似アカウントではドメインブロックできない
                // 自ドメインはブロックできない
                btnDomainBlock.vg(
                    !(accessInfo.isPseudo || accessInfo.matchHost(whoApiHost))
                )

                btnDomainTimeline.vg(
                    PrefB.bpEnableDomainTimeline.value &&
                            !accessInfo.isPseudo &&
                            !accessInfo.isMisskey
                )
            }

        if (who == null) {
            btnCopyAccountId.visibility = View.GONE
            btnOpenAccountInAdminWebUi.visibility = View.GONE
            btnOpenInstanceInAdminWebUi.visibility = View.GONE

            btnReportUser.visibility = View.GONE
        } else {

            btnCopyAccountId.visibility = View.VISIBLE
            btnCopyAccountId.text =
                activity.getString(R.string.copy_account_id, who.id.toString())

            btnOpenAccountInAdminWebUi.vg(!accessInfo.isPseudo)
            btnOpenInstanceInAdminWebUi.vg(!accessInfo.isPseudo)

            btnReportUser.vg(!(accessInfo.isPseudo || accessInfo.isMe(who)))

            btnStatusNotification.vg(!accessInfo.isPseudo && accessInfo.isMastodon && relation.following)
                ?.text = when (relation.notifying) {
                true -> activity.getString(R.string.stop_notify_posts_from_this_user)
                else -> activity.getString(R.string.notify_posts_from_this_user)
            }
        }

        if (accessInfo.isPseudo) {
            btnProfile.visibility = View.GONE
            btnSendMessage.visibility = View.GONE
            btnEndorse.visibility = View.GONE
        }

        btnEndorse.text = when (relation.endorsed) {
            false -> activity.getString(R.string.endorse_set)
            else -> activity.getString(R.string.endorse_unset)
        }

        if (columnType != ColumnType.FOLLOW_REQUESTS) {
            btnFollowRequestOK.visibility = View.GONE
            btnFollowRequestNG.visibility = View.GONE
        }

        if (columnType != ColumnType.FOLLOW_SUGGESTION) {
            btnDeleteSuggestion.visibility = View.GONE
        }

        if (accountListNonPseudo.isEmpty()) {
            btnFollowFromAnotherAccount.visibility = View.GONE
            btnSendMessageFromAnotherAccount.visibility = View.GONE
        }

        if (accessInfo.isPseudo ||
            who == null ||
            !relation.getFollowing(who) ||
            relation.following_reblogs == UserRelation.REBLOG_UNKNOWN
        ) {
            btnHideBoost.visibility = View.GONE
            btnShowBoost.visibility = View.GONE
        } else if (relation.following_reblogs == UserRelation.REBLOG_SHOW) {
            btnHideBoost.visibility = View.VISIBLE
            btnShowBoost.visibility = View.GONE
        } else {
            btnHideBoost.visibility = View.GONE
            btnShowBoost.visibility = View.VISIBLE
        }

        when {
            who == null -> {
                btnHideFavourite.visibility = View.GONE
                btnShowFavourite.visibility = View.GONE
            }

            daoFavMute.contains(accessInfo.getFullAcct(who)) -> {
                btnHideFavourite.visibility = View.GONE
                btnShowFavourite.visibility = View.VISIBLE
            }

            else -> {
                btnHideFavourite.visibility = View.VISIBLE
                btnShowFavourite.visibility = View.GONE
            }
        }

        btnListMemberAddRemove.visibility = View.VISIBLE

        updateGroup(btnGroupStatusCrossAccount, llGroupStatusCrossAccount)
        updateGroup(btnGroupUserCrossAccount, llGroupUserCrossAccount)
        updateGroup(btnGroupStatusAround, llGroupStatusAround)
        updateGroup(btnGroupStatusByMe, llGroupStatusByMe)
        updateGroup(btnGroupStatusExtra, llGroupStatusExtra)
        updateGroup(btnGroupUserExtra, llGroupUserExtra)
    }

    fun show() {
        val window = dialog.window
        if (window != null) {
            val lp = window.attributes
            lp.width = (0.5f + 280f * activity.density).toInt()
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            window.attributes = lp
        }
        dialog.show()
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

    private fun updateGroup(btn: Button, group: View, toggle: Boolean = false) {

        if (btn.visibility != View.VISIBLE) {
            group.vg(false)
            return
        }

        when {
            PrefB.bpAlwaysExpandContextMenuItems.value -> {
                group.vg(true)
                btn.background = null
            }

            toggle -> group.vg(group.visibility != View.VISIBLE)
            else -> btn.setOnClickListener(this)
        }

        val iconId = when (group.visibility) {
            View.VISIBLE -> R.drawable.ic_arrow_drop_up
            else -> R.drawable.ic_arrow_drop_down
        }

        val iconColor = activity.attrColor(R.attr.colorTimeSmall)
        val drawable = createColoredDrawable(activity, iconId, iconColor, 1f)
        btn.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
    }

    private fun onClickUpdateGroup(v: View): Boolean {
        when (v) {
            btnGroupStatusCrossAccount -> updateGroup(
                btnGroupStatusCrossAccount,
                llGroupStatusCrossAccount,
                toggle = true
            )

            btnGroupUserCrossAccount -> updateGroup(
                btnGroupUserCrossAccount,
                llGroupUserCrossAccount,
                toggle = true
            )

            btnGroupStatusAround -> updateGroup(
                btnGroupStatusAround,
                llGroupStatusAround,
                toggle = true
            )

            btnGroupStatusByMe -> updateGroup(
                btnGroupStatusByMe,
                llGroupStatusByMe,
                toggle = true
            )

            btnGroupStatusExtra -> updateGroup(
                btnGroupStatusExtra,
                llGroupStatusExtra,
                toggle = true
            )

            btnGroupUserExtra -> updateGroup(
                btnGroupUserExtra,
                llGroupUserExtra,
                toggle = true
            )

            else -> return false
        }
        return true
    }

    private fun ActMain.onClickUserAndStatus(
        v: View,
        pos: Int,
        who: TootAccount,
        status: TootStatus,
    ): Boolean {
        when (v) {
            btnAroundAccountTL -> clickAroundAccountTL(accessInfo, pos, who, status)
            btnAroundLTL -> clickAroundLTL(accessInfo, pos, who, status)
            btnAroundFTL -> clickAroundFTL(accessInfo, pos, who, status)
            btnReportStatus -> userReportForm(accessInfo, who, status)
            else -> return false
        }
        return true
    }

    @Suppress("ComplexMethod")
    private fun ActMain.onClickUser(
        v: View,
        pos: Int,
        who: TootAccount,
        whoRef: TootAccountRef,
    ): Boolean {
        when (v) {
            btnReportUser -> userReportForm(accessInfo, who)
            btnFollow -> clickFollow(pos, accessInfo, whoRef, relation)
            btnMute -> clickMute(accessInfo, who, relation)
            btnBlock -> clickBlock(accessInfo, who, relation)
            btnAccountText -> launchActText(ActText.createIntent(activity, accessInfo, who))
            btnProfile -> userProfileLocal(pos, accessInfo, who)
            btnSendMessage -> mention(accessInfo, who)
            btnAccountWebPage -> openCustomTab(who.url)
            btnFollowRequestOK -> followRequestAuthorize(accessInfo, whoRef, true)
            btnDeleteSuggestion -> userSuggestionDelete(accessInfo, who)
            btnFollowRequestNG -> followRequestAuthorize(accessInfo, whoRef, false)
            btnFollowFromAnotherAccount -> followFromAnotherAccount(pos, accessInfo, who)
            btnSendMessageFromAnotherAccount -> mentionFromAnotherAccount(accessInfo, who)
            btnOpenProfileFromAnotherAccount -> userProfileFromAnotherAccount(
                pos,
                accessInfo,
                who
            )

            btnNickname -> clickNicknameCustomize(accessInfo, who)
            btnAccountQrCode -> activity.dialogQrCode(
                message = whoRef.decoded_display_name,
                url = who.getUserUrl()
            )

            btnDomainBlock -> clickDomainBlock(accessInfo, who)
            btnOpenTimeline -> who.apiHost.valid()?.let { timelineLocal(pos, it) }
            btnDomainTimeline -> who.apiHost.valid()
                ?.let { timelineDomain(pos, accessInfo, it) }

            btnAvatarImage -> openAvatarImage(who)
            btnQuoteName -> quoteName(who)
            btnHideBoost -> userSetShowBoosts(accessInfo, who, false)
            btnShowBoost -> userSetShowBoosts(accessInfo, who, true)
            btnHideFavourite -> clickHideFavourite(accessInfo, who)
            btnShowFavourite -> clickShowFavourite(accessInfo, who)
            btnListMemberAddRemove -> {
                openDlgListMember(who, accessInfo.getFullAcct(who), accessInfo)
            }
            btnInstanceInformation -> serverInformation(pos, getUserApiHost())
            btnProfileDirectory -> serverProfileDirectoryFromInstanceInformation(
                column,
                getUserApiHost()
            )

            btnEndorse -> userEndorsement(accessInfo, who, !relation.endorsed)
            btnCopyAccountId -> who.id.toString().copyToClipboard(activity)
            btnOpenAccountInAdminWebUi -> openBrowser("https://${accessInfo.apiHost.ascii}/admin/accounts/${who.id}")
            btnOpenInstanceInAdminWebUi -> openBrowser("https://${accessInfo.apiHost.ascii}/admin/instances/${who.apDomain.ascii}")
            btnNotificationFrom -> clickNotificationFrom(pos, accessInfo, who)
            btnStatusNotification -> clickStatusNotification(accessInfo, who, relation)
            btnQuoteUrlAccount -> openPost(who.url?.notEmpty())
            btnShareUrlAccount -> shareText(who.url?.notEmpty())
            else -> return false
        }
        return true
    }

    private fun ActMain.onClickStatus(v: View, pos: Int, status: TootStatus): Boolean {
        when (v) {
            btnBoostWithVisibility -> clickBoostWithVisibility(accessInfo, status)
            btnStatusWebPage -> openCustomTab(status.url)
            btnText -> launchActText(ActText.createIntent(this, accessInfo, status))
            btnFavouriteAnotherAccount -> favouriteFromAnotherAccount(accessInfo, status)
            btnBookmarkAnotherAccount -> bookmarkFromAnotherAccount(accessInfo, status)
            btnBoostAnotherAccount -> boostFromAnotherAccount(accessInfo, status)
            btnReactionAnotherAccount -> reactionFromAnotherAccount(accessInfo, status)
            btnReplyAnotherAccount -> replyFromAnotherAccount(accessInfo, status)
            btnQuoteAnotherAccount -> quoteFromAnotherAccount(accessInfo, status)
            btnQuoteTootBT -> quoteFromAnotherAccount(accessInfo, status.reblogParent)
            btnConversationAnotherAccount -> conversationOtherInstance(pos, status)
            btnDelete, btnStatusDelete2 -> clickStatusDelete(accessInfo, status)
            btnRedraft -> statusRedraft(accessInfo, status)
            btnStatusEdit, btnStatusEdit2 -> statusEdit(accessInfo, status)
            btnMuteApp -> appMute(status.application)
            btnBoostedBy -> clickBoostBy(pos, accessInfo, status, ColumnType.BOOSTED_BY)
            btnFavouritedBy -> clickBoostBy(pos, accessInfo, status, ColumnType.FAVOURITED_BY)
            btnTranslate, btnStatusTranslate2 -> CustomShare.invokeStatusText(
                CustomShareTarget.Translate,
                activity,
                accessInfo,
                status
            )

            btnQuoteUrlStatus -> openPost(status.url?.notEmpty())
            btnShareUrlStatus -> shareText(status.url?.notEmpty())
            btnConversationMute -> conversationMute(accessInfo, status)
            btnProfilePin -> statusPin(accessInfo, status, true)
            btnProfileUnpin -> statusPin(accessInfo, status, false)
            btnStatusHistory, btnStatusHistory2 -> openStatusHistory(
                pos,
                accessInfo,
                status
            )

            else -> return false
        }
        return true
    }

    private fun ActMain.onClickOther(v: View) {
        when (v) {
            btnNotificationDelete -> notificationDeleteOne(accessInfo, notification)
            btnCancel -> dialog.cancel()
        }
    }

    override fun onClick(v: View) {
        if (onClickUpdateGroup(v)) return // ダイアログを閉じない操作

        dialog.dismissSafe()

        val pos = activity.nextPosition(column)
        val status = this.status
        val whoRef = this.whoRef
        val who = whoRef?.get()

        if (status != null && activity.onClickStatus(v, pos, status)) return

        if (whoRef != null && who != null) {
            when {
                activity.onClickUser(v, pos, who, whoRef) -> return
                status != null && activity.onClickUserAndStatus(v, pos, who, status) -> return
            }
        }

        activity.onClickOther(v)
    }

    override fun onLongClick(v: View): Boolean {
        val whoRef = this.whoRef
        val who = whoRef?.get()

        with(activity) {
            val pos = nextPosition(column)

            when (v) {
                // events don't close dialog
                btnMute -> userMuteFromAnotherAccount(who, accessInfo)
                btnBlock -> userBlockFromAnotherAccount(who, accessInfo)
                btnQuoteAnotherAccount -> quoteFromAnotherAccount(accessInfo, status)
                btnQuoteTootBT -> quoteFromAnotherAccount(accessInfo, status?.reblogParent)

                // events close dialog before action
                btnFollow -> {
                    dialog.dismissSafe()
                    followFromAnotherAccount(pos, accessInfo, who)
                }

                btnProfile -> {
                    dialog.dismissSafe()
                    userProfileFromAnotherAccount(pos, accessInfo, who)
                }

                btnSendMessage -> {
                    dialog.dismissSafe()
                    mentionFromAnotherAccount(accessInfo, who)
                }

                else -> return false
            }
        }
        return true
    }
}
