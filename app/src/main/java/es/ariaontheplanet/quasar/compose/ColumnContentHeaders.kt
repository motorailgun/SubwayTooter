@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package es.ariaontheplanet.quasar.compose

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.ariaontheplanet.quasar.ActMain
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.actmain.nextPosition
import es.ariaontheplanet.quasar.api.entity.TootAccount
import es.ariaontheplanet.quasar.api.entity.TootAccountRef
import es.ariaontheplanet.quasar.api.entity.TootInstance
import es.ariaontheplanet.quasar.column.Column
import es.ariaontheplanet.quasar.column.HeaderType
import es.ariaontheplanet.quasar.compose.richtext.RichText
import es.ariaontheplanet.quasar.compose.richtext.toRichContent
import es.ariaontheplanet.quasar.span.MyClickableSpan
import es.ariaontheplanet.quasar.table.SavedAccount
import es.ariaontheplanet.quasar.util.openCustomTab

/**
 * Renders the appropriate content-specific header based on the column's [Column.HeaderType].
 *
 * Profile, Instance, Search, Filter, ProfileDirectory headers are inserted
 * as the first item in the timeline LazyColumn.
 */
@Composable
fun ColumnContentHeader(
    activity: ActMain,
    column: Column,
    callbacks: TimelineCallbacks,
    contentColor: Int,
    modifier: Modifier = Modifier,
) {
    when (column.type.headerType) {
        HeaderType.Profile -> {
            val whoRef = column.whoAccount
            if (whoRef != null) {
                ProfileHeader(
                    activity = activity,
                    column = column,
                    accessInfo = column.accessInfo,
                    whoRef = whoRef,
                    callbacks = callbacks,
                    contentColor = contentColor,
                    modifier = modifier,
                )
            }
        }

        HeaderType.Instance -> {
            val instance = column.instanceInformation
            if (instance != null) {
                InstanceHeader(
                    instance = instance,
                    contentColor = contentColor,
                    modifier = modifier,
                )
            }
        }

        HeaderType.Search,
        HeaderType.Filter,
        HeaderType.ProfileDirectory,
            -> {
            // Search input is handled by ColumnSearchBar.
            // Filter and ProfileDirectory have minimal header needs
            // (settings are in ColumnSettingsPanel).
        }

        null -> {
            // No special header for this column type.
        }
    }
}

// =========================================================================
// Profile Header
// =========================================================================

/**
 * Profile header showing banner, avatar, display name, acct, bio,
 * metadata fields, and follow stats.
 */
@Composable
fun ProfileHeader(
    activity: ActMain,
    column: Column,
    accessInfo: SavedAccount,
    whoRef: TootAccountRef,
    callbacks: TimelineCallbacks,
    contentColor: Int,
    modifier: Modifier = Modifier,
) {
    val who = whoRef.get()
    val contentColorCompose = Color(contentColor)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { callbacks.onFollowClick(column, null, whoRef) },
                onLongClick = { callbacks.onFollowLongClick(column, null, whoRef) },
            ),
    ) {
        // Banner image
        val bannerUrl = who.header
        if (!bannerUrl.isNullOrEmpty() &&
            !bannerUrl.endsWith("missing.png")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
            ) {
                NetworkImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    staticUrl = accessInfo.supplyBaseUrl(who.header_static),
                    animatedUrl = accessInfo.supplyBaseUrl(who.header),
                    contentDescription = null,
                    scaleType = ImageView.ScaleType.CENTER_CROP,
                )
                // Semi-transparent overlay for readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)),
                )
            }
        }

        // Avatar + Name + Follow button area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Avatar
            NetworkImage(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp)),
                staticUrl = accessInfo.supplyBaseUrl(who.avatar_static),
                animatedUrl = accessInfo.supplyBaseUrl(who.avatar),
                contentDescription = stringResource(R.string.thumbnail),
                scaleType = ImageView.ScaleType.FIT_CENTER,
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Name + acct
            Column(modifier = Modifier.weight(1f)) {
                val nameContent = remember(whoRef.decoded_display_name) {
                    whoRef.decoded_display_name.toRichContent()
                }
                val nameStyle = remember(ActMain.timelineFontBold) {
                    androidx.compose.ui.text.TextStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily(ActMain.timelineFontBold),
                        fontSize = 18f.sp,
                    )
                }
                RichText(
                    content = nameContent,
                    color = Color(contentColor),
                    style = nameStyle,
                )

                AcctText(
                    accessInfo = accessInfo,
                    who = who,
                    acctColor = contentColor,
                )

                // Badges (locked, bot, etc.)
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (who.locked) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lock),
                            contentDescription = "Locked",
                            tint = contentColorCompose,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    if (who.bot) {
                        Icon(
                            painter = painterResource(R.drawable.ic_bot),
                            contentDescription = "Bot",
                            tint = contentColorCompose,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            // Follow button
            FollowButtonArea(
                activity = activity,
                column = column,
                accessInfo = accessInfo,
                whoRef = whoRef,
                callbacks = callbacks,
                contentColor = contentColor,
            )
        }

        // Bio / note
        if (whoRef.decoded_note.isNotEmpty()) {
            val noteOnLinkClick = remember(activity, column) {
                { url: String ->
                    openCustomTab(
                        activity = activity,
                        pos = activity.nextPosition(column),
                        url = url,
                        accessInfo = column.accessInfo,
                    )
                }
            }
            val noteContent = remember(whoRef.decoded_note, noteOnLinkClick) {
                whoRef.decoded_note.toRichContent(MyClickableSpan.defaultLinkColor, noteOnLinkClick)
            }
            val noteStyle = remember {
                androidx.compose.ui.text.TextStyle(
                    fontSize = (ActMain.timelineFontSizeSp.takeIf { it.isFinite() } ?: 14f).sp,
                )
            }
            RichText(
                content = noteContent,
                color = Color(contentColor),
                style = noteStyle,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }

        // Metadata fields
        who.fields?.let { fields ->
            if (fields.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    fields.forEach { field ->
                        ProfileFieldRow(
                            field = field,
                            contentColor = contentColor,
                        )
                    }
                }
            }
        }

        // Stats row (followers, following, posts)
        ProfileStatsRow(
            who = who,
            contentColor = contentColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )

        HorizontalDivider(
            color = contentColorCompose.copy(alpha = 0.2f),
            thickness = 0.5.dp,
        )
    }
}

