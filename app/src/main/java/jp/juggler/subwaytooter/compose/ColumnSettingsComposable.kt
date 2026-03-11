package jp.juggler.subwaytooter.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.juggler.subwaytooter.R

/**
 * Compose replacement for inflateColumnSetting() in ColumnViewHolder.
 * Scrollable panel with checkboxes, regex filter, hashtag extras, action buttons.
 */
@Composable
fun ColumnSettingsPanel(
    uiState: ColumnUiState,
    callbacks: ColumnCallbacks,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = uiState.settingsVisible) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(Color(uiState.settingsBgColor))
                .heightIn(max = 240.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 3.dp),
        ) {
            // Quick filter (when moved into settings)
            if (uiState.quickFilterInsideSetting && uiState.quickFilterVisible) {
                ColumnQuickFilterBar(uiState = uiState, callbacks = callbacks)
            }

            // Hashtag extra fields
            if (uiState.showHashtagExtra) {
                HashtagExtraFields(uiState = uiState, callbacks = callbacks)
            }

            // Checkboxes
            SettingCheckbox(
                text = stringResource(R.string.dont_close_column),
                checked = uiState.dontClose,
                visible = true,
                onCheckedChange = callbacks.onDontCloseChanged,
            )
            SettingCheckbox(
                text = stringResource(R.string.remote_only),
                checked = uiState.remoteOnly,
                visible = uiState.showRemoteOnly,
                onCheckedChange = callbacks.onRemoteOnlyChanged,
            )
            SettingCheckbox(
                text = stringResource(R.string.show_media_description),
                checked = uiState.showMediaDescription,
                visible = true,
                onCheckedChange = callbacks.onShowMediaDescriptionChanged,
            )
            SettingCheckbox(
                text = stringResource(R.string.with_highlight),
                checked = uiState.withHighlight,
                visible = uiState.showWithHighlight,
                onCheckedChange = callbacks.onWithHighlightChanged,
            )
            SettingCheckbox(
                text = stringResource(R.string.dont_show_boost),
                checked = uiState.dontShowBoost,
                visible = uiState.showDontShowBoost,
                onCheckedChange = callbacks.onDontShowBoostChanged,
            )
            SettingCheckbox(
                text = stringResource(R.string.dont_show_favourite),
                checked = uiState.dontShowFavourite,
                visible = uiState.showDontShowFavourite,
                onCheckedChange = callbacks.onDontShowFavouriteChanged,
            )
            SettingCheckbox(
                text = stringResource(R.string.dont_show_follow),
                checked = uiState.dontShowFollow,
                visible = uiState.showDontShowFollow,
                onCheckedChange = callbacks.onDontShowFollowChanged,
            )
            SettingCheckbox(
                text = stringResource(R.string.dont_show_reply),
                checked = uiState.dontShowReply,
                visible = uiState.showDontShowReply,
                onCheckedChange = callbacks.onDontShowReplyChanged,
            )
            SettingCheckbox(
                text = stringResource(R.string.dont_show_reaction),
                checked = uiState.dontShowReaction,
                visible = uiState.showDontShowReaction,
                onCheckedChange = callbacks.onDontShowReactionChanged,
            )
            SettingCheckbox(
                text = stringResource(R.string.dont_show_vote),
                checked = uiState.dontShowVote,
                visible = uiState.showDontShowVote,
                onCheckedChange = callbacks.onDontShowVoteChanged,
            )
            SettingCheckbox(
                text = stringResource(R.string.dont_show_normal_toot),
                checked = uiState.dontShowNormalToot,
                visible = uiState.showDontShowNormalToot,
                onCheckedChange = callbacks.onDontShowNormalTootChanged,
            )
            SettingCheckbox(
                text = stringResource(R.string.dont_show_non_public_toot),
                checked = uiState.dontShowNonPublicToot,
                visible = uiState.showDontShowNonPublicToot,
                onCheckedChange = callbacks.onDontShowNonPublicTootChanged,
            )
            SettingCheckbox(
                text = stringResource(R.string.instance_local),
                checked = uiState.instanceLocal,
                visible = uiState.showInstanceLocal,
                onCheckedChange = callbacks.onInstanceLocalChanged,
            )
            SettingCheckbox(
                text = stringResource(R.string.dont_use_streaming_api),
                checked = uiState.dontStreaming,
                visible = uiState.showDontStreaming,
                onCheckedChange = callbacks.onDontStreamingChanged,
            )
            SettingCheckbox(
                text = stringResource(R.string.dont_refresh_on_activity_resume),
                checked = uiState.dontAutoRefresh,
                visible = uiState.showDontAutoRefresh,
                onCheckedChange = callbacks.onDontAutoRefreshChanged,
            )
            SettingCheckbox(
                text = stringResource(R.string.hide_media_default),
                checked = uiState.hideMediaDefault,
                visible = uiState.showHideMediaDefault,
                onCheckedChange = callbacks.onHideMediaDefaultChanged,
            )
            SettingCheckbox(
                text = stringResource(R.string.with_attachment),
                checked = uiState.withAttachment,
                visible = uiState.showWithAttachment,
                onCheckedChange = callbacks.onWithAttachmentChanged,
            )
            SettingCheckbox(
                text = stringResource(R.string.system_notification_not_related),
                checked = uiState.systemNotificationNotRelated,
                visible = uiState.showSystemNotificationNotRelated,
                onCheckedChange = callbacks.onSystemNotificationNotRelatedChanged,
            )
            SettingCheckbox(
                text = stringResource(R.string.enable_speech),
                checked = uiState.enableSpeech,
                visible = uiState.showEnableSpeech,
                onCheckedChange = callbacks.onEnableSpeechChanged,
            )
            SettingCheckbox(
                text = stringResource(R.string.use_old_api),
                checked = uiState.oldApi,
                visible = uiState.showOldApi,
                onCheckedChange = callbacks.onOldApiChanged,
            )

            // Regex filter
            if (uiState.showRegexFilter) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.regex_filter),
                        color = Color(uiState.headerPageNumberColor),
                    )
                    if (uiState.regexFilterError.isNotEmpty()) {
                        Text(
                            text = uiState.regexFilterError,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }

                var localRegex by remember(uiState.regexFilterText) {
                    mutableStateOf(uiState.regexFilterText)
                }
                TextField(
                    value = localRegex,
                    onValueChange = {
                        localRegex = it
                        callbacks.onRegexFilterChanged(it)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                )
            }

            // Action buttons
            if (uiState.showDeleteNotification) {
                Button(
                    onClick = callbacks.onDeleteNotification,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.notification_delete))
                }
            }

            Button(
                onClick = callbacks.onColorAndBackground,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.color_and_background))
            }

            if (uiState.showLanguageFilter) {
                Button(
                    onClick = callbacks.onLanguageFilter,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.language_filter))
                }
            }
        }
    }
}

