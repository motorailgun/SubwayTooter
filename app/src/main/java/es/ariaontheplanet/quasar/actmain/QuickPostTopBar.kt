package es.ariaontheplanet.quasar.actmain

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.ariaontheplanet.quasar.App1
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.compose.NetworkImage

/**
 * メイン画面上部の Material3 CenterAlignedTopAppBar。
 *
 * - navigationIcon: ハンバーガー（drawer を開く）
 * - title: タップで QuickPostSheet を開くプレースホルダー
 * - actions: 右端に円形のアカウントアバター（タップでアカウント設定へ）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPostTopBar(
    onClickHamburger: () -> Unit,
    onClickPostPlaceholder: () -> Unit,
    onClickAccountIcon: () -> Unit,
) {
    val context = LocalContext.current
    val appState = App1.getAppState(context)
    val currentAccount by appState.currentAccount.collectAsState()
    val avatarUrl = currentAccount?.loginAccount?.avatar_static

    CenterAlignedTopAppBar(
        navigationIcon = {
            IconButton(onClick = onClickHamburger) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = stringResource(R.string.menu),
                )
            }
        },
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onClickPostPlaceholder)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = stringResource(R.string.toot),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        actions = {
            val avatarSize = 36.dp
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(avatarSize)
                    .clip(CircleShape)
                    .clickable(onClick = onClickAccountIcon),
                contentAlignment = Alignment.Center,
            ) {
                if (!avatarUrl.isNullOrEmpty()) {
                    NetworkImage(
                        modifier = Modifier
                            .size(avatarSize)
                            .clip(CircleShape),
                        staticUrl = avatarUrl,
                        animatedUrl = currentAccount?.loginAccount?.avatar,
                        contentDescription = stringResource(R.string.account),
                    )
                } else {
                    Icon(
                        painter = rememberVectorPainter(Icons.Filled.Person),
                        contentDescription = stringResource(R.string.account),
                    )
                }
            }
        },
    )
}
