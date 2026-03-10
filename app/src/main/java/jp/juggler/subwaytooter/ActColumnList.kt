package jp.juggler.subwaytooter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.woxthebox.draglistview.DragItem
import com.woxthebox.draglistview.DragItemAdapter
import com.woxthebox.draglistview.DragListView
import com.woxthebox.draglistview.swipe.ListSwipeHelper
import com.woxthebox.draglistview.swipe.ListSwipeItem
import jp.juggler.subwaytooter.api.entity.Acct
import jp.juggler.subwaytooter.api.showApiError
import jp.juggler.subwaytooter.column.ColumnEncoder
import jp.juggler.subwaytooter.column.ColumnType
import jp.juggler.subwaytooter.dialog.DlgConfirm.confirm
import jp.juggler.subwaytooter.view.wrapTitleTextView
import jp.juggler.util.backPressed
import jp.juggler.util.coroutine.launchMain
import jp.juggler.util.data.JsonObject
import jp.juggler.util.data.notZero
import jp.juggler.util.data.toJsonArray
import jp.juggler.util.int
import jp.juggler.util.log.LogCategory
import jp.juggler.util.ui.attrColor
import jp.juggler.util.ui.attrDrawable
import jp.juggler.util.ui.dp
import jp.juggler.util.ui.setContentViewAndInsets
import jp.juggler.util.ui.setNavigationBack
import jp.juggler.util.ui.vg

class ActColumnList : AppCompatActivity() {

    companion object {

        private val log = LogCategory("ActColumnList")
        private const val TMP_FILE_COLUMN_LIST = "tmp_column_list"

        // リザルトに使うのでpublic
        const val EXTRA_ORDER = "order"
        const val EXTRA_SELECTION = "selection"

        fun createIntent(activity: ActMain, currentItem: Int) =
            Intent(activity, ActColumnList::class.java).apply {
                val array = activity.appState.encodeColumnList()
                AppState.saveColumnList(activity, TMP_FILE_COLUMN_LIST, array)
                putExtra(EXTRA_SELECTION, currentItem)
            }
    }

    private lateinit var toolbar: Toolbar
    private lateinit var listView: DragListView

    private fun createViews(): View {
        val tv = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)
        val actionBarHeight = resources.getDimensionPixelSize(tv.resourceId)