@Composable
private fun ProfileFieldRow(
    field: TootAccount.Field,
    contentColor: Int,
) {
    val contentColorCompose = Color(contentColor)
    val verified = field.verified_at > 0L

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = field.name,
            color = contentColorCompose.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(100.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = field.value,
            color = contentColorCompose,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (verified) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = "Verified",
                tint = Color(0xFF00C853),
                modifier = Modifier
                    .size(16.dp)
                    .padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun ProfileStatsRow(
    who: TootAccount,
    contentColor: Int,
    modifier: Modifier = Modifier,
) {
    val contentColorCompose = Color(contentColor)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ProfileStatItem(
            label = stringResource(R.string.toot_count),
            count = who.statuses_count,
            color = contentColorCompose,
        )
        ProfileStatItem(
            label = stringResource(R.string.following),
            count = who.following_count,
            color = contentColorCompose,
        )
        ProfileStatItem(
            label = stringResource(R.string.followers),
            count = who.followers_count,
            color = contentColorCompose,
        )
    }
}

@Composable
private fun ProfileStatItem(
    label: String,
    count: Long?,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count?.toString() ?: "-",
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            color = color.copy(alpha = 0.7f),
            fontSize = 11.sp,
        )
    }
}

// =========================================================================
// Instance Header
// =========================================================================

/**
 * Instance information header showing name, thumbnail, description, and stats.
 */
@Composable
fun InstanceHeader(
    instance: TootInstance,
    contentColor: Int,
    modifier: Modifier = Modifier,
) {
    val contentColorCompose = Color(contentColor)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
    ) {
        // Thumbnail
        val thumbnail = instance.thumbnail
        if (!thumbnail.isNullOrEmpty()) {
            NetworkImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(8.dp)),
                staticUrl = thumbnail,
                contentDescription = instance.title ?: "",
                scaleType = ImageView.ScaleType.CENTER_CROP,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Instance title
        instance.title?.let { title ->
            Text(
                text = title,
                color = contentColorCompose,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Version
        instance.version?.let { version ->
            Text(
                text = "v$version",
                color = contentColorCompose.copy(alpha = 0.6f),
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Description
        val desc = instance.description ?: instance.descriptionOld
        if (!desc.isNullOrEmpty()) {
            Text(
                text = desc,
                color = contentColorCompose,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            )
        }

        // Stats
        instance.stats?.let { stats ->
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                InstanceStatItem(
                    label = stringResource(R.string.user_count),
                    count = stats.user_count,
                    color = contentColorCompose,
                )
                InstanceStatItem(
                    label = stringResource(R.string.toot_count),
                    count = stats.status_count,
                    color = contentColorCompose,
                )
                InstanceStatItem(
                    label = stringResource(R.string.domain_count),
                    count = stats.domain_count,
                    color = contentColorCompose,
                )
            }
        }

        // Contact email
        instance.email?.let { email ->
            if (email.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.contact) + ": " + email,
                    color = contentColorCompose.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(
            color = contentColorCompose.copy(alpha = 0.2f),
            thickness = 0.5.dp,
        )
    }
}

@Composable
private fun InstanceStatItem(
    label: String,
    count: Long,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (count >= 0) count.toString() else "-",
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            color = color.copy(alpha = 0.7f),
            fontSize = 11.sp,
        )
    }
}
