package jp.juggler.subwaytooter.compose

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.column.Column

/**
 * Compose replacement for inflateColumnBody().
 * Contains background image, loading/error states, timeline content, and refresh error overlay.
 */
@Composable
fun ColumnBody(
    activity: ActMain,
    column: Column,
    uiState: ColumnUiState,
    timelineState: TimelineState,
    timelineCallbacks: TimelineCallbacks,
    columnCallbacks: ColumnCallbacks,
    bSimpleList: Boolean,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val rev = uiState.uiRevision // read for recomposition

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(uiState.columnBgColor)),
    ) {
        // Background image
        uiState.columnBgImageBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(uiState.columnBgImageAlpha),
            )
        }

        // Loading/Error state
        if (uiState.showLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        // Simple fling detection for reload on error screen
                        detectVerticalDragGestures { _, _ -> }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                Text(
                    text = uiState.loadingMessage,
                    color = Color(uiState.contentColor),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )

                if (uiState.showConfirmMail) {
                    Button(onClick = columnCallbacks.onConfirmMail) {
                        Text(stringResource(R.string.resend_confirm_mail))
                    }
                }
            }
        }

        // Timeline content
        if (uiState.showRefreshLayout) {
            TimelineColumn(
                activity = activity,
                column = column,
                timelineState = timelineState,
                bSimpleList = bSimpleList,
                callbacks = timelineCallbacks,
                lazyListState = lazyListState,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Refresh error overlay (bottom)
        AnimatedVisibility(
            visible = uiState.refreshErrorVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                    )
                    .clickable { columnCallbacks.onRefreshErrorClick() }
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_error),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp),
                )
                Text(
                    text = uiState.refreshErrorText,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    maxLines = if (uiState.refreshErrorSingleLine) 1 else Int.MAX_VALUE,
                    overflow = if (uiState.refreshErrorSingleLine) TextOverflow.Ellipsis else TextOverflow.Clip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp),
                )
            }
        }
    }
}
