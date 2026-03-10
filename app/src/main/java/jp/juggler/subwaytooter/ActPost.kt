package jp.juggler.subwaytooter

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import jp.juggler.subwaytooter.action.saveWindowSize
import jp.juggler.subwaytooter.actpost.ActPostRootLinearLayout
import jp.juggler.subwaytooter.actpost.ActPostStates
import jp.juggler.subwaytooter.actpost.CompletionHelper
import jp.juggler.subwaytooter.actpost.FeaturedTagCache
import jp.juggler.subwaytooter.actpost.addAttachment
import jp.juggler.subwaytooter.actpost.applyMushroomText
import jp.juggler.subwaytooter.actpost.onPickCustomThumbnailImpl
import jp.juggler.subwaytooter.actpost.onPostAttachmentCompleteImpl
import jp.juggler.subwaytooter.actpost.openAttachment
import jp.juggler.subwaytooter.actpost.openMushroom
import jp.juggler.subwaytooter.actpost.openVisibilityPicker
import jp.juggler.subwaytooter.actpost.performAccountChooser
import jp.juggler.subwaytooter.actpost.performAttachmentClick
import jp.juggler.subwaytooter.actpost.performMore
import jp.juggler.subwaytooter.actpost.performPost
import jp.juggler.subwaytooter.actpost.performSchedule
import jp.juggler.subwaytooter.actpost.rearrangeAttachments
import jp.juggler.subwaytooter.actpost.removeReply
import jp.juggler.subwaytooter.actpost.resetSchedule
import jp.juggler.subwaytooter.actpost.restoreState
import jp.juggler.subwaytooter.actpost.saveDraft
import jp.juggler.subwaytooter.actpost.saveState
import jp.juggler.subwaytooter.actpost.showContentWarningEnabled
import jp.juggler.subwaytooter.actpost.showMediaAttachment
import jp.juggler.subwaytooter.actpost.showMediaAttachmentProgress
import jp.juggler.subwaytooter.actpost.showPoll
import jp.juggler.subwaytooter.actpost.showQuotedRenote
import jp.juggler.subwaytooter.actpost.showReplyTo
import jp.juggler.subwaytooter.actpost.showVisibility
import jp.juggler.subwaytooter.actpost.updateText
import jp.juggler.subwaytooter.actpost.updateTextCount
import jp.juggler.subwaytooter.api.entity.TootScheduled
import jp.juggler.subwaytooter.api.entity.TootStatus
import jp.juggler.subwaytooter.pref.PrefB
import jp.juggler.subwaytooter.span.MyClickableSpan
import jp.juggler.subwaytooter.span.MyClickableSpanHandler
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.subwaytooter.util.AttachmentPicker
import jp.juggler.subwaytooter.util.AttachmentUploader
import jp.juggler.subwaytooter.util.PostAttachment
import jp.juggler.subwaytooter.util.loadLanguageList
import jp.juggler.subwaytooter.util.openBrowser
import jp.juggler.subwaytooter.view.MyEditText
import jp.juggler.subwaytooter.view.MyNetworkImageView
import jp.juggler.util.backPressed
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.coroutine.launchIO
import jp.juggler.util.coroutine.launchMain
import jp.juggler.util.data.UriAndType
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import jp.juggler.util.string
import jp.juggler.util.ui.ActivityResultHandler
import jp.juggler.util.ui.attrColor
import jp.juggler.util.ui.dp
import jp.juggler.util.ui.isNotOk
import jp.juggler.util.ui.setContentViewAndInsets
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import java.lang.ref.WeakReference
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap

