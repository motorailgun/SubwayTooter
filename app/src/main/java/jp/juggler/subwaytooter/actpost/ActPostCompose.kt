package jp.juggler.subwaytooter.actpost

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import jp.juggler.subwaytooter.ActPost
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.compose.NetworkImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActPostScreen(
    activity: ActPost,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.post)) },
                navigationIcon = {
                    IconButton(onClick = { activity.finish() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { activity.performPost() }) {
                        Text(stringResource(R.string.post))
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(scrollState)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Account Selection
            AccountSelector(activity)

            Spacer(modifier = Modifier.height(16.dp))
            
            // Reply / Quote Info
            if (activity.showReplySection) {
                 Text(
                     text = stringResource(R.string.reply_to_x, activity.replyToText),
                     style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant
                 )
                 Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (activity.showQuoteOption) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = activity.quoteChecked,
                        onCheckedChange = { activity.quoteChecked = it }
                    )
                    Text(stringResource(R.string.quote_renote))
                }
            }

            // Content Warning
            if (activity.contentWarningChecked) {
                PostTextField(
                    state = activity.etContentWarning,
                    label = stringResource(R.string.content_warning),
                    onFocusChanged = { if (it.isFocused) activity.focusedEditField = 1 },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Main Content
            PostTextField(
                state = activity.etContent,
                label = stringResource(R.string.post),
                onFocusChanged = { if (it.isFocused) activity.focusedEditField = 0 },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Attachments
            if (activity.showAttachmentSection) {
                AttachmentGrid(activity)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Poll
            if (activity.pollTypeIndex > 0) {
                PollSection(activity)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun AccountSelector(activity: ActPost) {
    OutlinedButton(
        onClick = { activity.performAccountChooser() },
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            val avatarUrl = activity.accountAvatarStaticUrl
            if (avatarUrl != null) {
                NetworkImage(
                    url = avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Text(
                text = activity.accountButtonText,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun PostTextField(
    state: TextEditState,
    label: String,
    onFocusChanged: (androidx.compose.ui.focus.FocusState) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = state.fieldValue,
        onValueChange = { state.fieldValue = it },
        modifier = modifier.onFocusChanged(onFocusChanged),
        label = { Text(label) }
    )
}

@Composable
fun AttachmentGrid(activity: ActPost) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        activity.attachmentSlots.forEachIndexed { index, slot ->
            if (slot.visible) {
                 AttachmentSlot(
                     slot = slot,
                     onClick = { activity.performAttachmentClick(index) },
                     modifier = Modifier.weight(1f)
                 )
            }
        }
    }
}

@Composable
fun AttachmentSlot(
    slot: AttachmentSlotUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (slot.previewUrl != null) {
            NetworkImage(
                url = slot.previewUrl,
                contentDescription = "Attachment",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
             // Fallback icon
             // Note: In a real app we'd load the resource ID, but Compose resources 
             // handling for generic IDs can be tricky without a wrapper.
             // Assuming slot.fallbackIconRes is valid
             // Using a placeholder Icon for now as we might not have easy access to arbitrary drawable resources in Compose 
             // without Context or similar if not using painterResource(id)
             // We can use painterResource with the ID.
             Icon(
                 painter = androidx.compose.ui.res.painterResource(id = slot.fallbackIconRes),
                 contentDescription = null,
                 tint = MaterialTheme.colorScheme.onSurfaceVariant
             )
        }
    }
}

@Composable
fun PollSection(activity: ActPost) {
    Column {
        Text(stringResource(R.string.vote), style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        
        activity.etChoices.forEachIndexed { index, state ->
            PostTextField(
                state = state,
                label = stringResource(R.string.choice_n, index + 1),
                onFocusChanged = { if (it.isFocused) activity.focusedEditField = 2 + index },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        // Expiration
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.expire_after))
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = activity.etExpireDays.fieldValue,
                onValueChange = { activity.etExpireDays.fieldValue = it },
                label = { Text(stringResource(R.string.days)) },
                modifier = Modifier.width(80.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                 value = activity.etExpireHours.fieldValue,
                 onValueChange = { activity.etExpireHours.fieldValue = it },
                 label = { Text(stringResource(R.string.hours)) },
                 modifier = Modifier.width(80.dp),
                 keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                 value = activity.etExpireMinutes.fieldValue,
                 onValueChange = { activity.etExpireMinutes.fieldValue = it },
                 label = { Text(stringResource(R.string.minutes)) },
                 modifier = Modifier.width(80.dp),
                 keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}

