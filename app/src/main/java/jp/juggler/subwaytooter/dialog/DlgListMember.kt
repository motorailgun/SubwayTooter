package jp.juggler.subwaytooter.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.action.*
import jp.juggler.subwaytooter.api.*
import jp.juggler.subwaytooter.api.entity.*
import jp.juggler.subwaytooter.calcIconRound
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.subwaytooter.table.accountListNonPseudo
import jp.juggler.subwaytooter.table.daoAcctColor
import jp.juggler.subwaytooter.util.NetworkEmojiInvalidator
import jp.juggler.subwaytooter.view.MyNetworkImageView
import jp.juggler.util.coroutine.EmptyScope
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.coroutine.launchMain
import jp.juggler.util.data.*
import jp.juggler.util.log.*
import jp.juggler.util.network.toPostRequestBuilder
import jp.juggler.util.ui.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.*

private val log = LogCategory("DlgListMember")

/**
 * 表示や変更処理に使うデータ
 */
private data class OwnerListStatus(
    val owner: SavedAccount,
    val list: TootList,
    val isRegistered: Boolean,
) {
    override fun hashCode(): Int {
        return super.hashCode()
    }

    // ownerとlistのIDが同じなら真
    override fun equals(other: Any?): Boolean =
        other is OwnerListStatus &&
                owner.db_id == other.owner.db_id &&
                list.id == other.list.id

    // IDが同じかつ表示内容が同じなら真
    fun contentEquals(other: OwnerListStatus): Boolean =
        equals(other) &&
                list.title == other.list.title &&
                isRegistered == other.isRegistered
}

// リストと登録状態を表すViewHolder
private class VhOwnerAndList(
    private val layoutInflater: LayoutInflater,
    private val parent: ViewGroup,
    private val handleCheckChange: (OwnerListStatus) -> Unit,
) : RecyclerView.ViewHolder(
    FrameLayout(parent.context).apply {
        layoutParams = RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT,
            RecyclerView.LayoutParams.WRAP_CONTENT,
        )
        val dp12 = (12 * parent.context.resources.displayMetrics.density + 0.5f).toInt()
        setPadding(dp12, 0, dp12, 0)
    }
) {
    private val cbItem = CheckBox(parent.context).apply {
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        )
        gravity = android.view.Gravity.CENTER_VERTICAL
        minimumHeight = (48 * context.resources.displayMetrics.density + 0.5f).toInt()
        val dp4 = (4 * context.resources.displayMetrics.density + 0.5f).toInt()
        setPadding(paddingLeft, dp4, paddingRight, dp4)
    }

    init {
        (itemView as FrameLayout).addView(cbItem)
    }

    fun bind(data: OwnerListStatus) {
        // リスナを外してから値をセットする
        cbItem.setOnCheckedChangeListener(null)
        cbItem.isChecked = data.isRegistered
        cbItem.text = data.list.title
        // 最後にリスナをセットし直す
        cbItem.setOnCheckedChangeListener { v, isChecked ->
            handleCheckChange(data.copy(isRegistered = isChecked))
        }
    }
}

// New List ボタンを表すViewHolder
private class VhCreate(
    private val layoutInflater: LayoutInflater,
    private val parent: ViewGroup,
    private val handleCreate: () -> Unit,
) : RecyclerView.ViewHolder(
    FrameLayout(parent.context).apply {
        layoutParams = RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT,
            RecyclerView.LayoutParams.WRAP_CONTENT,
        )
        val dp12 = (12 * parent.context.resources.displayMetrics.density + 0.5f).toInt()
        setPadding(dp12, 0, dp12, 0)
        val btn = Button(parent.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            )
            text = parent.context.getString(R.string.list_create)
            isAllCaps = false
            setOnClickListener { handleCreate() }
        }
        addView(btn)
    }
) {
}

// エラーテキストを表すViewHolder
private class VhError(
    private val layoutInflater: LayoutInflater,
    private val parent: ViewGroup,
) : RecyclerView.ViewHolder(
    TextView(parent.context).apply {
        layoutParams = RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT,
            RecyclerView.LayoutParams.WRAP_CONTENT,
        )
        gravity = android.view.Gravity.CENTER_VERTICAL
        minimumHeight = (48 * parent.context.resources.displayMetrics.density + 0.5f).toInt()
        val dp12 = (12 * parent.context.resources.displayMetrics.density + 0.5f).toInt()
        val dp4 = (4 * parent.context.resources.displayMetrics.density + 0.5f).toInt()
        setPadding(dp12, dp4, dp12, dp4)
    }
) {
    fun bind(data: Any?) {
        (itemView as TextView).text = data.toString()
    }
}

