package es.ariaontheplanet.quasar.compose

import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.ariaontheplanet.quasar.ActMain
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.api.entity.MisskeyAntenna
import es.ariaontheplanet.quasar.api.entity.TimelineItem
import es.ariaontheplanet.quasar.api.entity.TootAccountRef
import es.ariaontheplanet.quasar.api.entity.TootAggBoost
import es.ariaontheplanet.quasar.api.entity.TootConversationSummary
import es.ariaontheplanet.quasar.api.entity.TootDomainBlock
import es.ariaontheplanet.quasar.api.entity.TootFilter
import es.ariaontheplanet.quasar.api.entity.TootGap
import es.ariaontheplanet.quasar.api.entity.TootList
import es.ariaontheplanet.quasar.api.entity.TootMessageHolder
import es.ariaontheplanet.quasar.api.entity.TootNotification
import es.ariaontheplanet.quasar.api.entity.TootScheduled
import es.ariaontheplanet.quasar.api.entity.TootSearchGap
import es.ariaontheplanet.quasar.api.entity.TootStatus
import es.ariaontheplanet.quasar.api.entity.TootTag
import es.ariaontheplanet.quasar.column.Column
import es.ariaontheplanet.quasar.column.getAcctColor
import es.ariaontheplanet.quasar.column.getContentColor
import es.ariaontheplanet.quasar.table.SavedAccount
import jp.juggler.util.data.notEmpty

/**
 * Main dispatcher composable that renders a TimelineItem based on its type.
 */
@Composable
fun TimelineItemContent(
    activity: ActMain,
    column: Column,
    accessInfo: SavedAccount,
    item: TimelineItem,
    bSimpleList: Boolean,
    callbacks: TimelineCallbacks,
    modifier: Modifier = Modifier,
) {
    val contentColor = remember(column) { column.getContentColor() }
    val acctColor = remember(column) { column.getAcctColor() }

    Column(modifier = modifier.fillMaxWidth()) {
        when (item) {
            is TootStatus -> StatusItemContent(
                activity = activity,
                column = column,
                accessInfo = accessInfo,
                status = item,
                bSimpleList = bSimpleList,
                callbacks = callbacks,
                contentColor = contentColor,
                acctColor = acctColor,
            )

            is TootAggBoost -> AggBoostItemContent(
                activity = activity,
                column = column,
                accessInfo = accessInfo,
                item = item,
                bSimpleList = bSimpleList,
                callbacks = callbacks,
                contentColor = contentColor,
                acctColor = acctColor,
            )

            is TootAccountRef -> AccountItemContent(
                activity = activity,
                column = column,
                accessInfo = accessInfo,
                whoRef = item,
                callbacks = callbacks,
                contentColor = contentColor,
                acctColor = acctColor,
            )

            is TootNotification -> NotificationItemContent(
                activity = activity,
                column = column,
                accessInfo = accessInfo,
                notification = item,
                bSimpleList = bSimpleList,
                callbacks = callbacks,
                contentColor = contentColor,
                acctColor = acctColor,
            )

            is TootGap -> GapItemContent(
                column = column,
                item = item,
                callbacks = callbacks,
                contentColor = contentColor,
            )

            is TootSearchGap -> SearchGapItemContent(
                item = item,
                callbacks = callbacks,
                contentColor = contentColor,
            )

            is TootTag -> TagItemContent(
                activity = activity,
                item = item,
                callbacks = callbacks,
                contentColor = contentColor,
                acctColor = acctColor,
            )

            is TootDomainBlock -> DomainBlockItemContent(
                item = item,
                callbacks = callbacks,
                contentColor = contentColor,
            )

            is TootList -> ListItemContent(
                item = item,
                callbacks = callbacks,
                contentColor = contentColor,
            )

            is MisskeyAntenna -> AntennaItemContent(
                item = item,
                callbacks = callbacks,
                contentColor = contentColor,
            )

            is TootFilter -> FilterItemContent(
                activity = activity,
                item = item,
                callbacks = callbacks,
                contentColor = contentColor,
                acctColor = acctColor,
            )

            is TootMessageHolder -> MessageHolderContent(
                item = item,
                contentColor = contentColor,
            )

            is TootConversationSummary -> ConversationSummaryItemContent(
                activity = activity,
                column = column,
                accessInfo = accessInfo,
                item = item,
                bSimpleList = bSimpleList,
                callbacks = callbacks,
                contentColor = contentColor,
                acctColor = acctColor,
            )

            is TootScheduled -> ScheduledItemContent(
                activity = activity,
                column = column,
                accessInfo = accessInfo,
                item = item,
                callbacks = callbacks,
                contentColor = contentColor,
                acctColor = acctColor,
            )
        }
    }
}
