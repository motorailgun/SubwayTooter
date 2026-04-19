package es.ariaontheplanet.quasar.emoji

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.compose.NetworkImage
import es.ariaontheplanet.quasar.pref.PrefS
import es.ariaontheplanet.quasar.table.SavedAccount

@Composable
fun EmojiPickerScreen(
    viewModel: EmojiPickerViewModel,
    accessInfo: SavedAccount?,
    onPicked: (PickerItem) -> Unit,
) {
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val items by viewModel.items.collectAsState()
    val selectedSkinTone by viewModel.selectedSkinTone.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
    ) {
        // Category Tabs
        ScrollableTabRow(
            selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
            edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            categories.forEach { category ->
                Tab(
                    selected = category == selectedCategory,
                    onClick = { viewModel.selectCategory(category) },
                    text = { Text(stringResource(category.titleId)) }
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            placeholder = { Text("Search emoji") },
            singleLine = true,
            leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) }
        )
        
        // Skin Tone Selector (only for Unicode categories)
        if (selectedCategory != EmojiCategory.Recent && selectedCategory != EmojiCategory.Custom) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EmojiPickerViewModel.skinTones.forEach { tone ->
                    val isSelected = tone == selectedSkinTone
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                shape = MaterialTheme.shapes.small
                            )
                            .clickable { viewModel.selectSkinTone(tone) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = tone.code, fontSize = 20.sp)
                    }
                }
            }
        }

        // Emoji Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 48.dp),
            contentPadding = PaddingValues(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(items, key = { it.key }) { item ->
                EmojiItem(item = item, onClick = { 
                    viewModel.onPicked(item)
                    onPicked(item)
                })
            }
        }
    }
}

@Composable
fun EmojiItem(
    item: PickerItem,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when (item) {
            is PickerItemUnicode -> {
                Text(
                    text = item.emoji.unifiedCode,
                    fontSize = 24.sp
                )
            }
            is PickerItemCustom -> {
                NetworkImage(
                    staticUrl = item.customEmoji.url,
                    modifier = Modifier.size(32.dp),
                    contentDescription = item.customEmoji.shortcode
                )
            }
        }
    }
}