class ActPostViews(
    val root: ActPostRootLinearLayout,
    val scrollView: ScrollView,
    val llReply: LinearLayout,
    val ivReply: MyNetworkImageView,
    val tvReplyTo: TextView,
    val btnRemoveReply: ImageButton,
    val cbQuote: CheckBox,
    val ivAccount: MyNetworkImageView,
    val btnAccount: Button,
    val llAttachment: FlexboxLayout,
    val ivMedia1: MyNetworkImageView,
    val ivMedia2: MyNetworkImageView,
    val ivMedia3: MyNetworkImageView,
    val ivMedia4: MyNetworkImageView,
    val btnAttachmentsRearrange: ImageButton,
    val tvAttachmentProgress: TextView,
    val cbNSFW: CheckBox,
    val cbContentWarning: CheckBox,
    val etContentWarning: MyEditText,
    val btnFeaturedTag: ImageButton,
    val btnEmojiPicker: ImageButton,
    val etContent: MyEditText,
    val spLanguage: Spinner,
    val tvSchedule: TextView,
    val ibSchedule: ImageButton,
    val ibScheduleReset: ImageButton,
    val spPollType: Spinner,
    val llEnquete: LinearLayout,
    val etChoice1: MyEditText,
    val etChoice2: MyEditText,
    val etChoice3: MyEditText,
    val etChoice4: MyEditText,
    val cbMultipleChoice: CheckBox,
    val cbHideTotals: CheckBox,
    val llExpire: LinearLayout,
    val etExpireDays: EditText,
    val etExpireHours: EditText,
    val etExpireMinutes: EditText,
    val llFooterBar: LinearLayout,
    val btnAttachment: ImageButton,
    val btnVisibility: ImageButton,
    val btnPlugin: ImageButton,
    val btnMore: ImageButton,
    val tvCharCount: TextView,
    val btnPost: ImageButton,
)