        toolbar = Toolbar(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                actionBarHeight
            )
            background = resources.getDrawable(R.drawable.action_bar_bg, theme)
            elevation = dp(4).toFloat()
        }

        listView = DragListView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1f
            )
        }

        val descText = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(4)
                bottomMargin = dp(4)
            }
            gravity = Gravity.CENTER
            setText(R.string.column_list_desc)
            textSize = 12f
        }

        return LinearLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(attrColor(R.attr.colorMainBackground))
            addView(toolbar)
            addView(listView)
            addView(descText)
        }
    }

    private val listAdapter by lazy { MyListAdapter() }

    private val defaultAcctColorFg by lazy {
        attrColor(R.attr.colorColumnHeaderAcct)
    }
    private val defaultColumnColorFg by lazy {
        attrColor(R.attr.colorColumnHeaderName)
    }
    private var oldSelection: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        backPressed {
            makeResult(-1)
            finish()
        }

        super.onCreate(savedInstanceState)

        App1.setActivityTheme(this)
        setContentViewAndInsets(createViews())
        initUI()

        if (savedInstanceState != null) {
            restoreData(savedInstanceState.int(EXTRA_SELECTION) ?: -1)
        } else {
            val intent = intent
            restoreData(intent?.int(EXTRA_SELECTION) ?: -1)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_SELECTION, oldSelection)
        val array = listAdapter.itemList.map { it.json }.toJsonArray()
        AppState.saveColumnList(this, TMP_FILE_COLUMN_LIST, array)
    }

    private fun initUI() {
        setSupportActionBar(toolbar)
        wrapTitleTextView()
        setNavigationBack(toolbar)
        fixHorizontalMargin(listView)

        // ハンドル部分をドラッグで並べ替えできるRecyclerView
        listView.setLayoutManager(androidx.recyclerview.widget.LinearLayoutManager(this))
        listView.setAdapter(listAdapter, true)
        listView.setCanDragHorizontally(false)
        listView.setCustomDragItem(MyDragItem(this))

        listView.recyclerView.isVerticalScrollBarEnabled = true
        listView.setDragListListener(object : DragListView.DragListListenerAdapter() {
            override fun onItemDragStarted(position: Int) = Unit
            override fun onItemDragEnded(fromPosition: Int, toPosition: Int) = Unit
        })

        // リストを左右スワイプした
        listView.setSwipeListener(object : ListSwipeHelper.OnSwipeListenerAdapter() {
            override fun onItemSwipeStarted(item: ListSwipeItem) = Unit
            override fun onItemSwipeEnded(
                item: ListSwipeItem,
                swipedDirection: ListSwipeItem.SwipeDirection?,
            ) {
                // 左にスワイプした(右端に青が見えた) なら要素を削除する
                if (swipedDirection == ListSwipeItem.SwipeDirection.LEFT) {
                    val adapterItem = (item.tag as MyViewHolder).lastItem ?: return
                    launchMain {
                        try {
                            if (adapterItem.json.optBoolean(ColumnEncoder.KEY_DONT_CLOSE, false)) {
                                confirm(R.string.confirm_remove_column_mark_as_dont_close)
                            }
                            listAdapter.removeItem(listAdapter.getPositionForItem(adapterItem))
                        } catch (ex: Throwable) {
                            showApiError(ex)
                        } finally {
                            try {
                                listView.resetSwipedViews(null)
                            } catch (_: Throwable) {
                            }
                        }
                    }
                }
            }
        })
    }

    private fun restoreData(ivSelection: Int) {
        oldSelection = ivSelection
        // DragItemAdapter はMutableListを要求する
        listAdapter.itemList = ArrayList<MyItem>().apply {
            try {
                AppState.loadColumnList(applicationContext, TMP_FILE_COLUMN_LIST)
                    ?.objectList()
                    ?.forEachIndexed { index, src ->
                        try {
                            val item = MyItem(
                                src,
                                index.toLong(),
                            )
                            add(item)
                            if (oldSelection == item.oldIndex) {
                                item.setOldSelection(true)
                            }
                        } catch (ex: Throwable) {
                            log.e(ex, "restoreData: item decode failed.")
                        }
                    }
            } catch (ex: Throwable) {
                log.e(ex, "restoreData failed.")
            }
        }
    }

    private fun makeResult(newSelection: Int) {
        val intent = Intent()

        val itemList = listAdapter.itemList
        // どの要素を選択するか
        if (newSelection >= 0 && newSelection < listAdapter.itemCount) {
            intent.putExtra(EXTRA_SELECTION, newSelection)
        } else {
            var i = 0
            val ie = itemList.size
            while (i < ie) {
                if (itemList[i].bOldSelection) {
                    intent.putExtra(EXTRA_SELECTION, i)
                    break
                }
                ++i
            }
        }
        // 並べ替え用データ
        val orderList = ArrayList<Int>()
        for (item in itemList) {
            orderList.add(item.oldIndex)
        }
        intent.putExtra(EXTRA_ORDER, orderList)

        setResult(Activity.RESULT_OK, intent)
    }

    private fun performItemSelected(item: MyItem?) {
        item ?: return
        val idx = listAdapter.getPositionForItem(item)
        makeResult(idx)
        finish()
    }

    // リスト要素のデータ
    internal inner class MyItem(
        val json: JsonObject,
        val id: Long,
        val name: String = json.optString(ColumnEncoder.KEY_COLUMN_NAME),
        val acct: Acct = Acct.parse(json.optString(ColumnEncoder.KEY_COLUMN_ACCESS_ACCT)),
        val acctName: String = json.optString(ColumnEncoder.KEY_COLUMN_ACCESS_STR),
        val oldIndex: Int = json.optInt(ColumnEncoder.KEY_OLD_INDEX),
        val type: ColumnType = ColumnType.parse(json.optInt(ColumnEncoder.KEY_TYPE)),
        val acctColorBg: Int = json.optInt(ColumnEncoder.KEY_COLUMN_ACCESS_COLOR_BG, 0),
        val acctColorFg: Int = json.optInt(ColumnEncoder.KEY_COLUMN_ACCESS_COLOR, 0),
        val columnColorFg: Int = json.optInt(ColumnEncoder.KEY_HEADER_TEXT_COLOR, 0),
        val columnColorBg: Int = json.optInt(ColumnEncoder.KEY_HEADER_BACKGROUND_COLOR, 0),
    ) {
        var bOldSelection: Boolean = false

        fun setOldSelection(b: Boolean) {
            bOldSelection = b
        }
    }

    // リスト要素のViewHolder
    private inner class MyViewHolder(
        parent: ViewGroup?,
        val rootView: ListSwipeItem = LayoutInflater.from(parent?.context ?: this@ActColumnList)
            .inflate(R.layout.lv_column_list, parent, false) as ListSwipeItem,
    ) : DragItemAdapter.ViewHolder(
        rootView,
        R.id.ivDragHandle, // View ID。 ここを押すとドラッグ操作をすぐに開始する
        true, // 長押しでドラッグ開始するなら真
    ) {
        var lastItem: MyItem? = null
        val acctPadLr = (0.5f + 4f * rootView.resources.displayMetrics.density).toInt()
        val ivSelected: ImageView = rootView.findViewById(R.id.ivSelected)
        val tvAccess: TextView = rootView.findViewById(R.id.tvAccess)
        val llColumn: LinearLayout = rootView.findViewById(R.id.llColumn)
        val tvColumnName: TextView = rootView.findViewById(R.id.tvColumnName)
        val ivColumnIcon: ImageView = rootView.findViewById(R.id.ivColumnIcon)

        init {
            rootView.tag = this
            rootView.setSwipeInStyle(ListSwipeItem.SwipeInStyle.SLIDE)
            rootView.supportedSwipeDirection = ListSwipeItem.SwipeDirection.LEFT
        }

        fun bind(item: MyItem?) {
            item ?: return
            lastItem = item
            ivSelected.vg(item.bOldSelection)
            tvAccess.text = item.acctName
            tvAccess.setTextColor(item.acctColorFg.notZero() ?: defaultAcctColorFg)
            tvAccess.setBackgroundColor(item.acctColorBg)
            tvAccess.setPaddingRelative(acctPadLr, 0, acctPadLr, 0)

            val columnColorFg = item.columnColorFg.notZero() ?: defaultColumnColorFg
            llColumn.setBackgroundColor(item.columnColorBg)
            tvColumnName.text = item.name
            tvColumnName.setTextColor(columnColorFg)
            ivColumnIcon.setImageResource(item.type.iconId(item.acct))
            ivColumnIcon.imageTintList = ColorStateList.valueOf(columnColorFg)
            ivSelected.imageTintList = ColorStateList.valueOf(columnColorFg)

            // 背景色がテーマ次第なので、カラム設定の色を反映するとアイコンが見えなくなる可能性がある
            // よってアイコンやテキストにカラム設定の色を反映しない
        }

        override fun onItemClicked(view: View?) {
            performItemSelected(lastItem)
        }
    }

    // ドラッグ操作中のデータ
    private inner class MyDragItem(context: Context) : DragItem(context, R.layout.lv_column_list) {
        override fun onBindDragView(clickedView: View, dragView: View) {
            val clickVh = clickedView.tag as MyViewHolder
            val item = clickVh.lastItem!!

            dragView.findViewById<TextView>(R.id.tvAccess).run {
                text = item.acctName
                setTextColor(item.acctColorFg.notZero() ?: defaultAcctColorFg)
                setBackgroundColor(item.acctColorBg)
            }
            val columnColorFg = item.columnColorFg.notZero() ?: defaultColumnColorFg
            dragView.findViewById<TextView>(R.id.tvColumnName).run {
                text = item.name
                setTextColor(columnColorFg)
            }
            dragView.findViewById<ImageView>(R.id.ivColumnIcon).run {
                imageTintList = ColorStateList.valueOf(columnColorFg)
                setImageResource(item.type.iconId(item.acct))
            }
            dragView.findViewById<ImageView>(R.id.ivSelected).run {
                imageTintList = ColorStateList.valueOf(columnColorFg)
                visibility = clickVh.ivSelected.visibility
            }
            dragView.findViewById<LinearLayout>(R.id.llColumn)
                .setBackgroundColor(item.columnColorBg)
            dragView.findViewById<FrameLayout>(R.id.item_layout)
                .setBackgroundColor(attrColor(R.attr.list_item_bg_pressed_dragged))
        }
    }

    private inner class MyListAdapter : DragItemAdapter<MyItem, MyViewHolder>() {

        init {
            setHasStableIds(true)
            itemList = ArrayList()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder =
            MyViewHolder(parent)

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            super.onBindViewHolder(holder, position)
            holder.bind(itemList[position])
        }

        override fun getUniqueItemId(position: Int): Long {
            val item = mItemList[position] // mItemList は親クラスのメンバ変数
            return item.id
        }
    }
}
