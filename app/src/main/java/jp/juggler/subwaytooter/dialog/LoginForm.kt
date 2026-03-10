package jp.juggler.subwaytooter.dialog

import android.app.Dialog
import android.graphics.Color
import android.text.util.Linkify
import android.view.Gravity
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Filter
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import androidx.core.widget.TextViewCompat
import androidx.core.widget.addTextChangedListener
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.api.entity.Host
import jp.juggler.subwaytooter.api.entity.TootInstance
import jp.juggler.subwaytooter.api.getApiHostFromWebFinger
import jp.juggler.subwaytooter.api.runApiTask2
import jp.juggler.subwaytooter.util.DecodeOptions
import jp.juggler.subwaytooter.util.LinkHelper
import jp.juggler.util.coroutine.AppDispatchers
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.coroutine.launchMain
import jp.juggler.util.data.notBlank
import jp.juggler.util.data.notEmpty
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import jp.juggler.util.ui.ProgressDialogEx
import jp.juggler.util.ui.attrColor
import jp.juggler.util.ui.attrDrawable
import jp.juggler.util.ui.dismissSafe
import jp.juggler.util.ui.dp
import jp.juggler.util.ui.hideKeyboard
import jp.juggler.util.ui.invisible
import jp.juggler.util.ui.isEnabledAlpha
import jp.juggler.util.ui.resDrawable
import jp.juggler.util.ui.vg
import jp.juggler.util.ui.visible
import jp.juggler.util.ui.visibleOrInvisible
import jp.juggler.util.ui.wrapAndTint
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.IDN

