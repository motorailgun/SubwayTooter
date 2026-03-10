package jp.juggler.subwaytooter

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import jp.juggler.subwaytooter.api.entity.NotificationType.Companion.toNotificationType
import jp.juggler.subwaytooter.dialog.actionsDialog
import jp.juggler.subwaytooter.dialog.runInProgress
import jp.juggler.subwaytooter.push.PushMessageIconColor
import jp.juggler.subwaytooter.push.pushMessageIconAndColor
import jp.juggler.subwaytooter.push.pushRepo
import jp.juggler.subwaytooter.table.PushMessage
import jp.juggler.subwaytooter.table.daoAccountNotificationStatus
import jp.juggler.subwaytooter.table.daoPushMessage
import jp.juggler.subwaytooter.table.daoSavedAccount
import jp.juggler.subwaytooter.util.permissionSpecNotification
import jp.juggler.subwaytooter.util.requester
import jp.juggler.subwaytooter.view.wrapTitleTextView
import jp.juggler.util.coroutine.AppDispatchers
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.data.encodeBase64Url
import jp.juggler.util.data.notBlank
import jp.juggler.util.data.notZero
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.dialogOrToast
import jp.juggler.util.os.saveToDownload
import jp.juggler.util.time.formatLocalTime
import jp.juggler.util.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.PrintWriter

class ActPushMessageList : AppCompatActivity() {
    companion object {
        private val log = LogCategory("ActPushMessageList")
    }

    private lateinit var toolbar: Toolbar
    private lateinit var rvMessages: RecyclerView
    private lateinit var rootView: LinearLayout

    private val listAdapter = MyAdapter()

    private val layoutManager by lazy {
        LinearLayoutManager(this)
    }

    private val prNotification = permissionSpecNotification.requester {
        // 特に何もしない
    }
    private val acctMap by lazy {
        daoSavedAccount.loadRealAccounts().associateBy { it.acct }
    }

    private fun createViews() {
        val tv = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)

