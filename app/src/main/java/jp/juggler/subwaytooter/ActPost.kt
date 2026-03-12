package jp.juggler.subwaytooter

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import jp.juggler.subwaytooter.compose.StThemedContent
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import jp.juggler.subwaytooter.action.saveWindowSize
import jp.juggler.subwaytooter.actpost.ActPostStates
import jp.juggler.subwaytooter.actpost.FeaturedTagCache
import jp.juggler.subwaytooter.actpost.TextEditState
import jp.juggler.subwaytooter.actpost.addAttachment
import jp.juggler.subwaytooter.actpost.applyMushroomText
import jp.juggler.subwaytooter.actpost.onPickCustomThumbnailImpl
import jp.juggler.subwaytooter.actpost.onPostAttachmentCompleteImpl
import jp.juggler.subwaytooter.actpost.openAttachment
import jp.juggler.subwaytooter.actpost.openMushroom
import jp.juggler.subwaytooter.actpost.openEmojiPickerForContent
import jp.juggler.subwaytooter.actpost.openFeaturedTagList
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
import jp.juggler.subwaytooter.getVisibilityIconId
import jp.juggler.subwaytooter.pref.PrefB
import jp.juggler.subwaytooter.pref.PrefI
import jp.juggler.subwaytooter.span.MyClickableSpan
import jp.juggler.subwaytooter.span.MyClickableSpanHandler
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.subwaytooter.util.AttachmentPicker
import jp.juggler.subwaytooter.util.AttachmentUploader
import jp.juggler.subwaytooter.util.PostAttachment
import jp.juggler.subwaytooter.util.loadLanguageList
import jp.juggler.subwaytooter.util.openBrowser
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.lang.ref.WeakReference
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import com.google.android.material.R as MR

class ActPostViews(
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
    val cwFrame: View,
    val etContentWarning: TextEditState,
    val btnFeaturedTag: ImageButton,
    val btnEmojiPicker: ImageButton,
    val etContent: TextEditState,
    val etContentView: View,
    val spLanguage: Spinner,
    val tvSchedule: TextView,
    val ibSchedule: ImageButton,
    val ibScheduleReset: ImageButton,
    val spPollType: Spinner,
    val llEnquete: LinearLayout,
    val etChoice1: TextEditState,
    val etChoice2: TextEditState,
    val etChoice3: TextEditState,
    val etChoice4: TextEditState,
    val cbMultipleChoice: CheckBox,
    val cbHideTotals: CheckBox,
    val llExpire: LinearLayout,
    val etExpireDays: TextEditState,
    val etExpireHours: TextEditState,
    val etExpireMinutes: TextEditState,
)