@Suppress("LongMethod")
fun createActPostViews(context: Context): ActPostViews {
    val matchParent = ViewGroup.LayoutParams.MATCH_PARENT
    val wrapContent = ViewGroup.LayoutParams.WRAP_CONTENT

    val tintColor = ColorStateList.valueOf(context.attrColor(R.attr.colorTextContent))
    val btnBg = ContextCompat.getDrawable(context, R.drawable.btn_bg_transparent_round6dp)
    fun btnBg() = ContextCompat.getDrawable(context, R.drawable.btn_bg_transparent_round6dp)

    fun makeMediaView() = MyNetworkImageView(context).apply {
        layoutParams = FlexboxLayout.LayoutParams(context.dp(48), context.dp(48)).apply {
            marginEnd = context.dp(4)
        }
        background = btnBg()
        scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
    }

    fun makeImageButton48(
        iconRes: Int,
        marginStart: Int = 0,
    ) = ImageButton(context).apply {
        layoutParams = LinearLayout.LayoutParams(context.dp(48), context.dp(48)).apply {
            if (marginStart > 0) this.marginStart = context.dp(marginStart)
        }
        background = btnBg()
        setImageResource(iconRes)
        imageTintList = tintColor
    }

    fun makeImageButton40(iconRes: Int, marginStart: Int = 0) = ImageButton(context).apply {
        layoutParams = LinearLayout.LayoutParams(context.dp(40), context.dp(40)).apply {
            if (marginStart > 0) this.marginStart = context.dp(marginStart)
        }
        background = btnBg()
        setImageResource(iconRes)
        imageTintList = tintColor
    }

    fun makeChoiceEditText() = MyEditText(context).apply {
        layoutParams = FrameLayout.LayoutParams(matchParent, wrapContent)
        gravity = Gravity.START or Gravity.TOP
        inputType = InputType.TYPE_CLASS_TEXT
    }

    fun makeChoiceFrame(editText: MyEditText) = FrameLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
        setBackgroundColor(context.attrColor(R.attr.colorPostFormBackground))
        addView(editText)
    }

    fun makeChoiceLabel(textRes: Int) = TextView(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent).apply {
            topMargin = context.dp(3)
        }
        setText(textRes)
    }

    fun makeExpireEditText(defaultText: String = "") = EditText(context).apply {
        layoutParams = LinearLayout.LayoutParams(wrapContent, wrapContent)
        gravity = Gravity.CENTER
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        minWidth = context.dp(48)
        if (defaultText.isNotEmpty()) setText(defaultText)
    }

    fun makeExpireLabel(textRes: Int, marginStart: Int = 0, marginEnd: Int = 0) =
        TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(wrapContent, wrapContent).apply {
                if (marginStart > 0) this.marginStart = context.dp(marginStart)
                if (marginEnd > 0) this.marginEnd = context.dp(marginEnd)
            }
            setText(textRes)
        }

    // --- Reply section ---
    val ivReply = MyNetworkImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(context.dp(40), context.dp(40)).apply {
            marginEnd = context.dp(8)
        }
        background = btnBg()
        scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
    }

    val tvReplyTo = TextView(context).apply {
        layoutParams = LinearLayout.LayoutParams(0, wrapContent, 1f)
        gravity = Gravity.CENTER_VERTICAL
    }

    val btnRemoveReply = ImageButton(context).apply {
        layoutParams = LinearLayout.LayoutParams(context.dp(40), context.dp(40)).apply {
            marginStart = context.dp(4)
        }
        background = btnBg()
        setImageResource(R.drawable.ic_close)
        imageTintList = tintColor
        contentDescription = context.getString(R.string.delete)
    }

    val cbQuote = CheckBox(context).apply {
        layoutParams = LinearLayout.LayoutParams(wrapContent, wrapContent)
        setText(R.string.use_quote_toot)
    }

    val llReply = LinearLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent).apply {
            bottomMargin = context.dp(4)
        }
        setBackgroundColor(context.attrColor(R.attr.colorReplyBackground))
        orientation = LinearLayout.VERTICAL
        val pad = context.dp(6)
        setPadding(pad, pad, pad, pad)

        addView(TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
            setText(R.string.reply_to_this_status)
        })

        addView(LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            addView(ivReply)
            addView(tvReplyTo)
            addView(btnRemoveReply)
        })

        addView(cbQuote)
    }

    // --- Account row ---
    val ivAccount = MyNetworkImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(context.dp(32), context.dp(32)).apply {
            marginEnd = context.dp(2)
        }
        contentDescription = context.getString(R.string.quick_post_account)
        scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
    }

    val btnAccount = Button(context).apply {
        layoutParams = LinearLayout.LayoutParams(0, wrapContent, 1f)
        background = btnBg()
        gravity = Gravity.CENTER_VERTICAL
        val padH = context.dp(8)
        setPadding(padH, paddingTop, padH, paddingBottom)
        isAllCaps = false
    }

    val accountRow = LinearLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
        isBaselineAligned = false
        gravity = Gravity.CENTER_VERTICAL
        orientation = LinearLayout.HORIZONTAL
        addView(TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(wrapContent, wrapContent).apply {
                marginEnd = context.dp(4)
            }
            setText(R.string.post_from)
        })
        addView(ivAccount)
        addView(btnAccount)
    }

    // --- Attachment section ---
    val ivMedia1 = makeMediaView()
    val ivMedia2 = makeMediaView()
    val ivMedia3 = makeMediaView()
    val ivMedia4 = makeMediaView()

    val btnAttachmentsRearrange = ImageButton(context).apply {
        layoutParams = FlexboxLayout.LayoutParams(context.dp(40), context.dp(48)).apply {
            marginEnd = context.dp(4)
        }
        background = btnBg()
        setImageResource(R.drawable.swap_horiz_24px)
        imageTintList = tintColor
        contentDescription = context.getString(R.string.rearrange)
        visibility = View.GONE
    }

    val tvAttachmentProgress = TextView(context).apply {
        layoutParams = FlexboxLayout.LayoutParams(wrapContent, wrapContent)
        maxWidth = context.dp(160)
        textSize = 11f // sp
        visibility = View.GONE
    }

    val llAttachment = FlexboxLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent).apply {
            topMargin = context.dp(4)
        }
        flexWrap = FlexWrap.WRAP
        addView(ivMedia1)
        addView(ivMedia2)
        addView(ivMedia3)
        addView(ivMedia4)
        addView(btnAttachmentsRearrange)
        addView(tvAttachmentProgress)
    }

    // --- NSFW / CW checkboxes ---
    val cbNSFW = CheckBox(context).apply {
        layoutParams = FlexboxLayout.LayoutParams(wrapContent, wrapContent).apply {
            marginEnd = context.dp(4)
        }
        gravity = Gravity.CENTER_VERTICAL or Gravity.START
        setText(R.string.nsfw)
    }

    val cbContentWarning = CheckBox(context).apply {
        layoutParams = FlexboxLayout.LayoutParams(wrapContent, wrapContent)
        gravity = Gravity.CENTER_VERTICAL or Gravity.START
        setText(R.string.content_warning)
    }

    val nsfwCwRow = FlexboxLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent).apply {
            topMargin = context.dp(4)
        }
        flexWrap = FlexWrap.WRAP
        addView(cbNSFW)
        addView(cbContentWarning)
    }

    // --- Content Warning input ---
    val etContentWarning = MyEditText(context).apply {
        layoutParams = FrameLayout.LayoutParams(matchParent, wrapContent)
        setHint(R.string.content_warning_hint)
        inputType = InputType.TYPE_CLASS_TEXT
    }

    val cwFrame = FrameLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
        setBackgroundColor(context.attrColor(R.attr.colorPostFormBackground))
        addView(etContentWarning)
    }

    // --- Content label row (with emoji/hashtag buttons) ---
    val btnFeaturedTag = ImageButton(context).apply {
        layoutParams = LinearLayout.LayoutParams(context.dp(40), context.dp(40)).apply {
            marginStart = context.dp(8)
        }
        background = btnBg()
        setImageResource(R.drawable.ic_hashtag)
        imageTintList = tintColor
        contentDescription = context.getString(R.string.open_picker_emoji)
    }

    val btnEmojiPicker = ImageButton(context).apply {
        layoutParams = LinearLayout.LayoutParams(context.dp(40), context.dp(40)).apply {
            marginStart = context.dp(8)
        }
        background = btnBg()
        setImageResource(R.drawable.ic_face)
        imageTintList = tintColor
        contentDescription = context.getString(R.string.open_picker_emoji)
    }

    val contentLabelRow = LinearLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
        isBaselineAligned = false
        orientation = LinearLayout.HORIZONTAL
        addView(TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, wrapContent, 1f).apply {
                gravity = Gravity.BOTTOM
                topMargin = context.dp(8)
            }
            setText(R.string.content)
        })
        addView(btnFeaturedTag)
        addView(btnEmojiPicker)
    }

    // --- Content input ---
    val etContent = MyEditText(context).apply {
        layoutParams = FrameLayout.LayoutParams(matchParent, wrapContent)
        gravity = Gravity.START or Gravity.TOP
        setHint(R.string.content_hint)
        inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE
        minLines = 5
    }

    val contentFrame = FrameLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
        setBackgroundColor(context.attrColor(R.attr.colorPostFormBackground))
        addView(etContent)
    }

    // --- Language row ---
    val spLanguage = Spinner(context).apply {
        layoutParams = LinearLayout.LayoutParams(0, context.dp(48), 1f)
    }

    val languageRow = LinearLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent).apply {
            topMargin = context.dp(2)
        }
        isBaselineAligned = false
        gravity = Gravity.CENTER_VERTICAL
        orientation = LinearLayout.HORIZONTAL
        addView(TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(wrapContent, wrapContent).apply {
                marginEnd = context.dp(4)
            }
            setText(R.string.language)
        })
        addView(spLanguage)
    }

    // --- Schedule section ---
    val tvSchedule = TextView(context).apply {
        layoutParams = LinearLayout.LayoutParams(0, wrapContent, 1f).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        gravity = Gravity.CENTER
    }

    val ibSchedule = makeImageButton40(R.drawable.ic_edit)
    val ibScheduleReset = makeImageButton40(R.drawable.ic_close)

    val scheduleRow = LinearLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
        orientation = LinearLayout.HORIZONTAL
        addView(tvSchedule)
        addView(ibSchedule)
        addView(ibScheduleReset)
    }

    // --- Poll type spinner ---
    val spPollType = Spinner(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent).apply {
            topMargin = context.dp(32)
        }
    }

    // --- Poll choices ---
    val etChoice1 = makeChoiceEditText()
    val etChoice2 = makeChoiceEditText()
    val etChoice3 = makeChoiceEditText()
    val etChoice4 = makeChoiceEditText()

    val cbMultipleChoice = CheckBox(context).apply {
        layoutParams = LinearLayout.LayoutParams(wrapContent, wrapContent).apply {
            topMargin = context.dp(3)
        }
        setText(R.string.allow_multiple_choice)
    }

    val cbHideTotals = CheckBox(context).apply {
        layoutParams = LinearLayout.LayoutParams(wrapContent, wrapContent).apply {
            topMargin = context.dp(3)
        }
        setText(R.string.hide_totals)
    }

    // --- Expiration row ---
    val etExpireDays = makeExpireEditText("1")
    val etExpireHours = makeExpireEditText()
    val etExpireMinutes = makeExpireEditText()

    val llExpire = LinearLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(wrapContent, wrapContent).apply {
            topMargin = context.dp(3)
        }
        orientation = LinearLayout.HORIZONTAL
        addView(makeExpireLabel(R.string.expiration, marginEnd = 4))
        addView(etExpireDays)
        addView(makeExpireLabel(R.string.poll_expire_days))
        addView(makeExpireLabel(R.string.plus, marginStart = 4, marginEnd = 4))
        addView(etExpireHours)
        addView(makeExpireLabel(R.string.poll_expire_hours))
        addView(makeExpireLabel(R.string.plus, marginStart = 4, marginEnd = 4))
        addView(etExpireMinutes)
        addView(makeExpireLabel(R.string.poll_expire_minutes))
    }

    val llEnquete = LinearLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
        orientation = LinearLayout.VERTICAL
        addView(makeChoiceLabel(R.string.choice1))
        addView(makeChoiceFrame(etChoice1))
        addView(makeChoiceLabel(R.string.choice2))
        addView(makeChoiceFrame(etChoice2))
        addView(makeChoiceLabel(R.string.choice3))
        addView(makeChoiceFrame(etChoice3))
        addView(makeChoiceLabel(R.string.choice4))
        addView(makeChoiceFrame(etChoice4))
        addView(cbMultipleChoice)
        addView(cbHideTotals)
        addView(llExpire)
    }

    // --- Content area (ScrollView child) ---
    val llContent = LinearLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
        orientation = LinearLayout.VERTICAL
        val pad12 = context.dp(12)
        setPadding(pad12, pad12, pad12, context.dp(320))
        addView(llReply)
        addView(accountRow)
        addView(llAttachment)
        addView(nsfwCwRow)
        addView(cwFrame)
        addView(contentLabelRow)
        addView(contentFrame)
        addView(languageRow)
        addView(TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(wrapContent, wrapContent).apply {
                topMargin = context.dp(32)
            }
            setText(R.string.scheduled_status)
        })
        addView(scheduleRow)
        addView(spPollType)
        addView(llEnquete)
    }

    // --- ScrollView ---
    val scrollView = ScrollView(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, 0, 1f)
        setBackgroundColor(context.attrColor(R.attr.colorMainBackground))
        isScrollbarFadingEnabled = false
        setFadingEdgeLength(context.dp(20))
        isFillViewport = true
        isVerticalFadingEdgeEnabled = true
        scrollBarStyle = View.SCROLLBARS_OUTSIDE_OVERLAY
        addView(llContent)
    }

    // --- Footer bar ---
    val btnAttachment = makeImageButton48(R.drawable.ic_clip)
    val btnVisibility = ImageButton(context).apply {
        layoutParams = LinearLayout.LayoutParams(context.dp(48), context.dp(48)).apply {
            marginStart = context.dp(4)
        }
        background = btnBg()
        imageTintList = tintColor
        contentDescription = context.getString(R.string.visibility)
        minimumHeight = context.dp(48)
        minimumWidth = context.dp(48)
    }
    val btnPlugin = makeImageButton48(R.drawable.ic_extension, marginStart = 4)
    val btnMore = makeImageButton48(R.drawable.ic_more, marginStart = 4)

    val tvCharCount = TextView(context).apply {
        layoutParams = LinearLayout.LayoutParams(wrapContent, context.dp(48), 1f).apply {
            marginEnd = context.dp(4)
        }
        gravity = Gravity.END or Gravity.CENTER_VERTICAL
        minWidth = context.dp(32)
    }

    val btnPost = makeImageButton48(R.drawable.ic_send)

    val llFooterBar = LinearLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, context.dp(48))
        setBackgroundColor(context.attrColor(R.attr.colorStatusButtonsPopupBg))
        isBaselineAligned = false
        orientation = LinearLayout.HORIZONTAL
        addView(btnAttachment)
        addView(btnVisibility)
        addView(btnPlugin)
        addView(btnMore)
        // spacer
        addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        })
        addView(tvCharCount)
        addView(btnPost)
    }

    // --- Root ---
    val root = ActPostRootLinearLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(matchParent, matchParent)
        orientation = LinearLayout.VERTICAL
        addView(scrollView)
        addView(llFooterBar)
    }

    return ActPostViews(
        root = root,
        scrollView = scrollView,
        llReply = llReply,
        ivReply = ivReply,
        tvReplyTo = tvReplyTo,
        btnRemoveReply = btnRemoveReply,
        cbQuote = cbQuote,
        ivAccount = ivAccount,
        btnAccount = btnAccount,
        llAttachment = llAttachment,
        ivMedia1 = ivMedia1,
        ivMedia2 = ivMedia2,
        ivMedia3 = ivMedia3,
        ivMedia4 = ivMedia4,
        btnAttachmentsRearrange = btnAttachmentsRearrange,
        tvAttachmentProgress = tvAttachmentProgress,
        cbNSFW = cbNSFW,
        cbContentWarning = cbContentWarning,
        etContentWarning = etContentWarning,
        btnFeaturedTag = btnFeaturedTag,
        btnEmojiPicker = btnEmojiPicker,
        etContent = etContent,
        spLanguage = spLanguage,
        tvSchedule = tvSchedule,
        ibSchedule = ibSchedule,
        ibScheduleReset = ibScheduleReset,
        spPollType = spPollType,
        llEnquete = llEnquete,
        etChoice1 = etChoice1,
        etChoice2 = etChoice2,
        etChoice3 = etChoice3,
        etChoice4 = etChoice4,
        cbMultipleChoice = cbMultipleChoice,
        cbHideTotals = cbHideTotals,
        llExpire = llExpire,
        etExpireDays = etExpireDays,
        etExpireHours = etExpireHours,
        etExpireMinutes = etExpireMinutes,
        llFooterBar = llFooterBar,
        btnAttachment = btnAttachment,
        btnVisibility = btnVisibility,
        btnPlugin = btnPlugin,
        btnMore = btnMore,
        tvCharCount = tvCharCount,
        btnPost = btnPost,
    )
}

