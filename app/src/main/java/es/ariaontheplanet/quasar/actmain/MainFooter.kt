package es.ariaontheplanet.quasar.actmain

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.column.ColumnType

/**
 * The bottom navigation bar for ActMain. Shows one item per entry in
 * [fixedColumnTypes]; tapping an item scrolls the pager/LazyRow to that column,
 * swiping the pager updates which item is highlighted.
 */
@Composable
fun MainFooter(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val currentPage by viewModel.currentPage.collectAsState()

    NavigationBar(modifier = modifier) {
        fixedColumnTypes.forEachIndexed { index, type ->
            val (iconRes, labelRes) = iconAndLabelOf(type)
            NavigationBarItem(
                selected = currentPage == index,
                onClick = { viewModel.requestScrollToColumn(index) },
                icon = {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = stringResource(id = labelRes),
                    )
                },
                label = { Text(stringResource(id = labelRes)) },
            )
        }
    }
}

private fun iconAndLabelOf(type: ColumnType): Pair<Int, Int> = when (type) {
    ColumnType.HOME -> R.drawable.ic_home to R.string.home
    ColumnType.NOTIFICATIONS -> R.drawable.ic_announcement to R.string.notifications
    ColumnType.LOCAL -> R.drawable.ic_run to R.string.local_timeline
    ColumnType.FEDERATE -> R.drawable.ic_bike to R.string.federate_timeline
    else -> R.drawable.ic_home to R.string.home
}