@Composable
private fun SettingCheckbox(
    text: String,
    checked: Boolean,
    visible: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    if (!visible) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
        Text(text = text)
    }
}

@Composable
private fun HashtagExtraFields(
    uiState: ColumnUiState,
    callbacks: ColumnCallbacks,
) {
    val headerColor = Color(uiState.headerPageNumberColor)

    Text(
        text = stringResource(R.string.hashtag_extra_any),
        color = headerColor,
    )
    var localAny by remember(uiState.hashtagExtraAny) { mutableStateOf(uiState.hashtagExtraAny) }
    TextField(
        value = localAny,
        onValueChange = {
            localAny = it
            callbacks.onHashtagExtraAnyChanged(it)
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    Text(
        text = stringResource(R.string.hashtag_extra_all),
        color = headerColor,
    )
    var localAll by remember(uiState.hashtagExtraAll) { mutableStateOf(uiState.hashtagExtraAll) }
    TextField(
        value = localAll,
        onValueChange = {
            localAll = it
            callbacks.onHashtagExtraAllChanged(it)
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    Text(
        text = stringResource(R.string.hashtag_extra_none),
        color = headerColor,
    )
    var localNone by remember(uiState.hashtagExtraNone) { mutableStateOf(uiState.hashtagExtraNone) }
    TextField(
        value = localNone,
        onValueChange = {
            localNone = it
            callbacks.onHashtagExtraNoneChanged(it)
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
