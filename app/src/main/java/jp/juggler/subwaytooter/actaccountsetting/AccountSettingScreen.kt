package jp.juggler.subwaytooter.actaccountsetting

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.api.entity.TootVisibility
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.subwaytooter.view.MyNetworkImageView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingScreen(
    viewModel: AccountSettingViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    MaterialTheme {
        val account by viewModel.account.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        // val error by viewModel.error.collectAsState()

        Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account_setting)) },
                navigationIcon = {
                    // Back button logic if needed, usually handled by activity or nav controller
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                val acct = account
                if (acct != null) {
                    AccountSettingContent(acct, viewModel)
                } else {
                    Text("Account not found", modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingContent(
    account: SavedAccount,
    viewModel: AccountSettingViewModel
) {
    // Collect revision to trigger recomposition when mutable properties change
    val revision by viewModel.revision.collectAsState()
    
    val editingDisplayName by viewModel.editingDisplayName.collectAsState()
    val editingNote by viewModel.editingNote.collectAsState()
    val editingLocked by viewModel.editingLocked.collectAsState()
    val avatarUri by viewModel.avatarUri.collectAsState()
    val headerUri by viewModel.headerUri.collectAsState()
    val tootAccount by viewModel.tootAccount.collectAsState()
    val editingVisibility by viewModel.editingVisibility.collectAsState()
    val editingDefaultSensitive by viewModel.editingDefaultSensitive.collectAsState()

    val launcherAvatar = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        viewModel.setAvatar(uri)
    }
    val launcherHeader = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        viewModel.setHeader(uri)
    }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.accountDeleted.collect {
             (context as? Activity)?.finish()
        }
    }
    
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to delete this account? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Profile Edit",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Header Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color.Gray)
                .clickable { launcherHeader.launch("image/*") }
        ) {
             if (headerUri != null) {
                 AndroidView(
                     modifier = Modifier.fillMaxSize(),
                     factory = { ctx -> 
                         android.widget.ImageView(ctx).apply {
                             scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                             setImageURI(headerUri) 
                         }
                     },
                     update = { view -> view.setImageURI(headerUri) }
                 )
             } else {
                 val url = tootAccount?.header ?: account.loginAccount?.header
                 if (url != null) {
                      AndroidView(
                          modifier = Modifier.fillMaxSize(),
                          factory = { ctx -> 
                             MyNetworkImageView(ctx).apply { 
                                 scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                                 setImageUrl(0f, url) 
                             }
                          },
                          update = { view -> (view as? MyNetworkImageView)?.setImageUrl(0f, url) }
                      )
                 }
             }
             Text("Tap to change Header", color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        // Avatar Image
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
                .align(Alignment.CenterHorizontally)
                .clickable { launcherAvatar.launch("image/*") }
        ) {
             if (avatarUri != null) {
                 AndroidView(
                     modifier = Modifier.fillMaxSize(),
                     factory = { ctx -> 
                         android.widget.ImageView(ctx).apply {
                             scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                             setImageURI(avatarUri) 
                         }
                     },
                     update = { view -> view.setImageURI(avatarUri) }
                 )
             } else {
                 val url = tootAccount?.avatar ?: account.loginAccount?.avatar
                 if (url != null) {
                      AndroidView(
                          modifier = Modifier.fillMaxSize(),
                          factory = { ctx -> 
                             MyNetworkImageView(ctx).apply { 
                                 scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                                 setImageUrl(0.5f, url) 
                             }
                          },
                          update = { view -> (view as? MyNetworkImageView)?.setImageUrl(0.5f, url) }
                      )
                 }
             }
        }
        Text("Tap to change Avatar", style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally))
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = editingDisplayName,
            onValueChange = { viewModel.setDisplayName(it) },
            label = { Text("Display Name") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = editingNote,
            onValueChange = { viewModel.setNote(it) },
            label = { Text("Bio") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        
        SettingSwitch(
            label = "Locked Account",
            checked = editingLocked,
            onCheckedChange = { viewModel.setLocked(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))
        
        // Visibility
        var expandedVisibility by remember { mutableStateOf(false) }
        val visibilityOptions = listOf(TootVisibility.Public, TootVisibility.UnlistedHome, TootVisibility.PrivateFollowers)
        
        ExposedDropdownMenuBox(
            expanded = expandedVisibility,
            onExpandedChange = { expandedVisibility = !expandedVisibility },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                readOnly = true,
                value = when (editingVisibility) {
                    TootVisibility.Public -> stringResource(R.string.visibility_public)
                    TootVisibility.UnlistedHome -> stringResource(R.string.visibility_unlisted)
                    TootVisibility.PrivateFollowers -> stringResource(R.string.visibility_private)
                    else -> editingVisibility.strMastodon
                },
                onValueChange = {},
                label = { Text("Default Privacy") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVisibility) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            )
            ExposedDropdownMenu(
                expanded = expandedVisibility,
                onDismissRequest = { expandedVisibility = false },
            ) {
                visibilityOptions.forEach { visibility ->
                    DropdownMenuItem(
                        text = { 
                            Text(when (visibility) {
                                TootVisibility.Public -> stringResource(R.string.visibility_public)
                                TootVisibility.UnlistedHome -> stringResource(R.string.visibility_unlisted)
                                TootVisibility.PrivateFollowers -> stringResource(R.string.visibility_private)
                                else -> visibility.strMastodon
                            })
                        },
                        onClick = {
                            viewModel.setVisibility(visibility)
                            expandedVisibility = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        SettingSwitch(
            label = "Mark media as sensitive (Server Default)",
            checked = editingDefaultSensitive,
            onCheckedChange = { viewModel.setDefaultSensitive(it) }
        )
        
        Button(
            onClick = { viewModel.confirmProfile() },
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) {
            Text("Save Profile Changes")
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            text = "Account: ${account.acct.pretty}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        SettingSectionHeader(stringResource(R.string.pull_notification_use))
        SettingSwitch(
            label = stringResource(R.string.pull_notification_use),
            checked = account.notificationPullEnable,
            onCheckedChange = { viewModel.updateNotificationPull(it) }
        )

        SettingSectionHeader(stringResource(R.string.push_notification_use))
        SettingSwitch(
            label = stringResource(R.string.push_notification_use),
            checked = account.notificationPushEnable,
            onCheckedChange = { viewModel.updateNotificationPush(it) }
        )

        SettingSectionHeader("Behavior") // TODO: Find resource
        SettingSwitch(
            label = stringResource(R.string.mark_sensitive_by_default),
            checked = account.defaultSensitive,
            onCheckedChange = { viewModel.updateDefaultSensitive(it) }
        )

        SettingSwitch(
            label = stringResource(R.string.sensitive_content_default_open),
            checked = account.dontHideNsfw,
            onCheckedChange = { viewModel.updateDontHideNsfw(it) }
        )

        SettingSwitch(
            label = stringResource(R.string.cw_default_open),
            checked = account.expandCw,
            onCheckedChange = { viewModel.updateExpandCw(it) }
        )

        SettingSectionHeader(stringResource(R.string.notifications))
        
        SettingCheckbox(
            label = stringResource(R.string.mention2),
            checked = account.notificationMention,
            onCheckedChange = { viewModel.updateNotificationMention(it) }
        )
        SettingCheckbox(
            label = stringResource(R.string.boost),
            checked = account.notificationBoost,
            onCheckedChange = { viewModel.updateNotificationBoost(it) }
        )
        SettingCheckbox(
            label = stringResource(R.string.favourite),
            checked = account.notificationFavourite,
            onCheckedChange = { viewModel.updateNotificationFavourite(it) }
        )
        SettingCheckbox(
            label = stringResource(R.string.follow),
            checked = account.notificationFollow,
            onCheckedChange = { viewModel.updateNotificationFollow(it) }
        )
        SettingCheckbox(
            label = stringResource(R.string.follow_request),
            checked = account.notificationFollowRequest,
            onCheckedChange = { viewModel.updateNotificationFollowRequest(it) }
        )
        SettingCheckbox(
            label = stringResource(R.string.reaction),
            checked = account.notificationReaction,
            onCheckedChange = { viewModel.updateNotificationReaction(it) }
        )
        SettingCheckbox(
            label = stringResource(R.string.vote_polls),
            checked = account.notificationVote,
            onCheckedChange = { viewModel.updateNotificationVote(it) }
        )
        SettingCheckbox(
            label = stringResource(R.string.notification_type_post),
            checked = account.notificationPost,
            onCheckedChange = { viewModel.updateNotificationPost(it) }
        )
        SettingCheckbox(
            label = stringResource(R.string.notification_type_update),
            checked = account.notificationUpdate,
            onCheckedChange = { viewModel.updateNotificationUpdate(it) }
        )
        SettingCheckbox(
            label = stringResource(R.string.notification_type_status_reference_fedibird),
            checked = account.notificationStatusReference,
            onCheckedChange = { viewModel.updateNotificationStatusReference(it) }
        )
        SettingCheckbox(
            label = stringResource(R.string.notification_type_severed_relationships),
            checked = account.notificationSeveredRelationships,
            onCheckedChange = { viewModel.updateNotificationSeveredRelationships(it) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Delete Account")
        }
    }
}

@Composable
fun SettingSectionHeader(title: String) {
    Column {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

@Composable
fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp)
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp)
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}
