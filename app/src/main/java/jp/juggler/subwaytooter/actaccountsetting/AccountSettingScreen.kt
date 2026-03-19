package jp.juggler.subwaytooter.actaccountsetting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.table.SavedAccount

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

@Composable
fun AccountSettingContent(
    account: SavedAccount,
    viewModel: AccountSettingViewModel
) {
    // Collect revision to trigger recomposition when mutable properties change
    val revision by viewModel.revision.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
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
