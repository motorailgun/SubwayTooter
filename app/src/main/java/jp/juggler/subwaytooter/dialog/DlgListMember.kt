package jp.juggler.subwaytooter.dialog

import android.app.Dialog
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.action.*
import jp.juggler.subwaytooter.api.*
import jp.juggler.subwaytooter.api.entity.*
import jp.juggler.subwaytooter.compose.NetworkImage
import jp.juggler.subwaytooter.compose.SpannableTextView
import jp.juggler.subwaytooter.compose.StThemedContent
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.subwaytooter.table.accountListNonPseudo
import jp.juggler.subwaytooter.table.daoAcctColor
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
}

/**
 * リストメンバー管理ダイアログの状態
 */
class DlgListMember(
    private val activity: ActMain,
    private val who: TootAccount,
    private val whoAcct: Acct,
    initialOwner: SavedAccount?,
) {
    private val accountList = accountListNonPseudo(null)
    private var whoLocal: TootAccount? = null
    private var listOwner: SavedAccount? = initialOwner

    // Compose observable state
    private val itemsState = mutableStateOf(emptyList<Any>())
    private val listOwnerState = mutableStateOf(initialOwner)

    private val requestChannel = Channel<OwnerListStatus>(capacity = Channel.UNLIMITED).apply {
        EmptyScope.launch {
            while (true) {
                val request = receiveCatching().getOrNull()
                    ?: break
                when (val error = handleRequest(request)) {
                    null -> Unit
                    else -> {
                        error.notEmpty()?.let { activity.showToast(true, it) }
                        updateListItem(request, !request.isRegistered)
                    }
                }
            }
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

    private fun updateListItem(idSample: OwnerListStatus, newRegistered: Boolean) {
        itemsState.value = itemsState.value.map {
            when {
                it is OwnerListStatus && it == idSample ->
                    it.copy(isRegistered = newRegistered)

                else -> it
            }
        }
    }

    private fun handleCheckChange(data: OwnerListStatus) = EmptyScope.launch {
        try {
            updateListItem(data, data.isRegistered)
            requestChannel.send(data)
        } catch (ex: Throwable) {
            log.e(ex, "handleCheckChange failed.")
        }
    }

    private fun showLists(owner: SavedAccount?, srcList: ArrayList<TootList>?) {
        with(activity) {
            itemsState.value = buildList {
                when {
                    owner == null || srcList == null ->
                        add(getString(R.string.cant_access_list))

                    else -> {
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
        }
    }

    private fun loadLists(owner: SavedAccount?) {
        if (owner == null) {
            showLists(null, null)
            return
        }
        launchMain {
            var resultList: ArrayList<TootList>? = null
            val result = activity.runApiTask(owner) { client ->
                val (r1, ar) = client.syncAccountByAcct(owner, whoAcct)
                r1 ?: return@runApiTask null

                val whoLocal = ar?.get()
                this@DlgListMember.whoLocal = whoLocal

                if (whoLocal == null) activity.showToast(true, r1.error)

                if (owner.isMisskey) {
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
        listOwner = a
        listOwnerState.value = a
        loadLists(a)
    }

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
                is CancellationException, is MemberNotFollowedException -> ""
                else -> ex.message.notBlank() ?: ""
            }
        }
    }

    fun show() {
        val dialog = Dialog(activity)
        val act = this.activity
        val displayName = who.decodeDisplayName(act)
        val actHandler = act.handler
        val composeView = ComposeView(act).apply {
            setContent {
                StThemedContent {
                    DlgListMemberContent(
                        who = who,
                        whoAcct = whoAcct,
                        displayName = displayName,
                        items = itemsState.value,
                        listOwner = listOwnerState.value,
                        handler = actHandler,
                        onPickOwner = {
                            launchMain {
                                act.pickAccount(
                                    bAllowPseudo = false,
                                    bAuto = false,
                                    accountListArg = accountList
                                )?.let { setListOwner(it) }
                            }
                        },
                        onCheckChange = { data ->
                            handleCheckChange(data)
                        },
                        onCreate = ::openListCreator,
                        onClose = { dialog.dismissSafe() },
                    )
                }
            }
        }
        dialog.setContentView(composeView)
        dialog.setOnDismissListener { requestChannel.close() }
        dialog.window?.apply {
            setFlags(0, Window.FEATURE_NO_TITLE)
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }
        dialog.show()
        setListOwner(listOwner)
    }
}

@Composable
private fun DlgListMemberContent(
    who: TootAccount,
    whoAcct: Acct,
    displayName: CharSequence,
    items: List<Any>,
    listOwner: SavedAccount?,
    handler: android.os.Handler,
    onPickOwner: () -> Unit,
    onCheckChange: (OwnerListStatus) -> Unit,
    onCreate: () -> Unit,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(6.dp))

        // "Target user" section
        Text(
            text = stringResource(R.string.target_user),
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NetworkImage(
                modifier = Modifier.size(48.dp, 40.dp),
                cornerRadius = 0f,
                staticUrl = who.avatar_static,
                animatedUrl = who.avatar,
                contentDescription = null,
                scaleType = ImageView.ScaleType.FIT_END,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                SpannableTextView(
                    text = displayName,
                    handler = handler,
                )
                Text(
                    text = whoAcct.pretty,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 6.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        // "List owner" section
        Text(
            text = stringResource(R.string.list_owner),
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        ListOwnerButton(
            listOwner = listOwner,
            onClick = onPickOwner,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 3.dp),
        )

        // "List" section
        Text(
            text = stringResource(R.string.list),
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            items(
                items = items,
                key = { item ->
                    when (item) {
                        is OwnerListStatus -> "list:${item.owner.db_id}:${item.list.id}"
                        is Double -> "create"
                        else -> "msg:$item"
                    }
                },
            ) { item ->
                when (item) {
                    is OwnerListStatus -> ListCheckboxRow(
                        data = item,
                        onCheckChange = onCheckChange,
                    )

                    is Double -> Button(
                        onClick = onCreate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                    ) {
                        Text(stringResource(R.string.list_create))
                    }

                    else -> Text(
                        text = item.toString(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
        }

        // Close button bar
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            TextButton(
                onClick = onClose,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.close))
            }
        }
    }
}

@Composable
private fun ListOwnerButton(
    listOwner: SavedAccount?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (listOwner == null) {
        Button(
            onClick = onClick,
            modifier = modifier,
        ) {
            Text(stringResource(R.string.not_selected_2))
        }
    } else {
        val ac = daoAcctColor.load(listOwner)
        val hasBg = daoAcctColor.hasColorBackground(ac)
        val bgColor = if (hasBg) Color(ac.colorBg) else Color.Transparent
        val fgColor = ac.colorFg.notZero()?.let { Color(it) }
            ?: MaterialTheme.colorScheme.onSurface
        Button(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = bgColor,
                contentColor = fgColor,
            ),
        ) {
            Text(ac.nickname)
        }
    }
}

@Composable
private fun ListCheckboxRow(
    data: OwnerListStatus,
    onCheckChange: (OwnerListStatus) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = data.isRegistered,
            onCheckedChange = { checked ->
                onCheckChange(data.copy(isRegistered = checked))
            },
        )
        Text(
            text = data.list.title ?: "",
            modifier = Modifier.weight(1f),
        )
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
            initialOwner = initialOwner.takeIf { !it.isPseudo },
        ).show()
    }
}