@Suppress("LongMethod")
fun createActPostViews(context: Context): ActPostViews {
    val matchParent = ViewGroup.LayoutParams.MATCH_PARENT
    val wrapContent = ViewGroup.LayoutParams.WRAP_CONTENT

    val tintColor = ColorStateList.valueOf(context.attrColor(MR.attr.colorOnSurface))
    val colorSurfaceContainerHigh = context.attrColor(MR.attr.colorSurfaceContainerHigh)
    val colorSurfaceContainerHighest = context.attrColor(MR.attr.colorSurfaceContainerHighest)
    val colorSurface = context.attrColor(MR.attr.colorSurface)
    val colorSurfaceContainer = context.attrColor(MR.attr.colorSurfaceContainer)
    val btnBg = ContextCompat.getDrawable(context, R.drawable.btn_bg_transparent_round6dp)
    fun btnBg() = ContextCompat.getDrawable(context, R.drawable.btn_bg_transparent_round6dp)

    fun makeMediaView() = MyNetworkImageView(context).apply {
        layoutParams = FlexboxLayout.LayoutParams(context.dp(48), context.dp(48)).apply {
            marginEnd = context.dp(4)
        }
        background = btnBg()
        scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
    }

    fun makeImageButton40(iconRes: Int, marginStart: Int = 0) = ImageButton(context).apply {
        layoutParams = LinearLayout.LayoutParams(context.dp(40), context.dp(40)).apply {
            if (marginStart > 0) this.marginStart = context.dp(marginStart)
        }
        background = btnBg()
        setImageResource(iconRes)
        imageTintList = tintColor
    }

    fun makeChoiceEditText() = TextEditState()
    fun makeChoiceComposeView(state: TextEditState, fieldIndex: Int) = ComposeView(context).apply {
        layoutParams = FrameLayout.LayoutParams(matchParent, wrapContent)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            StThemedContent {
                val textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface)
                BasicTextField(
                    value = state.fieldValue,
                    onValueChange = { state.fieldValue = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { fs ->
                            if (fs.isFocused) (context as ActPost).focusedEditField = fieldIndex
                        },
                    textStyle = textStyle,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    decorationBox = { innerTextField ->
                        Box(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                            innerTextField()
                        }
                    },
                )
            }
        }
    }
    fun makeChoiceFrame(state: TextEditState, fieldIndex: Int) = FrameLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
        setBackgroundColor(colorSurfaceContainerHigh)
        addView(makeChoiceComposeView(state, fieldIndex))
    }

    fun makeChoiceLabel(textRes: Int) = TextView(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent).apply {
            topMargin = context.dp(3)
        }
        setText(textRes)
    }

    fun makeExpireEditText(defaultText: String = "") = TextEditState(defaultText)
    fun makeExpireComposeView(state: TextEditState) = ComposeView(context).apply {
        layoutParams = LinearLayout.LayoutParams(wrapContent, wrapContent)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            StThemedContent {
                BasicTextField(
                    value = state.fieldValue,
                    onValueChange = { state.fieldValue = it },
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    decorationBox = { innerTextField ->
                        Box {
                            if (state.text.isEmpty()) {
                                Text("0", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }
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
        setBackgroundColor(colorSurfaceContainerHighest)
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
    val etContentWarning = TextEditState()
    val etContentWarningView = ComposeView(context).apply {
        layoutParams = FrameLayout.LayoutParams(matchParent, wrapContent)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            StThemedContent {
                val textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface)
                BasicTextField(
                    value = etContentWarning.fieldValue,
                    onValueChange = { etContentWarning.fieldValue = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { fs ->
                            if (fs.isFocused) (context as ActPost).focusedEditField = 1
                        },
                    textStyle = textStyle,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    decorationBox = { innerTextField ->
                        Box(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                            if (etContentWarning.text.isEmpty()) {
                                Text(
                                    context.getString(R.string.content_warning_hint),
                                    style = textStyle.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)),
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }
    }

    val cwFrame = FrameLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
        setBackgroundColor(colorSurfaceContainerHigh)
        addView(etContentWarningView)
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
    val etContent = TextEditState()
    val etContentView = ComposeView(context).apply {
        layoutParams = FrameLayout.LayoutParams(matchParent, wrapContent)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            StThemedContent {
                val textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface)
                BasicTextField(
                    value = etContent.fieldValue,
                    onValueChange = { etContent.fieldValue = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester((context as ActPost).contentFocusRequester)
                        .onFocusChanged { fs ->
                            if (fs.isFocused) (context as ActPost).focusedEditField = 0
                        },
                    textStyle = textStyle,
                    minLines = 5,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.None,
                    ),
                    decorationBox = { innerTextField ->
                        Box(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                            if (etContent.text.isEmpty()) {
                                Text(
                                    context.getString(R.string.content_hint),
                                    style = textStyle.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)),
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }
    }

    val contentFrame = FrameLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
        setBackgroundColor(colorSurfaceContainerHigh)
        addView(etContentView)
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
        addView(makeExpireComposeView(etExpireDays))
        addView(makeExpireLabel(R.string.poll_expire_days))
        addView(makeExpireLabel(R.string.plus, marginStart = 4, marginEnd = 4))
        addView(makeExpireComposeView(etExpireHours))
        addView(makeExpireLabel(R.string.poll_expire_hours))
        addView(makeExpireLabel(R.string.plus, marginStart = 4, marginEnd = 4))
        addView(makeExpireComposeView(etExpireMinutes))
        addView(makeExpireLabel(R.string.poll_expire_minutes))
    }

    val llEnquete = LinearLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
        orientation = LinearLayout.VERTICAL
        addView(makeChoiceLabel(R.string.choice1))
        addView(makeChoiceFrame(etChoice1, 2))
        addView(makeChoiceLabel(R.string.choice2))
        addView(makeChoiceFrame(etChoice2, 3))
        addView(makeChoiceLabel(R.string.choice3))
        addView(makeChoiceFrame(etChoice3, 4))
        addView(makeChoiceLabel(R.string.choice4))
        addView(makeChoiceFrame(etChoice4, 5))
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
        setBackgroundColor(colorSurface)
        isScrollbarFadingEnabled = false
        setFadingEdgeLength(context.dp(20))
        isFillViewport = true
        isVerticalFadingEdgeEnabled = true
        scrollBarStyle = View.SCROLLBARS_OUTSIDE_OVERLAY
        addView(llContent)
    }

    return ActPostViews(
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
        cwFrame = cwFrame,
        etContentWarning = etContentWarning,
        btnFeaturedTag = btnFeaturedTag,
        btnEmojiPicker = btnEmojiPicker,
        etContent = etContent,
        etContentView = etContentView,
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
    )
}

class ActPost : ComponentActivity(),
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
    val etChoices: List<TextEditState> get() = listOf(views.etChoice1, views.etChoice2, views.etChoice3, views.etChoice4)

    /** Which text field has focus: 0=content, 1=cw, 2-5=choice1-4. Used by Mushroom plugin. */
    var focusedEditField: Int = 0

    /** FocusRequester wired to etContent's BasicTextField. */
    val contentFocusRequester = FocusRequester()

    var charCountText by mutableStateOf("")
    var charCountColorArgb by mutableIntStateOf(0)
    var visibilityIconRes by mutableIntStateOf(R.drawable.ic_public)

    lateinit var handler: Handler
    lateinit var appState: AppState
    lateinit var attachmentUploader: AttachmentUploader
    lateinit var attachmentPicker: AttachmentPicker

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

        charCountColorArgb = attrColor(android.R.attr.textColorPrimary)
        visibilityIconRes = (states.visibility ?: jp.juggler.subwaytooter.api.entity.TootVisibility.Public)
            .getVisibilityIconId(account?.isMisskey == true)

        App1.setActivityTheme(this)
        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.systemBars.union(WindowInsets.ime).asPaddingValues())
                    .onSizeChanged {
                        if (isMultiWindowPost) saveWindowSize()
                    }
            ) {
                if (PrefB.bpPostButtonBarTop.value) PostFooterBar()
                AndroidView(
                    factory = { views.scrollView },
                    modifier = Modifier.weight(1f),
                )
                if (!PrefB.bpPostButtonBarTop.value) PostFooterBar()
            }
        }
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
            views.ivMedia1 -> performAttachmentClick(0)
            views.ivMedia2 -> performAttachmentClick(1)
            views.ivMedia3 -> performAttachmentClick(2)
            views.ivMedia4 -> performAttachmentClick(3)
            views.btnRemoveReply -> launchAndShowError { removeReply() }
            views.btnEmojiPicker -> launchAndShowError { openEmojiPickerForContent() }
            views.btnFeaturedTag -> launchAndShowError {
                openFeaturedTagList(featuredTagCache[account?.acct?.ascii ?: ""]?.list)
            }

            views.btnAttachmentsRearrange -> rearrangeAttachments()
            views.ibSchedule -> performSchedule()
            views.ibScheduleReset -> resetSchedule()
        }
    }

    override fun onKeyShortcut(keyCode: Int, event: KeyEvent?): Boolean {
        return when {
            super.onKeyShortcut(keyCode, event) -> true
            event?.isCtrlPressed == true && keyCode == KeyEvent.KEYCODE_T -> {
                performPost()
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

    @Composable
    private fun PostFooterBar() {
        val horizontalPadding = footerHorizontalPaddingDp()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = horizontalPadding),
        ) {
            FooterIconButton(R.drawable.ic_clip, getString(R.string.media_attachment)) {
                openAttachment()
            }
            FooterIconButton(
                visibilityIconRes,
                getString(R.string.visibility),
            ) {
                openVisibilityPicker()
            }
            FooterIconButton(R.drawable.ic_extension, getString(R.string.plugin_app_intro)) {
                launchAndShowError { openMushroom() }
            }
            FooterIconButton(R.drawable.ic_more, getString(R.string.more)) {
                performMore()
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = charCountText,
                color = Color(charCountColorArgb),
                modifier = Modifier
                    .padding(end = 4.dp)
                    .align(androidx.compose.ui.Alignment.CenterVertically),
            )
            FooterIconButton(R.drawable.ic_send, getString(R.string.toot)) {
                performPost()
            }
        }
    }

    @Composable
    private fun FooterIconButton(
        iconRes: Int,
        contentDescription: String,
        onClick: () -> Unit,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
            )
        }
    }

    private fun footerHorizontalPaddingDp() = run {
        val dm = resources.displayMetrics
        val widthDp = dm.widthPixels / dm.density
        val basePx = if (
            widthDp >= 640f &&
            resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        ) {
            when (PrefI.ipJustifyWindowContentPortrait.value) {
                PrefI.JWCP_START,
                PrefI.JWCP_END,
                -> 0
                else -> kotlin.math.max(0, (dm.widthPixels - (0.5f + 460f * dm.density).toInt()) / 2)
            }
        } else {
            kotlin.math.max(0, (dm.widthPixels - (0.5f + 460f * dm.density).toInt()) / 2)
        }
        (basePx / dm.density).dp
    }

    fun initUI() {
        if (!isMultiWindowPost) {
            fixHorizontalMargin(views.scrollView)
        }

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

        arrayOf(
            views.ibSchedule,
            views.ibScheduleReset,
            views.btnAccount,
            views.btnRemoveReply,
            views.btnFeaturedTag,
            views.btnEmojiPicker,
            views.ivAccount,
            views.btnAttachmentsRearrange,
        ).forEach { it.setOnClickListener(this) }

        ivMedia.forEach { it.setOnClickListener(this) }

        views.cbContentWarning.setOnCheckedChangeListener { _, _ -> showContentWarningEnabled() }

        // Observe all text fields to update the character count
        launchMain {
            androidx.compose.runtime.snapshotFlow { views.etContent.fieldValue.text }
                .collectLatest { updateTextCount() }
        }
        launchMain {
            androidx.compose.runtime.snapshotFlow { views.etContentWarning.fieldValue.text }
                .collectLatest { updateTextCount() }
        }
        for (et in etChoices) {
            launchMain {
                androidx.compose.runtime.snapshotFlow { et.fieldValue.text }
                    .collectLatest { updateTextCount() }
            }
        }

        views.spLanguage.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            languages.map { it.second }.toTypedArray()
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }
}
