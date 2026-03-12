package jp.juggler.subwaytooter.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.defaultColorIcon
import jp.juggler.subwaytooter.util.PostAttachment
import jp.juggler.util.coroutine.cancellationException
import jp.juggler.util.data.ellipsizeDot3
import jp.juggler.util.log.LogCategory
import jp.juggler.util.ui.attrColor
import jp.juggler.util.ui.dismissSafe
import jp.juggler.util.ui.dp
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

private val log = LogCategory("DlgAttachmentRearrange")

/**
 * 投稿画面で添付メディアを並べ替えるダイアログを開き、OKボタンが押されるまで非同期待機する。
 * OK以外の方法で閉じたらCancellationExceptionを投げる。
 */
suspend fun ComponentActivity.dialogAttachmentRearrange(
    initialList: List<PostAttachment>,
): List<PostAttachment> = suspendCancellableCoroutine { cont ->
    val dp8 = dp(8)
    val dp48 = dp(48)
    val dp1 = dp(1)
    val dividerColor = attrColor(R.attr.colorSettingDivider)
    val textColor = attrColor(R.attr.colorTextContent)

    val listView = RecyclerView(this).apply {
        clipToPadding = false
        isScrollbarFadingEnabled = false
        scrollBarStyle = View.SCROLLBARS_OUTSIDE_OVERLAY
        setPadding(dp48, dp8, dp48, dp8)
    }
    val btnCancel = Button(this).apply {
        text = getString(R.string.cancel)
        setBackgroundResource(R.drawable.btn_bg_transparent_round6dp)
        setTextColor(textColor)
    }
    val btnOk = Button(this).apply {
        text = getString(R.string.ok)
        setBackgroundResource(R.drawable.btn_bg_transparent_round6dp)
        setTextColor(textColor)
    }
    // header label
    val tvDesc = TextView(this).apply {
        text = getString(R.string.attachment_rearrange_desc)
        setPadding(dp8, dp8, dp8, dp8)
        gravity = android.view.Gravity.CENTER
        includeFontPadding = false
        setTextColor(textColor)
    }
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(tvDesc, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
        addView(listView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        // divider
        addView(View(this@dialogAttachmentRearrange).apply {
            setBackgroundColor(dividerColor)
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp1
        ))
        // button bar
        addView(LinearLayout(this@dialogAttachmentRearrange).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(btnCancel, LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(View(this@dialogAttachmentRearrange).apply {
                setBackgroundColor(dividerColor)
            }, LinearLayout.LayoutParams(dp1,
                LinearLayout.LayoutParams.MATCH_PARENT))
            addView(btnOk, LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
    }

    val dialog = Dialog(this).apply {
        setContentView(root)
        setOnDismissListener {
            if (cont.isActive) cont.resumeWithException(cancellationException())
        }
    }

    cont.invokeOnCancellation { dialog.dismissSafe() }

    val myAdapter = RearrangeAdapter(layoutInflater, initialList)

    btnCancel.setOnClickListener {
        dialog.dismissSafe()
    }

    btnOk.setOnClickListener {
        if (cont.isActive) cont.resume(myAdapter.list) { _, _, _ -> }
        dialog.dismissSafe()
    }

    listView.apply {
        layoutManager = LinearLayoutManager(context)
        adapter = myAdapter
        myAdapter.itemTouchHelper.attachToRecyclerView(this)
    }

    dialog.window?.setLayout(dp(300), dp(440))
    dialog.show()
}

/**
 * 並べ替えダイアログ内部のRecyclerViewに使うAdapter
 */
private class RearrangeAdapter(
    private val inflater: LayoutInflater,
    initialList: List<PostAttachment>,
) : RecyclerView.Adapter<RearrangeAdapter.MyViewHolder>(), MyDragCallback.Changer {

    val list = ArrayList(initialList)

    private var lastStateViewHolder: MyViewHolder? = null
    private var draggingItem: PostAttachment? = null

    val itemTouchHelper = ItemTouchHelper(MyDragCallback(this))

    override fun getItemCount() = list.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        MyViewHolder(parent)

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(list.elementAtOrNull(position))
    }

    // implements MyDragCallback.Changer
    override fun onMove(posFrom: Int, posTo: Int): Boolean {
        val item = list.removeAt(posFrom)
        list.add(posTo, item)
        notifyItemMoved(posFrom, posTo)
        return true
    }

    // implements MyDragCallback.Changer
    override fun onState(
        caller: String,
        viewHolder: RecyclerView.ViewHolder?,
        actionState: Int,
    ) {
        log.d("onState: caller=$caller, viewHolder=$viewHolder, actionState=$actionState")

        val holder = (viewHolder as? MyViewHolder)
        // 最後にドラッグ対象となったViewHolderを覚えておく
        holder?.let { lastStateViewHolder = it }
        // 現在ドラッグ対象のPostAttachmentを覚えておく
        val pa = holder?.lastItem
        draggingItem = when {
            pa != null && actionState == ItemTouchHelper.ACTION_STATE_DRAG -> pa
            else -> null
        }
        // 表示の更新
        holder?.bind()
        lastStateViewHolder?.takeIf { it != holder }?.bind()
    }

    private val iconPlaceHolder = defaultColorIcon(inflater.context, R.drawable.ic_hourglass)
    private val iconError = defaultColorIcon(inflater.context, R.drawable.ic_error)
    private val iconFallback = defaultColorIcon(inflater.context, R.drawable.ic_clip)

    @SuppressLint("ClickableViewAccessibility")
    inner class MyViewHolder(
        parent: ViewGroup,
    ) : RecyclerView.ViewHolder(
        LinearLayout(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT,
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            val dp6 = (6 * parent.context.resources.displayMetrics.density + 0.5f).toInt()
            val dp3 = (3 * parent.context.resources.displayMetrics.density + 0.5f).toInt()
            val dp4 = (4 * parent.context.resources.displayMetrics.density + 0.5f).toInt()
            val dp80 = (80 * parent.context.resources.displayMetrics.density + 0.5f).toInt()
            setPadding(dp6, dp3, dp6, dp3)
        }
    ) {
        val ivThumbnail: ImageView
        val tvText: TextView
        val rootLayout = itemView as LinearLayout

        var lastItem: PostAttachment? = null

        init {
            val context = parent.context
            val dp80 = (80 * context.resources.displayMetrics.density + 0.5f).toInt()
            val dp4 = (4 * context.resources.displayMetrics.density + 0.5f).toInt()
            ivThumbnail = ImageView(context).apply {
                setBackgroundColor(0x80808080.toInt())
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            rootLayout.addView(ivThumbnail, LinearLayout.LayoutParams(dp80, dp80))
            tvText = TextView(context).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
                setTextColor(context.attrColor(R.attr.colorTextContent))
            }
            rootLayout.addView(tvText, LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginStart = dp4 })

            // リスト項目のタッチですぐにドラッグを開始する
            rootLayout.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper.startDrag(this)
                }
                false
            }
        }

        fun bind(item: PostAttachment? = lastItem) {
            item ?: return
            lastItem = item

            val context = rootLayout.context

            // ドラッグ中は背景色を変える
            rootLayout.apply {
                when {
                    draggingItem === item -> setBackgroundColor(
                        context.attrColor(R.attr.colorSearchFormBackground)
                    )

                    else -> background = null
                }
            }

            // サムネイルのロード開始
            ivThumbnail.apply {
                when (val imageUrl = item.attachment?.preview_url) {
                    null, "" -> {
                        val iconDrawable = when (item.status) {
                            PostAttachment.Status.Progress -> iconPlaceHolder
                            PostAttachment.Status.Error -> iconError
                            else -> iconFallback
                        }
                        Glide.with(context).clear(this)
                        setImageDrawable(iconDrawable)
                    }

                    else -> {
                        Glide.with(context)
                            .load(imageUrl)
                            .placeholder(iconPlaceHolder)
                            .error(iconError)
                            .fallback(iconFallback)
                            .into(this)
                    }
                }
            }

            // テキストの表示
            tvText.text = item.attachment?.run {
                "${type.id} ${description?.ellipsizeDot3(40) ?: ""}"
            } ?: ""
        }
    }
}

/**
 * RectclerViewのDrag&Drop操作に関するコールバック
 */
private class MyDragCallback(
    private val changer: Changer,
) : ItemTouchHelper.SimpleCallback(
    ItemTouchHelper.UP or ItemTouchHelper.DOWN,
    0 // no swipe
) {
    // アダプタに行わせたい処理のinterface
    interface Changer {
        fun onMove(posFrom: Int, posTo: Int): Boolean
        fun onState(caller: String, viewHolder: RecyclerView.ViewHolder?, actionState: Int)
    }

    override fun isLongPressDragEnabled() = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder,
    ): Boolean = changer.onMove(
        // position of drag from
        viewHolder.bindingAdapterPosition,
        // position of drag to
        target.bindingAdapterPosition,
    )

    override fun onSelectedChanged(
        viewHolder: RecyclerView.ViewHolder?,
        actionState: Int,
    ) {
        super.onSelectedChanged(viewHolder, actionState)
        changer.onState("onSelectedChanged", viewHolder, actionState)
    }

    override fun clearView(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
    ) {
        super.clearView(recyclerView, viewHolder)
        changer.onState("clearView", viewHolder, ItemTouchHelper.ACTION_STATE_IDLE)
    }
}
