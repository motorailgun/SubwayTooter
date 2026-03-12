package jp.juggler.subwaytooter.dialog

import android.app.Dialog
import android.view.WindowManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.juggler.subwaytooter.ActPost
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.actpost.DRAFT_CONTENT
import jp.juggler.subwaytooter.actpost.DRAFT_CONTENT_WARNING
import jp.juggler.subwaytooter.api.entity.TootStatus
import jp.juggler.subwaytooter.compose.StThemedContent
import jp.juggler.subwaytooter.table.PostDraft
import jp.juggler.subwaytooter.table.daoPostDraft
import jp.juggler.util.coroutine.AppDispatchers
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.data.JsonObject
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import jp.juggler.util.ui.dismissSafe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

class DlgDraftPicker {

    companion object {
        private val log = LogCategory("DlgDraftPicker")
    }

    private lateinit var activity: ActPost
    private lateinit var callback: (draft: JsonObject) -> Unit

    fun open(activityArg: ActPost, callbackArg: (draft: JsonObject) -> Unit) {
        this.activity = activityArg
        this.callback = callbackArg

        val drafts = mutableStateListOf<PostDraft>()

        val dialog = Dialog(activity)

        fun reload() {
            activity.launchAndShowError {
                val newList = try {
                    withContext(AppDispatchers.IO) {
                        val cursor = daoPostDraft.createCursor()
                        val colIdx = PostDraft.ColIdx(cursor)
                        val result = mutableListOf<PostDraft>()
                        while (cursor.moveToNext()) {
                            result.add(colIdx.readRow(cursor))
                        }
                        cursor.close()
                        result
                    }
                } catch (ignored: CancellationException) {
                    return@launchAndShowError
                } catch (ex: Throwable) {
                    log.e(ex, "failed to loading drafts.")
                    activity.showToast(ex, "failed to loading drafts.")
                    return@launchAndShowError
                }
                if (dialog.isShowing) {
                    drafts.clear()
                    drafts.addAll(newList)
                }
            }
        }

        val composeView = ComposeView(activity).apply {
            setContent {
                StThemedContent {
                    DraftPickerContent(
                        drafts = drafts,
                        onSelect = { draft ->
                            val json = draft.json
                            if (json != null) {
                                callback(json)
                                dialog.dismissSafe()
                            }
                        },
                        onDelete = { draft ->
                            activity.launchAndShowError {
                                daoPostDraft.delete(draft)
                                reload()
                                activity.showToast(false, R.string.draft_deleted)
                            }
                        },
                        onCancel = { dialog.dismissSafe() },
                    )
                }
            }
        }
        dialog.setContentView(composeView)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
        dialog.show()
        reload()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DraftPickerContent(
    drafts: List<PostDraft>,
    onSelect: (PostDraft) -> Unit,
    onDelete: (PostDraft) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text(
                text = stringResource(R.string.select_draft),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth(),
            ) {
                items(drafts, key = { it.id }) { draft ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onSelect(draft) },
                                onLongClick = { onDelete(draft) },
                            )
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = TootStatus.formatTime(context, draft.time_save, false),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        val displayText = remember(draft) {
                            buildDraftDisplayText(draft)
                        }
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
            Text(
                text = stringResource(R.string.draft_picker_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

private fun buildDraftDisplayText(draft: PostDraft): String {
    val json = draft.json ?: return ""
    val cw = json.string(DRAFT_CONTENT_WARNING)
    val c = json.string(DRAFT_CONTENT)
    val sb = StringBuilder()
    if (cw?.trim()?.isNotEmpty() == true) {
        sb.append(cw)
    }
    if (c?.trim()?.isNotEmpty() == true) {
        if (sb.isNotEmpty()) sb.append("\n")
        sb.append(c)
    }
    return sb.toString()
}