class ActPost : AppCompatActivity(),
    View.OnClickListener,
    PostAttachment.Callback,
    MyClickableSpanHandler {

    companion object {
        private val log = LogCategory("ActPost")

        var refActPost: WeakReference<ActPost>? = null

        const val EXTRA_POSTED_ACCT = "posted_acct"
        const val EXTRA_POSTED_STATUS_ID = "posted_status_id"
        const val EXTRA_POSTED_REPLY_ID = "posted_reply_id"
        const val EXTRA_POSTED_REDRAFT_ID = "posted_redraft_id"
        const val EXTRA_MULTI_WINDOW = "multiWindow"

        const val KEY_ACCOUNT_DB_ID = "account_db_id"
        const val KEY_REPLY_STATUS = "reply_status"
        const val KEY_REDRAFT_STATUS = "redraft_status"
        const val KEY_EDIT_STATUS = "edit_status"
        const val KEY_INITIAL_TEXT = "initial_text"
        const val KEY_SHARED_INTENT = "sent_intent"
        const val KEY_QUOTE = "quote"
        const val KEY_SCHEDULED_STATUS = "scheduled_status"

        const val STATE_ALL = "all"

        /////////////////////////////////////////////////

        fun createIntent(
            context: Context,
            accountDbId: Long,
            multiWindowMode: Boolean,
            // 再編集する投稿。アカウントと同一のタンスであること
            redraftStatus: TootStatus? = null,
            // 編集する投稿。アカウントと同一のタンスであること
            editStatus: TootStatus? = null,
            // 返信対象の投稿。同一タンス上に同期済みであること
            replyStatus: TootStatus? = null,
            //初期テキスト
            initialText: String? = null,
            // 外部アプリから共有されたインテント
            sharedIntent: Intent? = null,
            // 返信ではなく引用トゥートを作成する
            quote: Boolean = false,
            //(Mastodon) 予約投稿の編集
            scheduledStatus: TootScheduled? = null,
        ) = Intent(context, ActPost::class.java).apply {
            putExtra(EXTRA_MULTI_WINDOW, multiWindowMode)
            putExtra(KEY_ACCOUNT_DB_ID, accountDbId)
            initialText?.let { putExtra(KEY_INITIAL_TEXT, it) }
            redraftStatus?.let { putExtra(KEY_REDRAFT_STATUS, it.json.toString()) }
            editStatus?.let { putExtra(KEY_EDIT_STATUS, it.json.toString()) }
            replyStatus?.let {
                putExtra(KEY_REPLY_STATUS, it.json.toString())
                putExtra(KEY_QUOTE, quote)
            }
            sharedIntent?.let { putExtra(KEY_SHARED_INTENT, it) }
            scheduledStatus?.let { putExtra(KEY_SCHEDULED_STATUS, it.src.toString()) }
        }
    }

    val views by lazy { createActPostViews(this) }
    lateinit var ivMedia: List<MyNetworkImageView>
    lateinit var etChoices: List<MyEditText>

    lateinit var handler: Handler
    lateinit var appState: AppState
    lateinit var attachmentUploader: AttachmentUploader
    lateinit var attachmentPicker: AttachmentPicker
    lateinit var completionHelper: CompletionHelper

    var density: Float = 0f

    val languages by lazy {
        loadLanguageList()
    }

    private lateinit var progressChannel: Channel<Unit>

    ///////////////////////////////////////////////////

    // SavedAccount.acctAscii => FeaturedTagCache
    val featuredTagCache = ConcurrentHashMap<String, FeaturedTagCache>()

    // background job
    var jobFeaturedTag: WeakReference<Job>? = null
    var jobMaxCharCount: WeakReference<Job>? = null

    ///////////////////////////////////////////////////

    var states = ActPostStates()

    var accountList: List<SavedAccount> = emptyList()
    var account: SavedAccount? = null
    var attachmentList = ArrayList<PostAttachment>()
    var isPostComplete: Boolean = false
    var scheduledStatus: TootScheduled? = null

    /////////////////////////////////////////////////////////////////////

    val isMultiWindowPost: Boolean
        get() = intent.getBooleanExtra(EXTRA_MULTI_WINDOW, false)

    val arMushroom = ActivityResultHandler(log) { r ->
        if (r.isNotOk) return@ActivityResultHandler
        r.data?.string("replace_key")?.let { text ->
            when (states.mushroomInput) {
                0 -> applyMushroomText(views.etContent, text)
                1 -> applyMushroomText(views.etContentWarning, text)
                else -> for (i in 0..3) {
                    if (states.mushroomInput == i + 2) {
                        applyMushroomText(etChoices[i], text)
                    }
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backPressed {
            launchAndShowError {
                finish()
                // 戻るボタンを押したときとonPauseで2回保存することになるが、
                // 同じ内容はDB上は重複しないはず…
                saveDraft()
            }
        }
        if (isMultiWindowPost) ActMain.refActMain?.get()?.closeList?.add(WeakReference(this))
        appState = App1.getAppState(this)
        handler = appState.handler
        attachmentUploader = AttachmentUploader(this, handler)
        attachmentPicker = AttachmentPicker(this, object : AttachmentPicker.Callback {
            override suspend fun onPickAttachment(item: UriAndType) {
                addAttachment(item.uri, item.mimeType)
            }

            override suspend fun onPickCustomThumbnail(
                attachmentId: String?,
                src: UriAndType?,
            ) {
                src ?: return
                val pa = attachmentList.find { it.attachment?.id?.toString() == attachmentId }
                    ?: error("missing attachment for attachmentId=$attachmentId")
                onPickCustomThumbnailImpl(pa, src)
            }
        })

        density = resources.displayMetrics.density
        arMushroom.register(this)

        progressChannel = Channel(capacity = Channel.CONFLATED)

        App1.setActivityTheme(this)
        setContentViewAndInsets(views.root)
        initUI()

        // 進捗表示チャネルの回収コルーチン
        launchAndShowError {
            try {
                while (true) {
                    progressChannel.receive()
                    showMediaAttachmentProgress()
                    delay(1000L)
                }
            } catch (ex: Throwable) {
                when (ex) {
                    is CancellationException, is ClosedReceiveChannelException -> Unit
                    else -> log.e(ex, "can't show media progress.")
                }
            }
        }

        // 初期化の続きをコルーチンでやる
        launchAndShowError {
            when (savedInstanceState) {
                null -> updateText(intent, saveDraft = false)
                else -> restoreState(savedInstanceState)
            }
        }
    }

    override fun onDestroy() {
        try {
            progressChannel.close()
        } catch (ex: Throwable) {
            log.e(ex, "progressChannel close failed.")
        }
        completionHelper.onDestroy()
        attachmentUploader.onActivityDestroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        showContentWarningEnabled()
        showMediaAttachment()
        showVisibility()
        updateTextCount()
        launchAndShowError { showReplyTo() }
        showPoll()
        showQuotedRenote()
    }

    override fun onResume() {
        super.onResume()
        refActPost = WeakReference(this)
    }

    override fun onPause() {
        super.onPause()
        if (!isPostComplete) launchMain {
            try {
                // 編集中にホーム画面を押したり他アプリに移動する場合は下書きを保存する
                // やや過剰な気がするが、自アプリに戻ってくるときにランチャーからアイコンタップされると
                // メイン画面より上にあるアクティビティはすべて消されてしまうので
                // このタイミングで保存するしかない
                saveDraft()
            } catch (ex: Throwable) {
                log.e(ex, "can't save draft.")
                showToast(ex, "can't save draft.")
            }
        }
    }

    override fun onClick(v: View) {
        refActPost = WeakReference(this)
        when (v) {
            views.btnAccount, views.ivAccount -> performAccountChooser()
            views.btnVisibility -> openVisibilityPicker()
            views.btnAttachment -> openAttachment()
            views.ivMedia1 -> performAttachmentClick(0)
            views.ivMedia2 -> performAttachmentClick(1)
            views.ivMedia3 -> performAttachmentClick(2)
            views.ivMedia4 -> performAttachmentClick(3)
            views.btnPost -> performPost()
            views.btnRemoveReply -> launchAndShowError { removeReply() }
            views.btnMore -> performMore()
            views.btnPlugin -> launchAndShowError { openMushroom() }
            views.btnEmojiPicker -> completionHelper.openEmojiPickerFromMore()
            views.btnFeaturedTag -> completionHelper.openFeaturedTagList(
                featuredTagCache[account?.acct?.ascii ?: ""]?.list
            )

            views.btnAttachmentsRearrange -> rearrangeAttachments()
            views.ibSchedule -> performSchedule()
            views.ibScheduleReset -> resetSchedule()
        }
    }

    override fun onKeyShortcut(keyCode: Int, event: KeyEvent?): Boolean {
        return when {
            super.onKeyShortcut(keyCode, event) -> true
            event?.isCtrlPressed == true && keyCode == KeyEvent.KEYCODE_T -> {
                views.btnPost.performClick()
                true
            }

            else -> false
        }
    }

    override fun onMyClickableSpanClicked(viewClicked: View, span: MyClickableSpan) {
        openBrowser(span.linkInfo.url)
    }

    override fun onPostAttachmentProgress() {
        launchIO {
            try {
                progressChannel.send(Unit)
            } catch (ex: Throwable) {
                log.w(ex, "progressChannel send failed.")
            }
        }
    }

    override fun onPostAttachmentComplete(pa: PostAttachment) {
        onPostAttachmentCompleteImpl(pa)
    }

    fun initUI() {
        if (PrefB.bpPostButtonBarTop.value) {
            val bar = views.llFooterBar
            val parent = bar.parent as ViewGroup
            parent.removeView(bar)
            parent.addView(bar, 0)
        }

        if (!isMultiWindowPost) {
            fixHorizontalMargin(views.scrollView)
            fixHorizontalMargin(views.llFooterBar)
        }

        views.root.callbackOnSizeChanged = { _, _, _, _ ->
            if (isMultiWindowPost) saveWindowSize()
            // ビューのw,hはシステムバーその他を含まないので使わない
        }

        // https://github.com/tateisu/SubwayTooter/issues/123
        // 早い段階で指定する必要がある
        views.etContent.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        views.etContent.imeOptions = EditorInfo.IME_ACTION_NONE

        views.spPollType.apply {
            this.adapter = ArrayAdapter(
                this@ActPost,
                android.R.layout.simple_spinner_item,
                arrayOf(
                    getString(R.string.poll_dont_make),
                    getString(R.string.poll_make),
                )
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            this.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    showPoll()
                    updateTextCount()
                }

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    showPoll()
                    updateTextCount()
                }
            }
        }

        ivMedia = listOf(
            views.ivMedia1,
            views.ivMedia2,
            views.ivMedia3,
            views.ivMedia4,
        )

        etChoices = listOf(
            views.etChoice1,
            views.etChoice2,
            views.etChoice3,
            views.etChoice4,
        )

        arrayOf(
            views.ibSchedule,
            views.ibScheduleReset,
            views.btnAccount,
            views.btnVisibility,
            views.btnAttachment,
            views.btnPost,
            views.btnRemoveReply,
            views.btnFeaturedTag,
            views.btnPlugin,
            views.btnEmojiPicker,
            views.btnMore,
            views.ivAccount,
            views.btnAttachmentsRearrange,
        ).forEach { it.setOnClickListener(this) }

        ivMedia.forEach { it.setOnClickListener(this) }

        views.cbContentWarning.setOnCheckedChangeListener { _, _ -> showContentWarningEnabled() }

        completionHelper = CompletionHelper(this, appState.handler)
        completionHelper.attachEditText(
            views.root,
            views.etContent,
            false,
            object : CompletionHelper.Callback2 {
                override fun onTextUpdate() {
                    updateTextCount()
                }

                override fun canOpenPopup(): Boolean = true
            })

        val textWatcher: TextWatcher = object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                updateTextCount()
            }
        }

        views.etContentWarning.addTextChangedListener(textWatcher)

        for (et in etChoices) {
            et.addTextChangedListener(textWatcher)
        }

        val scrollListener: ViewTreeObserver.OnScrollChangedListener =
            ViewTreeObserver.OnScrollChangedListener { completionHelper.onScrollChanged() }

        views.scrollView.viewTreeObserver.addOnScrollChangedListener(scrollListener)

        views.etContent.contentCallback = { addAttachment(it) }

        views.spLanguage.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            languages.map { it.second }.toTypedArray()
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }
}
