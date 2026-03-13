package jp.juggler.subwaytooter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import jp.juggler.subwaytooter.actpost.*
import jp.juggler.subwaytooter.compose.NetworkImage
import jp.juggler.subwaytooter.compose.StThemedContent
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.ui.dp

@Composable
fun ActPostScreen(activity: ActPost, modifier: Modifier = Modifier) {
    StThemedContent {
        val scrollState = rememberScrollState()
        val horizontalPadding = activity.footerHorizontalPaddingDp()

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = horizontalPadding, vertical = 12.dp)
        ) {
            // --- Reply section ---
            if (activity.showReplySection || activity.showQuoteOption) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(6.dp),
                ) {
                    if (activity.showReplySection) {
                        Text(text = activity.getString(R.string.reply_to_this_status))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            NetworkImage(
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(end = 8.dp),
                                cornerRadius = 6.6f.dp.value, // 40dp * 0.165
                                staticUrl = activity.states.inReplyToImage,
                                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER,
                            )
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
                                    contentDescription = activity.getString(R.string.delete),
                                )
                            }
                        }
                    }
                    if (activity.showQuoteOption) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = activity.quoteChecked,
                                onCheckedChange = { activity.quoteChecked = it },
                            )
                            Text(text = activity.getString(R.string.use_quote_toot))
                        }
                    }
                }
            }

            // --- Account row ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = activity.getString(R.string.post_from),
                    modifier = Modifier.padding(end = 4.dp),
                )
                IconButton(onClick = { activity.performAccountChooser() }, modifier = Modifier.size(40.dp)) {
                    NetworkImage(
                        modifier = Modifier.fillMaxSize(),
                        cornerRadius = activity.accountAvatarCorner,
                        staticUrl = activity.accountAvatarStaticUrl,
                        animatedUrl = activity.accountAvatarAnimatedUrl,
                        contentDescription = activity.getString(R.string.quick_post_account),
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

            // --- Attachment row ---
            if (activity.showAttachmentSection) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
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
                                            contentDescription = activity.getString(R.string.media_attachment),
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(slot.fallbackIconRes),
                                            contentDescription = activity.getString(R.string.media_attachment),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (activity.showAttachmentRearrange) {
                            IconButton(
                                onClick = { activity.rearrangeAttachments() },
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.swap_horiz_24px),
                                    contentDescription = activity.getString(R.string.rearrange),
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

            // --- NSFW / CW Checkboxes ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = activity.nsfwChecked,
                        onCheckedChange = { activity.nsfwChecked = it },
                    )
                    Text(text = activity.getString(R.string.nsfw))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = activity.contentWarningChecked,
                        onCheckedChange = {
                            activity.contentWarningChecked = it
                            activity.showContentWarningEnabled()
                            activity.updateTextCount()
                        },
                    )
                    Text(text = activity.getString(R.string.content_warning))
                }
            }

            // --- Content Warning input ---
            if (activity.contentWarningChecked) {
                OutlinedTextField(
                    value = activity.etContentWarning.fieldValue,
                    onValueChange = { activity.etContentWarning.fieldValue = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 3.dp)
                        .onFocusChanged { fs ->
                            if (fs.isFocused) activity.focusedEditField = 1
                        },
                    singleLine = true,
                    label = { Text(activity.getString(R.string.content_warning)) },
                    placeholder = { Text(activity.getString(R.string.content_warning_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                )
            }

            // --- Content label row ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = activity.getString(R.string.content),
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 8.dp),
                )
                IconButton(onClick = {
                    activity.openFeaturedTagList(
                        activity.featuredTagCache[activity.account?.acct?.ascii ?: ""]?.list
                    )
                }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_hashtag),
                        contentDescription = activity.getString(R.string.featured_hashtags),
                    )
                }
                IconButton(onClick = { activity.openEmojiPickerForContent() }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_face),
                        contentDescription = activity.getString(R.string.open_picker_emoji),
                    )
                }
            }

            // --- Content input ---
            OutlinedTextField(
                value = activity.etContent.fieldValue,
                onValueChange = { activity.etContent.fieldValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 3.dp)
                    .focusRequester(activity.contentFocusRequester)
                    .onFocusChanged { fs ->
                        if (fs.isFocused) activity.focusedEditField = 0
                    },
                minLines = 5,
                maxLines = Int.MAX_VALUE,
                label = { Text(activity.getString(R.string.content)) },
                placeholder = { Text(activity.getString(R.string.content_hint)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.None,
                ),
            )

            // --- Language row ---
            var languageExpanded by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = activity.getString(R.string.language),
                    modifier = Modifier.padding(end = 4.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { languageExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            activity.languages.elementAtOrNull(activity.selectedLanguageIndex)?.second
                                ?: activity.getString(R.string.unspecified)
                        )
                    }
                    DropdownMenu(
                        expanded = languageExpanded,
                        onDismissRequest = { languageExpanded = false },
                    ) {
                        activity.languages.forEachIndexed { index, pair ->
                            DropdownMenuItem(
                                text = { Text(pair.second) },
                                onClick = {
                                    languageExpanded = false
                                    activity.selectedLanguageIndex = index
                                },
                            )
                        }
                    }
                }
            }

            // --- Scheduled status label ---
            Text(
                text = activity.getString(R.string.scheduled_status),
                modifier = Modifier.padding(top = 32.dp),
            )

            // --- Schedule row ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = activity.scheduleText,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { activity.performSchedule() }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = activity.getString(R.string.scheduled_status),
                    )
                }
                IconButton(onClick = { activity.resetSchedule() }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = activity.getString(R.string.delete),
                    )
                }
            }

            // --- Poll type row ---
            var pollExpanded by remember { mutableStateOf(false) }
            val pollChoices = listOf(
                activity.getString(R.string.poll_dont_make),
                activity.getString(R.string.poll_make),
            )
            Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp)) {
                OutlinedButton(
                    onClick = { pollExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(pollChoices.getOrElse(activity.pollTypeIndex) { pollChoices[0] })
                }
                DropdownMenu(
                    expanded = pollExpanded,
                    onDismissRequest = { pollExpanded = false },
                ) {
                    pollChoices.forEachIndexed { index, label ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                pollExpanded = false
                                activity.pollTypeIndex = index
                                activity.showPoll()
                                activity.updateTextCount()
                            },
                        )
                    }
                }
            }

            // --- Poll Choices & Expiration ---
            if (activity.pollTypeIndex != 0) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ChoiceField(activity, R.string.choice1, activity.etChoice1, 2)
                    ChoiceField(activity, R.string.choice2, activity.etChoice2, 3)
                    ChoiceField(activity, R.string.choice3, activity.etChoice3, 4)
                    ChoiceField(activity, R.string.choice4, activity.etChoice4, 5)
                    if (activity.pollTypeIndex == 1) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = activity.pollMultipleChoiceChecked,
                                onCheckedChange = { activity.pollMultipleChoiceChecked = it },
                            )
                            Text(text = activity.getString(R.string.allow_multiple_choice))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = activity.pollHideTotalsChecked,
                                onCheckedChange = { activity.pollHideTotalsChecked = it },
                            )
                            Text(text = activity.getString(R.string.hide_totals))
                        }
                        Row(modifier = Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activity.getString(R.string.expiration),
                                modifier = Modifier.padding(end = 4.dp),
                            )
                            ExpireField(activity.etExpireDays)
                            Text(text = activity.getString(R.string.poll_expire_days))
                            Text(
                                text = activity.getString(R.string.plus),
                                modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                            )
                            ExpireField(activity.etExpireHours)
                            Text(text = activity.getString(R.string.poll_expire_hours))
                            Text(
                                text = activity.getString(R.string.plus),
                                modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                            )
                            ExpireField(activity.etExpireMinutes)
                            Text(text = activity.getString(R.string.poll_expire_minutes))
                        }
                    }
                }
            }
            
            // Bottom spacing (like in original scrollView padding)
            Spacer(modifier = Modifier.height(320.dp))
        }
    }
}

@Composable
fun ChoiceField(activity: ActPost, textRes: Int, state: TextEditState, fieldIndex: Int) {
    OutlinedTextField(
        value = state.fieldValue,
        onValueChange = { state.fieldValue = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 3.dp)
            .onFocusChanged { fs ->
                if (fs.isFocused) activity.focusedEditField = fieldIndex
            },
        label = { Text(activity.getString(textRes)) },
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