class LoginForm(
    val activity: AppCompatActivity,
    val onClickOk: (
        dialog: Dialog,
        apiHost: Host,
        serverInfo: TootInstance?,
        action: Action,
    ) -> Unit,
) {
    companion object {
        private val log = LogCategory("LoginForm")

        @Suppress("RegExpSimplifiable")
        val reBadChars = """([^\p{L}\p{N}A-Za-z0-9:;._-]+)""".toRegex()

        fun AppCompatActivity.showLoginForm(
            onClickOk: (
                dialog: Dialog,
                apiHost: Host,
                serverInfo: TootInstance?,
                action: Action,
            ) -> Unit,
        ) = LoginForm(this, onClickOk)
    }

    enum class Action(
        @StringRes val idName: Int,
        @StringRes val idDesc: Int,
    ) {
        Login(R.string.existing_account, R.string.existing_account_desc),
        Pseudo(R.string.pseudo_account, R.string.pseudo_account_desc),

        //    Create(2, R.string.create_account, R.string.create_account_desc),
        Token(R.string.input_access_token, R.string.input_access_token_desc),
    }

    // 実行時キャストのためGenericsを含まない型を定義する
    private class StringArrayList : ArrayList<String>()

    private val matchParent = LinearLayout.LayoutParams.MATCH_PARENT
    private val wrapContent = LinearLayout.LayoutParams.WRAP_CONTENT

    private lateinit var tvHeader: TextView
    private lateinit var btnCancel: ImageButton
    private lateinit var llPageServerHost: LinearLayout
    private lateinit var etInstance: AutoCompleteTextView
    private lateinit var tvError: TextView
    private lateinit var btnNext: Button
    private lateinit var llPageAuthType: LinearLayout
    private lateinit var tvServerHost: TextView
    private lateinit var btnPrev: ImageButton
    private lateinit var tvServerDesc: TextView

    val dialog = Dialog(activity)

    private var targetServer: Host? = null
    private var targetServerInfo: TootInstance? = null

    private fun createViews() = ScrollView(activity).apply {
        layoutParams = LinearLayout.LayoutParams(matchParent, matchParent)
        isFillViewport = true
        isScrollbarFadingEnabled = false
        scrollBarStyle = ScrollView.SCROLLBARS_OUTSIDE_OVERLAY

        addView(LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(matchParent, wrapContent)

            // header row
            addView(LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                isBaselineAligned = false
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)

                tvHeader = TextView(activity).apply {
                    layoutParams = LinearLayout.LayoutParams(0, activity.dp(48), 1f).apply {
                        marginStart = activity.dp(12)
                    }
                    gravity = Gravity.CENTER_VERTICAL
                    includeFontPadding = false
                    textSize = 18f
                }
                addView(tvHeader)

                btnCancel = ImageButton(activity).apply {
                    layoutParams = LinearLayout.LayoutParams(activity.dp(48), activity.dp(48))
                    background = activity.resDrawable(R.drawable.btn_bg_transparent_round6dp)
                    contentDescription = activity.getString(R.string.cancel)
                    setImageDrawable(
                        activity.resDrawable(R.drawable.ic_close)
                            ?.wrapAndTint(activity.attrColor(R.attr.colorColumnHeaderName))
                    )
                }
                addView(btnCancel)
            })

            // page 1: input server name
            llPageServerHost = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent).apply {
                    setMargins(activity.dp(12), activity.dp(12), activity.dp(12), activity.dp(12))
                }

                etInstance = AppCompatAutoCompleteTextView(activity).apply {
                    layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
                    hint = activity.getString(R.string.instance_hint)
                    imeOptions = EditorInfo.IME_ACTION_DONE
                    includeFontPadding = false
                    inputType = EditorInfo.TYPE_TEXT_VARIATION_URI
                }
                addView(etInstance)

                addView(LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    isBaselineAligned = false
                    gravity = Gravity.TOP
                    layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)

                    tvError = TextView(activity).apply {
                        layoutParams = LinearLayout.LayoutParams(0, wrapContent, 1f)
                        gravity = Gravity.CENTER or Gravity.START
                        setTextColor(activity.attrColor(R.attr.colorRegexFilterError))
                    }
                    addView(tvError)

                    btnNext = Button(activity).apply {
                        layoutParams = LinearLayout.LayoutParams(wrapContent, wrapContent).apply {
                            gravity = Gravity.END
                        }
                        text = activity.getString(R.string.next_step)
                    }
                    addView(btnNext)
                })

                addView(TextView(activity).apply {
                    layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
                    autoLinkMask = Linkify.WEB_URLS
                    gravity = Gravity.TOP
                    text = activity.getString(R.string.input_server_name_desc)
                    setTextColor(activity.attrColor(R.attr.colorTextHelp))
                })
            }
            addView(llPageServerHost)

            // page 2: select action
            llPageAuthType = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent).apply {
                    setMargins(activity.dp(12), activity.dp(12), activity.dp(12), activity.dp(12))
                }

                addView(LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    isBaselineAligned = false
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)

                    tvServerHost = TextView(activity).apply {
                        layoutParams = LinearLayout.LayoutParams(0, activity.dp(50), 1f)
                        includeFontPadding = false
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        TextViewCompat.setAutoSizeTextTypeWithDefaults(
                            this,
                            TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM
                        )
                    }
                    addView(tvServerHost)

                    btnPrev = ImageButton(activity).apply {
                        layoutParams = LinearLayout.LayoutParams(activity.dp(48), activity.dp(48))
                        contentDescription = activity.getString(R.string.previous)
                        setImageDrawable(
                            activity.resDrawable(R.drawable.ic_edit)
                                ?.wrapAndTint(activity.attrColor(R.attr.colorColumnHeaderName))
                        )
                    }
                    addView(btnPrev)
                })

                tvServerDesc = TextView(activity).apply {
                    layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent).apply {
                        marginStart = activity.dp(16)
                    }
                    background = activity.attrDrawable(R.attr.colorColumnSettingBackground)
                    setPadding(activity.dp(2), activity.dp(2), activity.dp(2), activity.dp(2))
                    setTextColor(activity.attrColor(R.attr.colorTextContent))
                }
                addView(tvServerDesc)

                addView(TextView(activity).apply {
                    layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent).apply {
                        topMargin = activity.dp(16)
                    }
                    autoLinkMask = Linkify.WEB_URLS
                    text = activity.getString(R.string.authentication_select_desc)
                    setTextColor(activity.attrColor(R.attr.colorTextHelp))
                })
            }
            addView(llPageAuthType)
        })
    }

    init {
        val root = createViews()

        for (a in Action.entries) {
            val ll = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent).apply {
                    setPadding(0, activity.dp(12), 0, activity.dp(12))
                }
            }
            val btn = Button(activity).apply {
                layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
                isAllCaps = false
                setText(a.idName)
            }
            ll.addView(btn)
            val tv = TextView(activity).apply {
                layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent).apply {
                    marginStart = activity.dp(16)
                }
                autoLinkMask = Linkify.WEB_URLS
                setTextColor(activity.attrColor(R.attr.colorTextHelp))
                setText(a.idDesc)
            }
            ll.addView(tv)
            btn.setOnClickListener { onAuthTypeSelect(a) }
            llPageAuthType.addView(ll)
        }
        btnPrev.setOnClickListener { showPage(0) }
        btnNext.setOnClickListener { nextPage() }
        btnCancel.setOnClickListener { dialog.cancel() }
        etInstance.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                nextPage()
                return@OnEditorActionListener true
            }
            false
        })
        etInstance.addTextChangedListener { validateAndShow() }

        showPage(0)

        validateAndShow()

        dialog.setContentView(root)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.show()

        initServerNameList()
    }

    private fun initServerNameList() {
        val progress = ProgressDialogEx(activity)
        progress.setMessageEx(activity.getString(R.string.autocomplete_list_loading))
        progress.show()
        launchMain {
            try {
                val instanceList = HashSet<String>().apply {
                    try {
                        withContext(AppDispatchers.IO) {
                            activity.resources.openRawResource(R.raw.server_list).use { inStream ->
                                val br = BufferedReader(InputStreamReader(inStream, "UTF-8"))
                                while (true) {
                                    (br.readLine() ?: break)
                                        .trim { it <= ' ' }
                                        .notEmpty()
                                        ?.lowercase()
                                        ?.let {
                                            add(it)
                                            add(IDN.toASCII(it, IDN.ALLOW_UNASSIGNED))
                                            add(IDN.toUnicode(it, IDN.ALLOW_UNASSIGNED))
                                        }
                                }
                            }
                        }
                    } catch (ex: Throwable) {
                        log.e(ex, "can't load server list.")
                    }
                }.toList().sorted()

                val adapter = object : ArrayAdapter<String>(
                    activity, android.R.layout.simple_spinner_dropdown_item, ArrayList()
                ) {
                    override fun getFilter(): Filter = nameFilter

                    val nameFilter: Filter = object : Filter() {
                        override fun convertResultToString(value: Any) =
                            value as String

                        override fun performFiltering(constraint: CharSequence?) =
                            FilterResults().also { result ->
                                constraint?.notEmpty()?.toString()?.lowercase()?.let { key ->
                                    // suggestions リストは毎回生成する必要がある。publishResultsと同時にアクセスされる場合がある
                                    val suggestions = StringArrayList()
                                    for (s in instanceList) {
                                        if (s.contains(key)) {
                                            suggestions.add(s)
                                            if (suggestions.size >= 20) break
                                        }
                                    }
                                    result.values = suggestions
                                    result.count = suggestions.size
                                }
                            }

                        override fun publishResults(
                            constraint: CharSequence?,
                            results: FilterResults?,
                        ) {
                            clear()
                            (results?.values as? StringArrayList)?.let { addAll(it) }
                            notifyDataSetChanged()
                        }
                    }
                }
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                etInstance.setAdapter<ArrayAdapter<String>>(adapter)
            } catch (ex: Throwable) {
                activity.showToast(ex, "initServerNameList failed.")
            } finally {
                progress.dismissSafe()
            }
        }
    }

    // return validated name. else null
    private fun validateAndShow(): String? {
        fun showError(s: String) {
            btnNext.isEnabledAlpha = false
            tvError.visible().text = s
        }

        val s = etInstance.text.toString().trim()
        if (s.isEmpty()) {
            showError(activity.getString(R.string.instance_not_specified))
            return null
        }

        // コピペミスに合わせたガイド
        arrayOf(
            "http://",
            "https://",
        ).forEach {
            if (s.contains(it)) {
                showError(activity.getString(R.string.server_host_name_cant_contains_it, it))
                return null
            }
        }
        if (s.contains("/") || s.contains("@")) {
            showError(activity.getString(R.string.instance_not_need_slash))
            return null
        }

        reBadChars.findAll(s).joinToString("") { it.value }.notEmpty()?.let {
            showError(activity.getString(R.string.server_host_name_cant_contains_it, it))
            return null
        }
        tvError.invisible()
        btnNext.isEnabledAlpha = true
        return s
    }

    private fun showPage(n: Int) {
        etInstance.dismissDropDown()
        etInstance.hideKeyboard()
        llPageServerHost.vg(n == 0)
        llPageAuthType.vg(n == 1)
        val canBack = n != 0
        btnPrev.vg(canBack)
        val canNext = n == 0
        btnNext.visibleOrInvisible(canNext)
        tvHeader.setText(when (n) {
            0 -> R.string.server_host_name
            else -> R.string.authentication_select
        })
    }

    private fun nextPage() {
        activity.run {
            launchAndShowError {
                var host = Host.parse(validateAndShow() ?: return@launchAndShowError)
                var error: String? = null
                val tootInstance = runApiTask2(host) { client ->
                    try {
                        // ユーザの入力がホスト名かドメイン名かは分からない。
                        // WebFingerでホストを調べる
                        client.getApiHostFromWebFinger(host)?.let {
                            if (it != host) {
                                host = it
                                client.apiHost = it
                            }
                        }

                        // サーバ情報を読む
                        TootInstance.getExOrThrow(client, forceUpdate = true)
                    } catch (ex: Throwable) {
                        error = ex.message
                        null
                    }
                }
                if (isDestroyed || isFinishing) return@launchAndShowError
                targetServer = host
                targetServerInfo = tootInstance
                tvServerHost.text = tootInstance?.apDomain?.pretty ?: host.pretty
                tvServerDesc.run {
                    when (tootInstance) {
                        null -> {
                            setTextColor(attrColor(R.attr.colorRegexFilterError))
                            text = error
                        }

                        else -> {
                            setTextColor(attrColor(R.attr.colorTextContent))
                            text = (tootInstance.description.notBlank()
                                ?: tootInstance.descriptionOld.notBlank()
                                ?: "(empty server description)"
                                    ).let {
                                    DecodeOptions(
                                        applicationContext,
                                        LinkHelper.create(tootInstance),
                                        forceHtml = true,
                                        short = true,
                                    ).decodeHTML(it)
                                }.replace("""\n[\s\n]+""".toRegex(), "\n")
                                .trim()
                        }
                    }
                }

                showPage(1)
            }
        }
    }

    private fun onAuthTypeSelect(action: Action) {
        targetServer?.let { onClickOk(dialog, it, targetServerInfo, action) }
    }
}
