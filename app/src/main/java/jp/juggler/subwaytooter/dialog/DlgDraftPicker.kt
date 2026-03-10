package jp.juggler.subwaytooter.dialog

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.database.Cursor
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import jp.juggler.subwaytooter.ActPost
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.actpost.DRAFT_CONTENT
import jp.juggler.subwaytooter.actpost.DRAFT_CONTENT_WARNING
import jp.juggler.subwaytooter.api.entity.TootStatus
import jp.juggler.subwaytooter.table.PostDraft
import jp.juggler.subwaytooter.table.daoPostDraft
import jp.juggler.util.*
import jp.juggler.util.coroutine.AppDispatchers
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.data.JsonObject
import jp.juggler.util.data.cast
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import jp.juggler.util.ui.attrColor
import jp.juggler.util.ui.dismissSafe
import jp.juggler.util.ui.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

class DlgDraftPicker : AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener,
    DialogInterface.OnDismissListener {

    companion object {

        private val log = LogCategory("DlgDraftPicker")
    }

    private lateinit var activity: ActPost
    private lateinit var callback: (draft: JsonObject) -> Unit
    private lateinit var lvDraft: ListView
    private lateinit var adapter: MyAdapter
    private lateinit var dialog: AlertDialog

    private var listCursor: Cursor? = null
    private var colIdx: PostDraft.ColIdx? = null

    private var task: Job? = null

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val json = getPostDraft(position)?.json
        if (json != null) {
            callback(json)
            dialog.dismissSafe()
        }
    }

    override fun onItemLongClick(
        parent: AdapterView<*>,
        view: View,
        position: Int,
        id: Long,
    ): Boolean {
        activity.launchAndShowError {
            getPostDraft(position)?.let {
                daoPostDraft.delete(it)
                reload()
                activity.showToast(false, R.string.draft_deleted)
            }
        }
        return true
    }

    override fun onDismiss(dialog: DialogInterface) {
        task?.cancel()
        task = null
        lvDraft.adapter = null
        listCursor?.close()
        listCursor = null
    }

    @SuppressLint("InflateParams")
    fun open(activityArg: ActPost, callbackArg: (draft: JsonObject) -> Unit) {
        this.activity = activityArg
        this.callback = callbackArg

        adapter = MyAdapter()

        val dp12 = activity.dp(12)
        val viewRoot = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
            )
        }
        lvDraft = ListView(activity).apply {
            isFastScrollEnabled = false
            isScrollbarFadingEnabled = false
            scrollBarStyle = View.SCROLLBARS_OUTSIDE_OVERLAY
        }
        viewRoot.addView(lvDraft, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        viewRoot.addView(TextView(activity).apply {
            text = activity.getString(R.string.draft_picker_desc)
            setPadding(dp12, 0, dp12, 0)
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))

        lvDraft.onItemClickListener = this
        lvDraft.onItemLongClickListener = this
        lvDraft.adapter = adapter

        this.dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.select_draft)
            .setNegativeButton(R.string.cancel, null)
            .setView(viewRoot)
            .create()
        dialog.setOnDismissListener(this)

        dialog.show()

        reload()
    }

    private fun reload() {

        // cancel old task
        task?.cancel()

        task = activity.launchAndShowError {
            val newCursor = try {
                withContext(AppDispatchers.IO) {
                    daoPostDraft.createCursor()
                }
            } catch (ignored: CancellationException) {
                return@launchAndShowError
            } catch (ex: Throwable) {
                log.e(ex, "failed to loading drafts.")
                activity.showToast(ex, "failed to loading drafts.")
                return@launchAndShowError
            }

            if (!dialog.isShowing) {
                // dialog is already closed.
                newCursor.close()
            } else {
                val old = listCursor
                listCursor = newCursor
                colIdx = PostDraft.ColIdx(newCursor)
                adapter.notifyDataSetChanged()
                old?.close()
            }
        }
    }

    private fun getPostDraft(position: Int): PostDraft? =
        listCursor?.let {
            daoPostDraft.loadFromCursor(it, colIdx, position)
        }

    private inner class MyViewHolder(
        parent: ViewGroup?,
    ) {
        val tvTime: TextView
        val tvText: TextView
        val root: LinearLayout

        init {
            val dp3 = activity.dp(3)
            val dp12 = activity.dp(12)
            root = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setPadding(dp12, dp3, dp12, dp3)
                tag = this@MyViewHolder
            }
            tvTime = TextView(activity).apply {
                gravity = android.view.Gravity.END
                setTextColor(activity.attrColor(R.attr.colorColumnHeaderAcct))
                textSize = 12f
            }
            root.addView(tvTime, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ))
            tvText = TextView(activity).apply {
                minHeight = activity.dp(40)
                maxWidth = activity.dp(300)
            }
            root.addView(tvText, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ))
        }

        fun bind(draft: PostDraft?) {
            draft ?: return
            val context = root.context
            tvTime.text =
                TootStatus.formatTime(context, draft.time_save, false)

            val json = draft.json
            if (json != null) {
                val cw = json.string(DRAFT_CONTENT_WARNING)
                val c = json.string(DRAFT_CONTENT)
                val sb = StringBuilder()
                if (cw?.trim { it <= ' ' }?.isNotEmpty() == true) {
                    sb.append(cw)
                }
                if (c?.trim { it <= ' ' }?.isNotEmpty() == true) {
                    if (sb.isNotEmpty()) sb.append("\n")
                    sb.append(c)
                }
                tvText.text = sb
            }
        }
    }

    private inner class MyAdapter : BaseAdapter() {
        override fun getCount() = listCursor?.count ?: 0
        override fun getItemId(position: Int) = 0L
        override fun getItem(position: Int) = getPostDraft(position)
        override fun getView(position: Int, convertView: View?, parent: ViewGroup) =
            (convertView?.tag?.cast() ?: MyViewHolder(parent))
                .also { it.bind(getItem(position)) }
                .root
    }
}
