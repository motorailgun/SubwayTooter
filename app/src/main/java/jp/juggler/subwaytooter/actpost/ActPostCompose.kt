package jp.juggler.subwaytooter.actpost

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jp.juggler.subwaytooter.ActPost
import jp.juggler.subwaytooter.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActPostScreen(activity: ActPost) {
    val views = activity.views
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(scrollState)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Account Selection
            LazyRow(modifier = Modifier.fillMaxWidth()) {
                items(activity.accountList) { acct ->
                    // Just a placeholder for now
                    Button(onClick = { 
                        activity.account = acct
                        // activity.onAccountChanged()
                    }) {
                        Text(acct.acct.pretty)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content Warning
            if (activity.contentWarningChecked) {
                OutlinedTextField(
                    value = activity.etContentWarning.fieldValue,
                    onValueChange = { activity.etContentWarning.fieldValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.content_warning)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Content
            OutlinedTextField(
                value = activity.etContent.fieldValue,
                onValueChange = { activity.etContent.fieldValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                label = { Text(stringResource(R.string.post)) }
            )

            // Attachment icons/thumbnails would go here.
            // Poll would go here.
            
            // For now, let's keep it simple to ensure it compiles.
        }
    }
}