// リストの更新部分を調べるDiffUtil
private class OnwedListsDiffUtil : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any) =
        oldItem == newItem

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: Any, newItem: Any) = when {
        oldItem is OwnerListStatus && newItem is OwnerListStatus ->
            oldItem.contentEquals(newItem)

        else -> oldItem == newItem
    }
}

// RecyclerViewのAdapter
private class OwnListAdapter(
    private val layoutInflater: LayoutInflater,
    private val handleCreate: () -> Unit,
    private val handleCheckChange: (OwnerListStatus) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_CREATE = 1
        private const val VIEW_TYPE_ERROR = 2
    }

    private val differ = AsyncListDiffer<Any>(this, OnwedListsDiffUtil())

    val items get() = differ.currentList

    fun replaceTo(newItems: List<Any>) = differ.submitList(newItems)

    fun getItem(position: Int): Any? = items.elementAtOrNull(position)

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) =
        when (getItem(position)) {
            is OwnerListStatus -> VIEW_TYPE_ITEM
            is Double -> VIEW_TYPE_CREATE
            else -> VIEW_TYPE_ERROR
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder = when (viewType) {
        VIEW_TYPE_ITEM -> VhOwnerAndList(
            layoutInflater = layoutInflater,
            parent = parent,
            handleCheckChange = handleCheckChange,
        )

        VIEW_TYPE_CREATE -> VhCreate(
            layoutInflater = layoutInflater,
            parent = parent,
            handleCreate = handleCreate,
        )

        else -> VhError(
            layoutInflater = layoutInflater,
            parent = parent,
        )
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        when (holder) {
            is VhOwnerAndList -> holder.bind(getItem(position) as OwnerListStatus)
            is VhCreate -> Unit
            is VhError -> holder.bind(getItem(position))
        }
    }
}

/**
 * リストメンバー管理ダイアログの状態
 */
