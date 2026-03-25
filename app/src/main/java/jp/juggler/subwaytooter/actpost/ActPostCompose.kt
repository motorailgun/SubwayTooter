package jp.juggler.subwaytooter.actpost

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import jp.juggler.subwaytooter.actpost.PostViewModel
import jp.juggler.subwaytooter.actpost.PostCompletionLogic
import jp.juggler.subwaytooter.compose.NetworkImage
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
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(
                modifier = Modifier
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
        
        SuggestionList(activity.viewModel, Modifier.align(Alignment.BottomCenter))
    }
    }
}

@Composable
fun SuggestionList(
    viewModel: PostViewModel,
    modifier: Modifier = Modifier
) {
    val result by viewModel.completionResult
    
    if (result is PostCompletionLogic.Result.None) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp)
            .padding(8.dp), // Add padding for floating effect
        shadowElevation = 8.dp,
        tonalElevation = 8.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        LazyColumn {
            when (val r = result) {
                is PostCompletionLogic.Result.Mentions -> {
                    items(r.list) { item ->
                        SuggestionItem(text = item) {
                            applyCompletion(viewModel, r.range, item)
                        }
                    }
                }
                is PostCompletionLogic.Result.Hashtags -> {
                    items(r.list) { item ->
                        SuggestionItem(text = "#$item") {
                            applyCompletion(viewModel, r.range, "#$item")
                        }
                    }
                }
                is PostCompletionLogic.Result.Emojis -> {
                    items(r.list) { item ->
                        SuggestionItem(text = ":${item.shortcode}:", iconUrl = item.url, staticUrl = item.staticUrl) {
                            applyCompletion(viewModel, r.range, ":${item.shortcode}:")
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun SuggestionItem(text: String, iconUrl: String? = null, staticUrl: String? = null, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(text) },
        leadingContent = if (iconUrl != null) {
            { 
                NetworkImage(
                    animatedUrl = iconUrl,
                    staticUrl = staticUrl ?: iconUrl,
                    contentDescription = text,
                    modifier = Modifier.size(24.dp)
                ) 
            }
        } else null,
        modifier = Modifier.clickable(onClick = onClick)
    )
}

fun applyCompletion(viewModel: PostViewModel, range: IntRange, text: String) {
    val et = viewModel.etContent
    val src = et.fieldValue.text
    
    // Check if we need to add space
    val suffix = " "
    val replacement = "$text$suffix"
    
    // Replace logic: replace range with replacement
    // range is start..end-1
    // range.start is the position of @, #, or : (or start of word)
    // range.endInclusive is the position of cursor - 1 (last typed char)
    
    // String.replaceRange takes (startIndex, endIndex, replacement)
    // range.endInclusive + 1 is the endIndex for replaceRange
    
    try {
        val newText = src.replaceRange(range.first, range.last + 1, replacement)
        val newCursor = range.first + replacement.length
        
        et.fieldValue = TextFieldValue(newText, TextRange(newCursor))
        viewModel.closeCompletion()
    } catch(ex: Throwable) {
        // Log error
    }
}
