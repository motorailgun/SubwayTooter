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
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import jp.juggler.subwaytooter.compose.NetworkImage
import jp.juggler.subwaytooter.compose.StThemedContent
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

data class AttachmentSlotUi(
    val visible: Boolean = false,
    val previewUrl: String? = null,
    val fallbackIconRes: Int = R.drawable.ic_clip,
)

class ActPostViews(
    val scrollView: ScrollView,
    val ivReply: MyNetworkImageView,
    val etContentWarning: TextEditState,
    val etContent: TextEditState,
    val etChoice1: TextEditState,
    val etChoice2: TextEditState,
    val etChoice3: TextEditState,
    val etChoice4: TextEditState,
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
    val btnBg = ContextCompat.getDrawable(context, R.drawable.btn_bg_transparent_round6dp)
    fun btnBg() = ContextCompat.getDrawable(context, R.drawable.btn_bg_transparent_round6dp)

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
    fun makeExpireEditText(defaultText: String = "") = TextEditState(defaultText)
    @Composable
    fun ChoiceField(textRes: Int, state: TextEditState, fieldIndex: Int) {
        OutlinedTextField(
            value = state.fieldValue,
            onValueChange = { state.fieldValue = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp)
                .onFocusChanged { fs ->
                    if (fs.isFocused) (context as ActPost).focusedEditField = fieldIndex
                },
            label = { Text(context.getString(textRes)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
    }

    @Composable
    fun ExpireField(state: TextEditState) {
        OutlinedTextField(
            value = state.fieldValue,
            onValueChange = { state.fieldValue = it },
            modifier = Modifier
                .width(84.dp)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            placeholder = { Text("0") },
        )
    }

    // --- Reply section ---
    val ivReply = MyNetworkImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(context.dp(40), context.dp(40)).apply {
            marginEnd = context.dp(8)
        }
        background = btnBg()
        scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
    }

    val replyRow = ComposeView(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent).apply {
            bottomMargin = context.dp(4)
        }
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            StThemedContent {
                val activity = context as ActPost
                if (activity.showReplySection || activity.showQuoteOption) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(colorSurfaceContainerHighest))
                            .padding(6.dp),
                    ) {
                        if (activity.showReplySection) {
                            Text(text = context.getString(R.string.reply_to_this_status))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            ) {
                                AndroidView(factory = { ivReply })
                                Text(
                                    text = activity.replyToText,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 8.dp),
                                )
                                IconButton(
                                    onClick = { activity.launchAndShowError { activity.removeReply() } },
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_close),
                                        contentDescription = context.getString(R.string.delete),
                                    )
                                }
                            }
                        }
                        if (activity.showQuoteOption) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                androidx.compose.material3.Checkbox(
                                    checked = activity.quoteChecked,
                                    onCheckedChange = { activity.quoteChecked = it },
                                )
                                Text(text = context.getString(R.string.use_quote_toot))
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Account row ---
    val accountRow = ComposeView(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            StThemedContent {
                val activity = context as ActPost
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text(
                        text = context.getString(R.string.post_from),
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    IconButton(onClick = { activity.performAccountChooser() }, modifier = Modifier.size(40.dp)) {
                        NetworkImage(
                            modifier = Modifier.fillMaxSize(),
                            cornerRadius = activity.accountAvatarCorner,
                            staticUrl = activity.accountAvatarStaticUrl,
                            animatedUrl = activity.accountAvatarAnimatedUrl,
                            contentDescription = context.getString(R.string.quick_post_account),
                            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER,
                        )
                    }
                    TextButton(
                        onClick = { activity.performAccountChooser() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(activity.accountButtonText)
                    }
                }
            }
        }
    }

    val attachmentRow = ComposeView(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent).apply {
            topMargin = context.dp(4)
        }
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            StThemedContent {
                val activity = context as ActPost
                if (activity.showAttachmentSection) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row {
                            activity.attachmentSlots.forEachIndexed { index, slot ->
                                if (slot.visible) {
                                    IconButton(
                                        onClick = { activity.performAttachmentClick(index) },
                                        modifier = Modifier.size(48.dp),
                                    ) {
                                        if (slot.previewUrl != null) {
                                            NetworkImage(
                                                modifier = Modifier.fillMaxSize(),
                                                cornerRadius = activity.attachmentThumbCorner,
                                                staticUrl = slot.previewUrl,
                                                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER,
                                                contentDescription = context.getString(R.string.media_attachment),
                                            )
                                        } else {
                                            Icon(
                                                painter = painterResource(slot.fallbackIconRes),
                                                contentDescription = context.getString(R.string.media_attachment),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            if (activity.showAttachmentRearrange) {
                                IconButton(
                                    onClick = { activity.rearrangeAttachments() },
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.swap_horiz_24px),
                                        contentDescription = context.getString(R.string.rearrange),
                                    )
                                }
                            }
                            if (activity.attachmentProgressText.isNotEmpty()) {
                                Text(
                                    text = activity.attachmentProgressText,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val nsfwCwRow = ComposeView(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent).apply {
            topMargin = context.dp(4)
        }
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            StThemedContent {
                val activity = context as ActPost
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(
                            checked = activity.nsfwChecked,
                            onCheckedChange = { activity.nsfwChecked = it },
                        )
                        Text(text = context.getString(R.string.nsfw))
                    }
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(
                            checked = activity.contentWarningChecked,
                            onCheckedChange = {
                                activity.contentWarningChecked = it
                                activity.showContentWarningEnabled()
                                activity.updateTextCount()
                            },
                        )
                        Text(text = context.getString(R.string.content_warning))
                    }
                }
            }
        }
    }

    // --- Content Warning input ---
    val etContentWarning = TextEditState()
    val cwFrame = ComposeView(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            StThemedContent {
                if ((context as ActPost).contentWarningChecked) {
                    OutlinedTextField(
                        value = etContentWarning.fieldValue,
                        onValueChange = { etContentWarning.fieldValue = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 3.dp)
                            .onFocusChanged { fs ->
                                if (fs.isFocused) (context as ActPost).focusedEditField = 1
                            },
                        singleLine = true,
                        label = { Text(context.getString(R.string.content_warning)) },
                        placeholder = { Text(context.getString(R.string.content_warning_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    )
                }
            }
        }
    }

    // --- Content label row (with emoji/hashtag buttons) ---
    val contentLabelRow = ComposeView(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            StThemedContent {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text(
                        text = context.getString(R.string.content),
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 8.dp),
                    )
                    IconButton(onClick = {
                        (context as ActPost).openFeaturedTagList(
                            (context as ActPost).featuredTagCache[(context as ActPost).account?.acct?.ascii ?: ""]?.list
                        )
                    }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_hashtag),
                            contentDescription = context.getString(R.string.featured_hashtags),
                        )
                    }
                    IconButton(onClick = { (context as ActPost).openEmojiPickerForContent() }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_face),
                            contentDescription = context.getString(R.string.open_picker_emoji),
                        )
                    }
                }
            }
        }
    }

    // --- Content input ---
    val etContent = TextEditState()
    val contentFrame = ComposeView(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            StThemedContent {
                OutlinedTextField(
                    value = etContent.fieldValue,
                    onValueChange = { etContent.fieldValue = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 3.dp)
                        .focusRequester((context as ActPost).contentFocusRequester)
                        .onFocusChanged { fs ->
                            if (fs.isFocused) (context as ActPost).focusedEditField = 0
                        },
                    minLines = 5,
                    maxLines = Int.MAX_VALUE,
                    label = { Text(context.getString(R.string.content)) },
                    placeholder = { Text(context.getString(R.string.content_hint)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.None,
                    ),
                )
            }
        }
    }
    // --- Language row ---
    val languageRow = ComposeView(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent).apply {
            topMargin = context.dp(2)
        }
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            StThemedContent {
                val activity = context as ActPost
                var expanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text(
                        text = context.getString(R.string.language),
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                activity.languages.elementAtOrNull(activity.selectedLanguageIndex)?.second
                                    ?: context.getString(R.string.unspecified)
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            activity.languages.forEachIndexed { index, pair ->
                                DropdownMenuItem(
                                    text = { Text(pair.second) },
                                    onClick = {
                                        expanded = false
                                        activity.selectedLanguageIndex = index
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Schedule section ---
    val scheduleRow = ComposeView(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            StThemedContent {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text(
                        text = (context as ActPost).scheduleText,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { (context as ActPost).performSchedule() }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit),
                            contentDescription = context.getString(R.string.scheduled_status),
                        )
                    }
                    IconButton(onClick = { (context as ActPost).resetSchedule() }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = context.getString(R.string.delete),
                        )
                    }
                }
            }
        }
    }

    val pollTypeRow = ComposeView(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent).apply {
            topMargin = context.dp(32)
        }
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            StThemedContent {
                val activity = context as ActPost
                var expanded by remember { mutableStateOf(false) }
                val choices = listOf(
                    context.getString(R.string.poll_dont_make),
                    context.getString(R.string.poll_make),
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(choices.getOrElse(activity.pollTypeIndex) { choices[0] })
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        choices.forEachIndexed { index, label ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    expanded = false
                                    activity.pollTypeIndex = index
                                    activity.showPoll()
                                    activity.updateTextCount()
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Poll choices ---
    val etChoice1 = makeChoiceEditText()
    val etChoice2 = makeChoiceEditText()
    val etChoice3 = makeChoiceEditText()
    val etChoice4 = makeChoiceEditText()

    // --- Expiration row ---
    val etExpireDays = makeExpireEditText("1")
    val etExpireHours = makeExpireEditText()
    val etExpireMinutes = makeExpireEditText()

    val llEnquete = ComposeView(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            StThemedContent {
                val activity = context as ActPost
                if (activity.pollTypeIndex != 0) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ChoiceField(R.string.choice1, etChoice1, 2)
                        ChoiceField(R.string.choice2, etChoice2, 3)
                        ChoiceField(R.string.choice3, etChoice3, 4)
                        ChoiceField(R.string.choice4, etChoice4, 5)
                        if (activity.pollTypeIndex == 1) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                androidx.compose.material3.Checkbox(
                                    checked = activity.pollMultipleChoiceChecked,
                                    onCheckedChange = { activity.pollMultipleChoiceChecked = it },
                                )
                                Text(text = context.getString(R.string.allow_multiple_choice))
                            }
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                androidx.compose.material3.Checkbox(
                                    checked = activity.pollHideTotalsChecked,
                                    onCheckedChange = { activity.pollHideTotalsChecked = it },
                                )
                                Text(text = context.getString(R.string.hide_totals))
                            }
                            Row(modifier = Modifier.padding(top = 3.dp)) {
                                Text(
                                    text = context.getString(R.string.expiration),
                                    modifier = Modifier.padding(end = 4.dp),
                                )
                                ExpireField(etExpireDays)
                                Text(text = context.getString(R.string.poll_expire_days))
                                Text(
                                    text = context.getString(R.string.plus),
                                    modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                                )
                                ExpireField(etExpireHours)
                                Text(text = context.getString(R.string.poll_expire_hours))
                                Text(
                                    text = context.getString(R.string.plus),
                                    modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                                )
                                ExpireField(etExpireMinutes)
                                Text(text = context.getString(R.string.poll_expire_minutes))
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Content area (ScrollView child) ---
    val llContent = LinearLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
        orientation = LinearLayout.VERTICAL
        val pad12 = context.dp(12)
        setPadding(pad12, pad12, pad12, context.dp(320))
        addView(replyRow)
        addView(accountRow)
        addView(attachmentRow)
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
        addView(pollTypeRow)
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
        ivReply = ivReply,
        etContentWarning = etContentWarning,
        etContent = etContent,
        etChoice1 = etChoice1,
        etChoice2 = etChoice2,
        etChoice3 = etChoice3,
        etChoice4 = etChoice4,
        etExpireDays = etExpireDays,
        etExpireHours = etExpireHours,
        etExpireMinutes = etExpireMinutes,
    )
}

class ActPost : ComponentActivity(),
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
    val etChoices: List<TextEditState> get() = listOf(views.etChoice1, views.etChoice2, views.etChoice3, views.etChoice4)

    /** Which text field has focus: 0=content, 1=cw, 2-5=choice1-4. Used by Mushroom plugin. */
    var focusedEditField: Int = 0

    /** FocusRequester wired to etContent's BasicTextField. */
    val contentFocusRequester = FocusRequester()

    var charCountText by mutableStateOf("")
    var charCountColorArgb by mutableIntStateOf(0)
    var visibilityIconRes by mutableIntStateOf(R.drawable.ic_public)
    var scheduleText by mutableStateOf("")
    var pollTypeIndex by mutableIntStateOf(0)
    var pollMultipleChoiceChecked by mutableStateOf(false)
    var pollHideTotalsChecked by mutableStateOf(false)
    var nsfwChecked by mutableStateOf(false)
    var contentWarningChecked by mutableStateOf(false)
    var quoteChecked by mutableStateOf(false)
    var showQuoteOption by mutableStateOf(false)
    var showReplySection by mutableStateOf(false)
    var replyToText by mutableStateOf("")
    var accountButtonText by mutableStateOf("")
    var accountAvatarStaticUrl by mutableStateOf<String?>(null)
    var accountAvatarAnimatedUrl by mutableStateOf<String?>(null)
    var accountAvatarCorner by mutableStateOf(0f)
    var showAttachmentSection by mutableStateOf(false)
    var selectedLanguageIndex by mutableIntStateOf(0)
    var attachmentSlots by mutableStateOf(List(4) { AttachmentSlotUi() })
    var attachmentThumbCorner by mutableStateOf(0f)
    var showAttachmentRearrange by mutableStateOf(false)
    var attachmentProgressText by mutableStateOf("")

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
        scheduleText = getString(R.string.unspecified)
        pollTypeIndex = 0
        nsfwChecked = false
        contentWarningChecked = false
        quoteChecked = false
        showQuoteOption = false
        showReplySection = false
        replyToText = ""
        accountButtonText = getString(R.string.not_selected_2)
        accountAvatarStaticUrl = null
        accountAvatarAnimatedUrl = null
        accountAvatarCorner = calcIconRound(dp(32))
        showAttachmentSection = false
        selectedLanguageIndex = 0
        attachmentSlots = List(4) { AttachmentSlotUi() }
        attachmentThumbCorner = calcIconRound(dp(48))
        showAttachmentRearrange = false
        attachmentProgressText = ""

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

    }
}