class DlgListMember(
    // 画面
    private val activity: ActMain,
    // リストに登録/解除したいユーザ
    private val who: TootAccount,
    // そのユーザのfull acct
    private val whoAcct: Acct,
    // リスト所有者(ダイアログ中で変更可能)
    private var listOwner: SavedAccount?,
) {
    // 選択可能なリストオーナーの一覧
    private val accountList = accountListNonPseudo(null)

    // listOwnerのサーバ上のwhoのアカウント情報
    private var whoLocal: TootAccount? = null

    private val requestChannel = Channel<OwnerListStatus>(capacity = Channel.UNLIMITED).apply {
        EmptyScope.launch {
            while (true) {
                val request = receiveCatching().getOrNull()
                    ?: break
                when (val error = handleRequest(request)) {
                    null -> Unit
                    else -> {
                        error.notEmpty()?.let { activity.showToast(true, it) }
                        // revoke list item
                        updateListItem(request, !request.isRegistered)
                    }
                }
            }
        }
    }

    private val listsAdapter = OwnListAdapter(
        layoutInflater = activity.layoutInflater,
        handleCreate = ::openListCreator,
        handleCheckChange = ::handleCheckChange,
    )

    private val dialog = Dialog(activity)

    // Programmatic views
    private val btnClose: Button
    private val btnListOwner: Button
    private val rvOwnedlists: RecyclerView
    private val ivUser: MyNetworkImageView
    private val tvUserAcct: TextView
    private val tvUserName: TextView
    private val root: LinearLayout

    init {
        val activity = this.activity
        val dp3 = activity.dp(3)
        val dp4 = activity.dp(4)
        val dp6 = activity.dp(6)
        val dp12 = activity.dp(12)
        val dp48 = activity.dp(48)

        ivUser = MyNetworkImageView(activity).apply {
            contentDescription = activity.getString(R.string.thumbnail)
            scaleType = android.widget.ImageView.ScaleType.FIT_END
        }
        tvUserName = TextView(activity)
        tvUserAcct = TextView(activity).apply {
            setTextColor(activity.attrColor(R.attr.colorTimeSmall))
            textSize = 12f
            setPadding(dp4, 0, dp4, 0)
        }
        btnListOwner = Button(activity).apply {
            isAllCaps = false
        }
        rvOwnedlists = RecyclerView(activity).apply {
            isScrollbarFadingEnabled = false
            scrollBarStyle = View.SCROLLBARS_OUTSIDE_OVERLAY
        }
        btnClose = Button(activity).apply {
            text = activity.getString(R.string.close)
        }

        root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            // spacer
            addView(View(activity), LinearLayout.LayoutParams(0, dp6))
            // "Target user" label
            addView(TextView(activity).apply {
                text = activity.getString(R.string.target_user)
                textSize = 14f
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { setMargins(dp12, 0, dp12, 0) })
            // User info row
            addView(LinearLayout(activity).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
                addView(ivUser, LinearLayout.LayoutParams(dp48, (40 * activity.resources.displayMetrics.density + 0.5f).toInt()).apply {
                    marginEnd = dp4
                })
                addView(LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(tvUserName, LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ))
                    addView(tvUserAcct, LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ))
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { setMargins(dp12, dp3, dp12, 0) })
            // divider
            addView(View(activity).apply {
                setBackgroundColor(activity.attrColor(R.attr.colorSettingDivider))
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, activity.dp(1)
            ).apply { setMargins(0, dp6, 0, dp6) })
            // "List owner" label
            addView(TextView(activity).apply {
                text = activity.getString(R.string.list_owner)
                textSize = 14f
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { setMargins(dp12, 0, dp12, 0) })
            // List owner button
            addView(btnListOwner, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { setMargins(dp12, dp3, dp12, 0) })
            // "List" label
            addView(TextView(activity).apply {
                text = activity.getString(R.string.list)
                textSize = 14f
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { setMargins(dp12, 0, dp12, 0) })
            // RecyclerView
            addView(rvOwnedlists, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            ))
            // divider
            addView(View(activity).apply {
                setBackgroundColor(activity.attrColor(R.attr.colorSettingDivider))
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, activity.dp(1)
            ))
            // Close button bar
            addView(LinearLayout(activity).apply {
                addView(btnClose, LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ))
        }
    }

    private fun openListCreator() {
        with(activity) {
            val owner = listOwner ?: return
            launchAndShowError {
                showTextInputDialog(
                    title = getString(R.string.list_create),
                    initialText = null,
                    onEmptyText = {
                        showToast(false, R.string.list_name_empty)
                    },
                ) { text ->
                    listCreate(owner, text)
                        ?: return@showTextInputDialog false
                    if (owner == listOwner) loadLists(owner)
                    true
                }
            }
        }
    }

    /**
     * 表示リスト中のIDがマッチする項目の isRegisteredを変更する
     */
    private fun updateListItem(idSample: OwnerListStatus, newRegistered: Boolean) {
        listsAdapter.replaceTo(
            listsAdapter.items.map<Any, Any> {
                when {
                    it is OwnerListStatus && it == idSample ->
                        it.copy(isRegistered = newRegistered)

                    else -> it
                }
            }
        )
    }

    private fun handleCheckChange(
        data: OwnerListStatus,
    ) = EmptyScope.launch {
        try {
            // 表示リストの内容をすぐに変更する
            updateListItem(data, data.isRegistered)
            // チャネルに変更リクエストを送る
            requestChannel.send(data)
        } catch (ex: Throwable) {
            log.e(ex, "handleCheckChange failed.")
        }
    }

    private fun showLists(owner: SavedAccount?, srcList: ArrayList<TootList>?) {
        with(activity) {
            val newList = buildList {
                when {
                    owner == null || srcList == null ->
                        add(getString(R.string.cant_access_list))

                    else -> {
                        // enable "New List..." if list is not null.
                        add(Double.NaN)
                        when {
                            srcList.isEmpty() ->
                                add(getString(R.string.list_not_created))

                            else -> addAll(
                                srcList.map {
                                    OwnerListStatus(
                                        owner = owner,
                                        list = it,
                                        isRegistered = it.isRegistered,
                                    )
                                }
                            )
                        }
                    }
                }
            }
            listsAdapter.replaceTo(newList)
        }
    }

    // リストの一覧とターゲットユーザの登録状況を取得する
    private fun loadLists(owner: SavedAccount?) {
        if (owner == null) {
            showLists(null, null)
            return
        }
        launchMain {
            var resultList: ArrayList<TootList>? = null
            val result = activity.runApiTask(owner) { client ->
                // 現在の登録状況を知るため、対象ユーザの自タンスでのアカウントIDを取得する
                // ドメインブロックなどの影響で同期できない場合があるが、
                // 一覧そのものは取得できるのでこの段階ではエラーにはしない
                val (r1, ar) = client.syncAccountByAcct(owner, whoAcct)
                r1 ?: return@runApiTask null // cancelled.

                val whoLocal = ar?.get() // may null
                this@DlgListMember.whoLocal = whoLocal

                if (whoLocal == null) activity.showToast(true, r1.error)

                if (owner.isMisskey) {
                    // misskeyではリスト全スキャンしないとユーザの登録状況が分からない?
                    client.request(
                        "/api/users/lists/list",
                        owner
                            .putMisskeyApiToken()
                            .toPostRequestBuilder()
                    )?.also { result ->
                        resultList = parseList(result.jsonArray) {
                            TootList(
                                TootParser(activity, owner),
                                it
                            )
                        }.apply {
                            if (whoLocal != null) {
                                forEach { list ->
                                    list.isRegistered =
                                        true == list.userIds?.any { it == whoLocal.id }
                                }
                            }
                        }
                    }
                } else {
                    val registeredSet = HashSet<EntityId>()

                    // メンバーを指定してリスト登録状況を取得
                    if (whoLocal != null) client.request(
                        "/api/v1/accounts/${whoLocal.id}/lists"
                    )?.also { result ->
                        parseList(result.jsonArray) {
                            TootList(
                                TootParser(activity, owner),
                                it
                            )
                        }.forEach {
                            registeredSet.add(it.id)
                        }
                    }

                    // リスト一覧を取得
                    client.request("/api/v1/lists")?.also { result ->
                        resultList = parseList(result.jsonArray) {
                            TootList(
                                TootParser(activity, owner),
                                it
                            )
                        }.apply {
                            sort()
                            forEach {
                                it.isRegistered = registeredSet.contains(it.id)
                            }
                        }
                    }
                }
            }
            result?.error?.let { activity.showToast(true, it) }
            if (owner == listOwner) showLists(owner, resultList)
        }
    }

    private fun setListOwner(a: SavedAccount?) {
        // リストオーナボタンの文字列を更新する
        if (a == null) {
            btnListOwner.setText(R.string.not_selected_2)
            btnListOwner.setTextColor(activity.attrColor(android.R.attr.textColorPrimary))
            btnListOwner.setBackgroundResource(R.drawable.btn_bg_transparent_round6dp)
        } else {
            val ac = daoAcctColor.load(a)
            btnListOwner.text = ac.nickname
            if (daoAcctColor.hasColorBackground(ac)) {
                btnListOwner.setBackgroundColor(ac.colorBg)
            } else {
                btnListOwner.setBackgroundResource(R.drawable.btn_bg_transparent_round6dp)
            }
            btnListOwner.setTextColor(
                ac.colorFg.notZero() ?: activity.attrColor(android.R.attr.textColorPrimary)
            )
        }
        // リスト一覧を取得する
        listOwner = a
        loadLists(a)
    }

    /**
     * リスト登録/解除処理を行い、失敗したらエラーメッセージを返す
     */

    private suspend fun handleRequest(request: OwnerListStatus): String? {
        val whoLocal = this@DlgListMember.whoLocal
            ?: return "target user is not synchronized"
        return try {
            when (request.isRegistered) {
                true -> activity.listMemberAdd(request.owner, request.list.id, whoLocal)
                else -> activity.listMemberDelete(request.owner, request.list.id, whoLocal)
            }
            null
        } catch (ex: Throwable) {
            log.e(ex, "listMemberAdd failed.")
            when (ex) {
                // チェック状態の巻き戻しは必要だが、トースト表示は必要ない
                is CancellationException, is MemberNotFollowedException -> ""
                else -> ex.message.notBlank() ?: ""
            }
        }
    }

    fun show() {
        btnClose.setOnClickListener { dialog.dismissSafe() }
        btnListOwner.setOnClickListener {
            launchMain {
                activity.pickAccount(
                    bAllowPseudo = false,
                    bAuto = false,
                    accountListArg = accountList
                )?.let { setListOwner(it) }
            }
        }
        rvOwnedlists.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = listsAdapter
        }

        ivUser.setImageUrl(
            calcIconRound(ivUser.layoutParams),
            who.avatar_static,
            who.avatar
        )
        tvUserAcct.text = whoAcct.pretty

        NetworkEmojiInvalidator(activity.handler, tvUserName)
            .text = who.decodeDisplayName(activity)

        dialog.apply {
            setTitle(R.string.your_lists)
            setContentView(root)
            setOnDismissListener { requestChannel.close() }
            window?.apply {
                setFlags(0, Window.FEATURE_NO_TITLE)
                setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
            }
            show()
        }
        setListOwner(listOwner)
    }
}

fun ActMain.openDlgListMember(
    who: TootAccount,
    whoAcct: Acct,
    initialOwner: SavedAccount,
) {
    if (!whoAcct.isValidFull) {
        showToast(true, "can't resolve user's full acct. $whoAcct")
    } else {
        DlgListMember(
            activity = this,
            who = who,
            whoAcct = whoAcct,
            listOwner = initialOwner.takeIf { !it.isPseudo },
        ).show()
    }
}
