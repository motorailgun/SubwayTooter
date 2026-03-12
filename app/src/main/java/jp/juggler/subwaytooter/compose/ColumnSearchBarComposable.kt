package jp.juggler.subwaytooter.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.juggler.subwaytooter.R

/**
 * Compose replacement for inflateSearchBar() in ColumnViewHolder.
 * Shows search input, emoji query flex, clear/search buttons, resolve checkbox.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnSearchBar(
    uiState: ColumnUiState,
    callbacks: ColumnCallbacks,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = uiState.searchBarVisible) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(Color(uiState.searchFormBgColor))
                .padding(horizontal = 12.dp, vertical = 3.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (uiState.showSearchInput) {
                    // Search text input
                    var localQuery by remember(uiState.searchQuery) {
                        mutableStateOf(uiState.searchQuery)
                    }
                    TextField(
                        value = localQuery,
                        onValueChange = { localQuery = it },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Search,
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = { callbacks.onSearchExecute(localQuery, uiState.searchResolve) },
                        ),
                    )
                }

                if (uiState.showEmojiQueryMode) {
                    // Emoji query items (FlowRow)
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        uiState.emojiQueryItems.forEachIndexed { index, item ->
                            Button(
                                onClick = {},
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color(uiState.contentColor),
                                ),
                            ) {
                                Text(text = item.displayText.toString())
                            }
                        }
                    }
                }

                // Emoji add button
                if (uiState.showEmojiQueryMode) {
                    IconButton(onClick = callbacks.onEmojiAdd) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = stringResource(R.string.add),
                            tint = Color(uiState.contentColor),
                        )
                    }
                }

                // Clear button
                if (uiState.showSearchClear) {
                    IconButton(onClick = callbacks.onSearchClear) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.clear),
                            tint = Color(uiState.contentColor),
                        )
                    }
                }

                // Search button
                if (uiState.showSearchInput) {
                    IconButton(
                        onClick = {
                            callbacks.onSearchExecute(
                                uiState.searchQuery,
                                uiState.searchResolve,
                            )
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = stringResource(R.string.search),
                            tint = Color(uiState.contentColor),
                        )
                    }
                }
            }

            // Resolve checkbox
            if (uiState.showResolveCheckbox) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = uiState.searchResolve,
                        onCheckedChange = callbacks.onSearchResolveChanged,
                    )
                    Text(text = stringResource(R.string.resolve_non_local_account))
                }
            }

            // Emoji description
            if (uiState.showEmojiQueryMode) {
                Text(
                    text = stringResource(R.string.long_tap_to_delete),
                    color = Color(uiState.headerPageNumberColor),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

/**
 * Compose replacement for inflateAggBoostBar() in ColumnViewHolder.
 */
@Composable
fun ColumnAggBoostBar(
    uiState: ColumnUiState,
    callbacks: ColumnCallbacks,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = uiState.aggBoostBarVisible) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(Color(uiState.searchFormBgColor))
                .padding(horizontal = 12.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.agg_status_limit),
                color = Color(uiState.contentColor),
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
            )

            var localLimit by remember(uiState.statusLoadLimit) {
                mutableStateOf(uiState.statusLoadLimit)
            }
            TextField(
                value = localLimit,
                onValueChange = { localLimit = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        localLimit.toIntOrNull()?.takeIf { it > 0 }?.let {
                            callbacks.onAggStart(it)
                        }
                    },
                ),
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            IconButton(onClick = {
                localLimit.toIntOrNull()?.takeIf { it > 0 }?.let {
                    callbacks.onAggStart(it)
                }
            }) {
                Icon(
                    painter = painterResource(R.drawable.baseline_start_24),
                    contentDescription = stringResource(R.string.search),
                    tint = Color(uiState.contentColor),
                )
            }
        }
    }
}

/**
 * Compose replacement for inflateListBar() in ColumnViewHolder.
 */
@Composable
fun ColumnListBar(
    uiState: ColumnUiState,
    callbacks: ColumnCallbacks,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = uiState.listBarVisible) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(Color(uiState.searchFormBgColor))
                .padding(horizontal = 12.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            var localName by remember(uiState.listName) { mutableStateOf(uiState.listName) }
            TextField(
                value = localName,
                onValueChange = { localName = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.list_create_hint)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        val name = localName.trim()
                        if (name.isNotEmpty()) callbacks.onListAdd(name)
                    },
                ),
                modifier = Modifier.weight(1f),
            )

            IconButton(onClick = {
                val name = localName.trim()
                if (name.isNotEmpty()) callbacks.onListAdd(name)
            }) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.add),
                    tint = Color(uiState.contentColor),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewColumnSearchBar() {
    ColumnSearchBar(
        uiState = ColumnUiState().apply { searchBarVisible = true },
        callbacks = ColumnCallbacks()
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewColumnAggBoostBar() {
    ColumnAggBoostBar(
        uiState = ColumnUiState().apply { aggBoostBarVisible = true },
        callbacks = ColumnCallbacks()
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewColumnListBar() {
    ColumnListBar(
        uiState = ColumnUiState().apply { listBarVisible = true },
        callbacks = ColumnCallbacks()
    )
}

