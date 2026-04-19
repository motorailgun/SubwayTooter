package es.ariaontheplanet.quasar.actmain

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.ariaontheplanet.quasar.R
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MainFooter(
    viewModel: MainViewModel,
    onClickMenu: () -> Unit,
    onClickToot: () -> Unit,
    onLongClickToot: () -> Unit,
    onClickColumn: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val columnList by viewModel.columnList.collectAsState()
    val visibleRange by viewModel.visibleRange.collectAsState()
    val scrollRequest by viewModel.scrollToColumn.collectAsState(initial = null)
    
    val listState = rememberLazyListState()

    // Handle scroll requests
    LaunchedEffect(scrollRequest) {
        scrollRequest?.let { index ->
            // Scroll so the item is centered or visible
            // The original logic centers it.
            // LazyListState.animateScrollToItem centers if we calculate offset? 
            // Default behavior brings it into view.
            try {
                listState.animateScrollToItem(index)
            } catch(e: Exception) {
                // ignore
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp) // Fixed height matching original
            .background(MaterialTheme.colorScheme.surfaceContainer),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Menu Button
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onClickMenu),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_hamburger),
                contentDescription = stringResource(id = R.string.menu),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
            )
        }

        // Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        )

        // Column Strip
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(columnList) { index, column ->
                    ColumnIcon(
                        item = column,
                        onClick = { onClickColumn(index) }
                    )
                }
            }
            
            // Indicator Overlay
            // We need to draw the indicator based on visibleRange (from ViewPager/RecyclerView)
            // AND map it to the positions in the LazyRow.
            // However, the LazyRow items move.
            // If the user scrolls the strip, the indicator should move with the items.
            // But visibleRange is about WHICH columns are selected.
            // So if columns 0-1 are selected, we draw a box over icons 0-1.
            
            // Implementation: We can draw the indicator inside the LazyRow items themselves?
            // No, the indicator slides across items.
            // Or we can use Modifier.drawWithContent on the LazyRow.
            
            ColumnStripIndicator(
                listState = listState,
                visibleRange = visibleRange,
                columnCount = columnList.size
            )
        }

        // Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        )

        // Toot Button
        Box(
            modifier = Modifier
                .size(48.dp)
                // TODO: Handle long click
                .clickable(onClick = onClickToot),
            contentAlignment = Alignment.Center
        ) {
             Image(
                painter = painterResource(id = R.drawable.ic_edit),
                contentDescription = stringResource(id = R.string.toot),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
            )
        }
    }
}

@Composable
fun ColumnIcon(
    item: MainViewModel.ColumnUiState,
    onClick: () -> Unit
) {
    // 1.5 aspect ratio approx?
    // Original: rootW = iconSize * 1.25, rootH = iconSize * 1.5
    // iconSize is typically 24dp or similar?
    // Let's use fixed size or weight?
    // Original uses fixed size calculated from pref.
    
    // For now, let's use a reasonable size.
    // 48dp height.
    val width = 40.dp
    
    Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .background(Color(item.headerBackgroundColor)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = item.iconId),
            contentDescription = item.contentDescription,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(Color(item.headerNameColor))
        )
        
        if (item.acctColor != 0) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(4.dp)
                    .background(Color(item.acctColor))
            )
        }
    }
}

@Composable
fun ColumnStripIndicator(
    listState: androidx.compose.foundation.lazy.LazyListState,
    visibleRange: MainViewModel.VisibleRange,
    columnCount: Int
) {
    val indicatorColor = MaterialTheme.colorScheme.primary // Or customizable
    val indicatorHeight = 4.dp

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val firstIndex = visibleRange.first
        val lastIndex = visibleRange.last
        val slideRatio = visibleRange.slideRatio

        if (firstIndex < 0 || lastIndex >= columnCount) return@Canvas

        val visibleItems = listState.layoutInfo.visibleItemsInfo
        val firstItem = visibleItems.find { it.index == firstIndex }
        val lastItem = visibleItems.find { it.index == lastIndex }

        if (firstItem == null && lastItem == null) return@Canvas

        // Fallback if one end is not visible (e.g. huge range)
        val startX = firstItem?.offset ?: 0
        val endX = (lastItem?.offset ?: startX) + (lastItem?.size ?: 0)
        
        val itemWidth = firstItem?.size ?: 0
        val slideOffset = (itemWidth * slideRatio).toInt()

        val rectLeft = startX + slideOffset
        val rectRight = endX + slideOffset

        if (rectRight > rectLeft) {
            drawRect(
                color = indicatorColor,
                topLeft = androidx.compose.ui.geometry.Offset(rectLeft.toFloat(), 0f),
                size = androidx.compose.ui.geometry.Size((rectRight - rectLeft).toFloat(), indicatorHeight.toPx())
            )
        }
    }
}