        toolbar = Toolbar(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(tv.resourceId)
            )
            setBackgroundResource(R.drawable.action_bar_bg)
            elevation = dpFloat(4)
        }

        rvMessages = RecyclerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1f
            )
            setBackgroundColor(attrColor(R.attr.colorMainBackground))
            isScrollbarFadingEnabled = false
            scrollBarStyle = RecyclerView.SCROLLBARS_OUTSIDE_OVERLAY
        }

        rootView = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            addView(toolbar)
            addView(rvMessages)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prNotification.register(this)
        prNotification.checkOrLaunch()
        super.onCreate(savedInstanceState)
        App1.setActivityTheme(this)
        createViews()
        setContentViewAndInsets(rootView)
        setSupportActionBar(toolbar)
        wrapTitleTextView()
        setNavigationBack(toolbar)

        rvMessages.also {
            val dividerItemDecoration = DividerItemDecoration(
                this,
                LinearLayout.VERTICAL,
            )
            it.addItemDecoration(dividerItemDecoration)
            it.adapter = listAdapter
            it.layoutManager = layoutManager
        }

        lifecycleScope.launch {
            PushMessage.flowDataChanged.collect {
                try {
                    listAdapter.items = withContext(AppDispatchers.IO) {
                        daoPushMessage.listAll()
                    }
                } catch (ex: Throwable) {
                    log.e(ex, "load failed.")
                }
            }
        }
    }

    fun itemActions(pm: PushMessage) {
        launchAndShowError {
            actionsDialog {
                action(getString(R.string.push_message_re_decode)) {
                    pushRepo.reprocess(pm)
                }
                action(getString(R.string.push_message_save_to_download_folder)) {
                    export(pm)
                }
                action(getString(R.string.push_message_save_to_download_folder_with_secret_key)) {
                    export(pm, exportKeys = true)
                }
            }
        }
    }

    /**
     * エクスポート、というか端末のダウンロードフォルダに保存する
     */
    private suspend fun export(pm: PushMessage, exportKeys: Boolean = false) {
        val path = runInProgress {
            withContext(AppDispatchers.DEFAULT) {
                saveToDownload(
                    displayName = "PushMessageDump-${pm.id}.txt",
                ) { PrintWriter(it).apply { dumpMessage(pm, exportKeys) }.flush() }
            }
        }
        if (!path.isNullOrEmpty()) {
            dialogOrToast(R.string.saved_to, path)
        }
    }

    private fun PrintWriter.dumpMessage(pm: PushMessage, exportKeys: Boolean) {
        println("timestamp: ${pm.timestamp.formatLocalTime()}")
        println("timeSave: ${pm.timeSave.formatLocalTime()}")
        println("timeDismiss: ${pm.timeDismiss.formatLocalTime()}")
        println("to: ${pm.loginAcct}")
        println("type: ${pm.notificationType}")
        println("id: ${pm.notificationId}")
        println("text: ${pm.textExpand}")
        println("formatJson=${pm.formatJson.toString(1, sort = true)}")
        println("messageJson=${pm.messageJson?.toString(1, sort = true)}")
        println("dataSize: ${pm.rawBody?.size}")
        if (exportKeys) {
            val acct = pm.loginAcct
            if (acct == null) {
                println("!!secret key is not exported because missing recepients acct.")
            } else {
                val status = daoAccountNotificationStatus.load(acct)
                if (status == null) {
                    println("!!secret key is not exported because missing status for acct $acct .")
                } else {
                    println("receiverPrivateBytes=${status.pushKeyPrivate?.encodeBase64Url()}")
                    println("receiverPublicBytes=${status.pushKeyPublic?.encodeBase64Url()}")
                    println("senderPublicBytes=${status.pushServerKey?.encodeBase64Url()}")
                    println("authSecret=${status.pushAuthSecret?.encodeBase64Url()}")
                }
            }
        }
        println("headerJson=${pm.headerJson}")
        println("rawBody=${pm.rawBody?.encodeBase64Url()}")
    }

    private val tintIconMap = HashMap<String, Drawable>()

    fun tintIcon(pm: PushMessage, ic: PushMessageIconColor) =
        tintIconMap.getOrPut("${ic.name}-${pm.loginAcct}") {
            val context = this
            val a = acctMap[pm.loginAcct]
            val c = ic.colorRes.notZero()?.let { ContextCompat.getColor(context, it) }
                ?: a?.notificationAccentColor?.notZero()
                ?: ContextCompat.getColor(this, R.color.colorOsNotificationAccent)

            context.resDrawable( ic.iconId).wrapAndTint(color = c)
        }

    @SuppressLint("SetTextI18n")
    private inner class MyViewHolder(
        parent: ViewGroup,
    ) : RecyclerView.ViewHolder(
        LinearLayout(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            isBaselineAligned = false
            val pad4 = dp(4)
            val pad12 = dp(12)
            setPadding(pad12, pad4, pad12, pad4)
        }
    ) {
        val ivSmall: ImageView
        val ivLarge: ImageView
        val tvText: TextView

        init {
            val context = parent.context
            val size48 = context.dp(48)

            val iconColumn = LinearLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.VERTICAL
            }
            ivSmall = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(size48, size48)
                importantForAccessibility = ImageView.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            ivLarge = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(size48, size48)
                importantForAccessibility = ImageView.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            iconColumn.addView(ivSmall)
            iconColumn.addView(ivLarge)

            val pad12 = context.dp(12)
            val pad4 = context.dp(4)
            tvText = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                gravity = Gravity.CENTER_VERTICAL
                minimumHeight = context.dp(40)
                setPadding(pad12, pad4, pad12, pad4)
            }

            (itemView as LinearLayout).addView(iconColumn)
            (itemView as LinearLayout).addView(tvText)

            itemView.setOnClickListener { lastItem?.let { itemActions(it) } }
        }

        var lastItem: PushMessage? = null

        fun bind(pm: PushMessage?) {
            pm ?: return
            lastItem = pm
            val type = pm.notificationType?.toNotificationType()
            val iconColor = type.pushMessageIconAndColor()

            Glide.with(ivSmall)
                .load(pm.iconSmall)
                .error(tintIcon(pm, iconColor))
                .into(ivSmall)

            Glide.with(ivLarge)
                .load(pm.iconLarge)
                .into(ivLarge)

            tvText.text = arrayOf(
                "when: ${pm.timestamp.formatLocalTime()}",
                pm.timeDismiss.takeIf { it > 0L }?.let { "既読: ${it.formatLocalTime()}" },
                "to: ${pm.loginAcct}",
                "type: ${pm.notificationType}",
                "id: ${pm.notificationId}",
                "dataSize: ${pm.rawBody?.size}",
                pm.textExpand,
                pm.formatError?.let { "error: $it" },
            ).mapNotNull { it.notBlank() }.joinToString("\n")
        }
    }

    private inner class MyAdapter : RecyclerView.Adapter<MyViewHolder>() {
        var items: List<PushMessage> = emptyList()
            set(value) {
                val oldScrollPos = layoutManager.findFirstVisibleItemPosition()
                    .takeIf { it != RecyclerView.NO_POSITION }
                val oldItems = field
                field = value
                DiffUtil.calculateDiff(
                    object : DiffUtil.Callback() {
                        override fun getOldListSize() = oldItems.size
                        override fun getNewListSize() = value.size

                        override fun areItemsTheSame(
                            oldItemPosition: Int,
                            newItemPosition: Int,
                        ) = oldItems[oldItemPosition] == value[newItemPosition]

                        override fun areContentsTheSame(
                            oldItemPosition: Int,
                            newItemPosition: Int,
                        ) = false
                    },
                    true
                ).dispatchUpdatesTo(this)
                if (oldScrollPos == 0) {
                    launchAndShowError {
                        delay(50L)
                        rvMessages.smoothScrollToPosition(0)
                    }
                }
            }

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MyViewHolder(parent)

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.bind(items.elementAtOrNull(position))
        }
    }
}
