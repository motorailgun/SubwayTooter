package jp.juggler.subwaytooter

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.jrummyapps.android.colorpicker.dialogColorPicker
import jp.juggler.subwaytooter.api.TootApiResult
import jp.juggler.subwaytooter.api.runApiTask
import jp.juggler.subwaytooter.column.Column
import jp.juggler.subwaytooter.column.getAcctColor
import jp.juggler.subwaytooter.column.getBackgroundImageDir
import jp.juggler.subwaytooter.column.getColumnName
import jp.juggler.subwaytooter.column.getContentColor
import jp.juggler.subwaytooter.column.getHeaderBackgroundColor
import jp.juggler.subwaytooter.column.getHeaderNameColor
import jp.juggler.subwaytooter.column.getIconId
import jp.juggler.subwaytooter.column.setHeaderBackground
import jp.juggler.subwaytooter.view.MyTextView
import jp.juggler.subwaytooter.view.wrapTitleTextView
import jp.juggler.util.backPressed
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.coroutine.launchMain
import jp.juggler.util.data.checkMimeTypeAndGrant
import jp.juggler.util.data.defaultLocale
import jp.juggler.util.data.intentGetContent
import jp.juggler.util.data.mayUri
import jp.juggler.util.data.notZero
import jp.juggler.util.int
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import jp.juggler.util.log.withCaption
import jp.juggler.util.media.createResizedBitmap
import jp.juggler.util.ui.*
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import kotlin.math.max

class ActColumnCustomize : AppCompatActivity() {

    companion object {

        internal val log = LogCategory("ActColumnCustomize")

        internal const val EXTRA_COLUMN_INDEX = "column_index"

        internal const val PROGRESS_MAX = 65536

        fun createIntent(activity: ActMain, idx: Int) =
            Intent(activity, ActColumnCustomize::class.java).apply {
                putExtra(EXTRA_COLUMN_INDEX, idx)
            }
    }

    private var columnIndex: Int = 0
    internal lateinit var column: Column
    internal lateinit var appState: AppState
    internal var density: Float = 0f

    private lateinit var toolbar: Toolbar
    private lateinit var svContent: ScrollView
    private lateinit var llColumnHeader: LinearLayout
    private lateinit var ivColumnHeader: ImageView
    private lateinit var tvColumnName: TextView
    private lateinit var btnHeaderBackgroundEdit: Button
    private lateinit var btnHeaderBackgroundReset: Button
    private lateinit var btnHeaderTextEdit: Button
    private lateinit var btnHeaderTextReset: Button
    private lateinit var btnColumnBackgroundColor: Button
    private lateinit var btnColumnBackgroundColorReset: Button
    private lateinit var btnColumnBackgroundImage: Button
    private lateinit var btnColumnBackgroundImageReset: Button
    private lateinit var btnAcctColor: Button
    private lateinit var btnAcctColorReset: Button
    private lateinit var btnContentColor: Button
    private lateinit var btnContentColorReset: Button
    private lateinit var flColumnBackground: FrameLayout
    private lateinit var ivColumnBackground: ImageView
    private lateinit var tvSampleAcct: TextView
    private lateinit var tvSampleContent: MyTextView
    private lateinit var etAlpha: EditText
    private lateinit var sbColumnBackgroundAlpha: SeekBar
    private lateinit var tvBackgroundError: TextView
    private lateinit var rootView: LinearLayout

    internal var loadingBusy: Boolean = false

    private var lastImageUri: String? = null
    private var lastImageBitmap: Bitmap? = null

    private val arColumnBackgroundImage = ActivityResultHandler(log) { r ->
        if (r.isNotOk) return@ActivityResultHandler
        r.data?.checkMimeTypeAndGrant(contentResolver)
            ?.firstOrNull()?.uri?.let { updateBackground(it) }
    }

    private fun makeResult() {
        val data = Intent()
        data.putExtra(EXTRA_COLUMN_INDEX, columnIndex)
        setResult(RESULT_OK, data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backPressed {
            makeResult()
            finish()
        }
        arColumnBackgroundImage.register(this)
        App1.setActivityTheme(this)
        createViews()
        setContentViewAndInsets(rootView)
        initUI()

        appState = App1.getAppState(this)
        density = appState.density
        columnIndex = intent.int(EXTRA_COLUMN_INDEX) ?: 0
        column = appState.column(columnIndex)!!
        show()
    }

    override fun onDestroy() {
        closeBitmaps()
        super.onDestroy()
    }

    private fun updateBackground(uriArg: Uri) {
        launchMain {
            var resultUri: String? = null
            runApiTask { client ->
                try {
                    val backgroundDir = getBackgroundImageDir(this@ActColumnCustomize)
                    val file =
                        File(backgroundDir, "${column.columnId}:${System.currentTimeMillis()}")
                    val fileUri = Uri.fromFile(file)

                    client.publishApiProgress("loading image from $uriArg")
                    contentResolver.openInputStream(uriArg)?.use { inStream ->
                        FileOutputStream(file).use { outStream ->
                            inStream.copyTo(outStream)
                        }
                    }

                    // リサイズや回転が必要ならする
                    client.publishApiProgress("check resize/rotation…")

                    val size = (max(
                        resources.displayMetrics.widthPixels,
                        resources.displayMetrics.heightPixels
                    ) * 1.5f).toInt()

                    val bitmap = createResizedBitmap(
                        this,
                        fileUri,
                        size,
                        skipIfNoNeedToResizeAndRotate = true,
                    )
                    if (bitmap != null) {
                        try {
                            client.publishApiProgress("save resized(${bitmap.width}x${bitmap.height}) image to $file")
                            FileOutputStream(file).use { os ->
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                            }
                        } finally {
                            bitmap.recycle()
                        }
                    }

                    resultUri = fileUri.toString()
                    TootApiResult()
                } catch (ex: Throwable) {
                    log.e(ex, "can't update background image.")
                    TootApiResult(ex.withCaption("can't update background image."))
                }
            }?.let { result ->
                when (val bgUri = resultUri) {
                    null -> showToast(true, result.error ?: "?")
                    else -> {
                        column.columnBgImage = bgUri
                        show()
                    }
                }
            }
        }
    }

    private fun createViews() {
        val pad6 = dp(6)
        val pad12 = dp(12)
        val pad3 = dp(3)
        val pad32 = dp(32)
        val size32 = dp(32)
        val size48 = dp(48)

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

        fun makeDivider() = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
            ).also {
                it.topMargin = pad12
                it.bottomMargin = pad12
            }
            setBackgroundColor(attrColor(R.attr.colorSettingDivider))
        }

        fun makeLabel(textRes: Int, indent: Boolean = false) = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                if (indent) it.topMargin = pad3
            }
            if (indent) {
                setPadding(size48, 0, 0, 0)
            }
            textSize = 14f
            setText(textRes)
        }

        fun makeFormRow() = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.marginStart = pad32
            }
            orientation = LinearLayout.HORIZONTAL
            isBaselineAligned = true
        }

        fun makeButton(textRes: Int) = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setText(textRes)
            isAllCaps = false
        }

        // Column Header section
        ivColumnHeader = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(size32, size32).also {
                it.marginEnd = dp(4)
            }
            importantForAccessibility = ImageView.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        tvColumnName = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        llColumnHeader = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = pad6 }
            gravity = Gravity.CENTER_VERTICAL
            setPadding(pad12, pad3, pad12, pad3)
            addView(ivColumnHeader)
            addView(tvColumnName)
        }

        btnHeaderBackgroundEdit = makeButton(R.string.edit)
        btnHeaderBackgroundReset = makeButton(R.string.reset)
        btnHeaderTextEdit = makeButton(R.string.edit)
        btnHeaderTextReset = makeButton(R.string.reset)

        // Column section
        ivColumnBackground = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            importantForAccessibility = ImageView.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        tvSampleAcct = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            ellipsize = android.text.TextUtils.TruncateAt.END
            gravity = Gravity.START
            maxLines = 1
            setText(R.string.acct_sample)
            setTextColor(attrColor(R.attr.colorTimeSmall))
            textSize = 12f
        }

        tvSampleContent = MyTextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = pad3 }
            gravity = Gravity.START
            setLineSpacing(0f, 1.1f)
            setText(R.string.content_sample)
            setTextColor(attrColor(R.attr.colorTextContent))
        }

        val sampleContent = LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(pad12, pad12, pad12, pad12)
            addView(tvSampleAcct)
            addView(tvSampleContent)
        }

        flColumnBackground = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            addView(ivColumnBackground)
            addView(sampleContent)
        }

        btnColumnBackgroundColor = makeButton(R.string.edit)
        btnColumnBackgroundColorReset = makeButton(R.string.reset)
        btnColumnBackgroundImage = makeButton(R.string.pick_image)
        btnColumnBackgroundImageReset = makeButton(R.string.reset)

        etAlpha = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.marginEnd = dp(4) }
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            maxLines = 1
            minLines = 1
            imeOptions = EditorInfo.IME_ACTION_DONE
            minWidth = dp(64)
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        }

        sbColumnBackgroundAlpha = SeekBar(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, size48, 1f)
            setPadding(pad32, 0, pad32, 0)
        }

        tvBackgroundError = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.marginStart = pad32 }
            setTextColor(attrColor(R.attr.colorRegexFilterError))
            visibility = View.GONE
        }

        btnAcctColor = makeButton(R.string.edit)
        btnAcctColorReset = makeButton(R.string.reset)
        btnContentColor = makeButton(R.string.edit)
        btnContentColorReset = makeButton(R.string.reset)

        // Build the scroll content
        val scrollContent = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(pad12, pad6, pad12, pad6)

            addView(makeDivider())
            addView(makeLabel(R.string.column_header))
            addView(makeFormRow().apply {
                addView(llColumnHeader)
            })
            addView(makeLabel(R.string.background_color, indent = true))
            addView(makeFormRow().apply {
                addView(btnHeaderBackgroundEdit)
                addView(btnHeaderBackgroundReset)
            })
            addView(makeLabel(R.string.foreground_color, indent = true))
            addView(makeFormRow().apply {
                addView(btnHeaderTextEdit)
                addView(btnHeaderTextReset)
            })
            addView(makeDivider())
            addView(makeLabel(R.string.column))
            addView(makeFormRow().apply {
                (layoutParams as LinearLayout.LayoutParams).bottomMargin = pad6
                orientation = LinearLayout.VERTICAL
                addView(flColumnBackground)
            })
            addView(makeFormRow().apply { addView(makeLabel(R.string.background_color)) })
            addView(makeFormRow().apply {
                addView(btnColumnBackgroundColor)
                addView(btnColumnBackgroundColorReset)
            })
            addView(makeFormRow().apply { addView(makeLabel(R.string.background_image)) })
            addView(makeFormRow().apply {
                addView(btnColumnBackgroundImage)
                addView(btnColumnBackgroundImageReset)
            })
            addView(makeFormRow().apply { addView(makeLabel(R.string.background_image_alpha)) })
            addView(makeFormRow().apply {
                layoutParams = (layoutParams as LinearLayout.LayoutParams).also {
                    it.height = size48
                }
                gravity = Gravity.CENTER_VERTICAL
                isBaselineAligned = false
                addView(etAlpha)
                addView(sbColumnBackgroundAlpha)
            })
            addView(tvBackgroundError)
            addView(makeFormRow().apply { addView(makeLabel(R.string.acct_color)) })
            addView(makeFormRow().apply {
                addView(btnAcctColor)
                addView(btnAcctColorReset)
            })
            addView(makeFormRow().apply { addView(makeLabel(R.string.content_color)) })
            addView(makeFormRow().apply {
                addView(btnContentColor)
                addView(btnContentColorReset)
            })
            addView(makeDivider())
        }

        svContent = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1f
            )
            setBackgroundColor(attrColor(R.attr.colorMainBackground))
            isFillViewport = true
            isScrollbarFadingEnabled = false
            scrollBarStyle = ScrollView.SCROLLBARS_OUTSIDE_OVERLAY
            addView(scrollContent)
        }

        rootView = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            addView(toolbar)
            addView(svContent)
        }
    }

    private fun initUI() {
        setSupportActionBar(toolbar)
        wrapTitleTextView()
        setNavigationBack(toolbar)
        fixHorizontalMargin(svContent)

        btnHeaderBackgroundEdit.setOnClickListener {
            launchAndShowError {
                column.headerBgColor = Color.BLACK or dialogColorPicker(
                    colorInitial = column.getHeaderBackgroundColor(),
                    alphaEnabled = false,
                )
            }
        }
        btnHeaderBackgroundReset.setOnClickListener {
            column.headerBgColor = 0
            show()
        }
        btnHeaderTextEdit.setOnClickListener {
            launchAndShowError {
                column.headerFgColor = Color.BLACK or dialogColorPicker(
                    colorInitial = column.getHeaderNameColor(),
                    alphaEnabled = false,
                )
            }
        }
        btnHeaderTextReset.setOnClickListener {
            column.headerFgColor = 0
            show()
        }
        btnColumnBackgroundColor.setOnClickListener {
            launchAndShowError {
                column.columnBgColor = Color.BLACK or dialogColorPicker(
                    colorInitial = column.columnBgColor.notZero(),
                    alphaEnabled = false,
                )
            }
        }
        btnColumnBackgroundColorReset.setOnClickListener {
            column.columnBgColor = 0
            show()
        }
        btnAcctColor.setOnClickListener {
            launchAndShowError {
                column.acctColor = dialogColorPicker(
                    colorInitial = column.getAcctColor(),
                    alphaEnabled = true,
                ).notZero() ?: 1
            }
        }
        btnAcctColorReset.setOnClickListener {
            column.acctColor = 0
            show()
        }
        btnContentColor.setOnClickListener {
            launchAndShowError {
                column.contentColor = dialogColorPicker(
                    colorInitial = column.getContentColor(),
                    alphaEnabled = true,
                ).notZero() ?: 1
            }
        }
        btnContentColorReset.setOnClickListener {
            column.contentColor = 0
            show()
        }
        btnColumnBackgroundImage.setOnClickListener {
            val intent = intentGetContent(
                false,
                getString(R.string.pick_image),
                arrayOf("image/*")
            )
            arColumnBackgroundImage.launch(intent)
        }
        btnColumnBackgroundImageReset.setOnClickListener {
            column.columnBgImage = ""
            show()
        }

        sbColumnBackgroundAlpha.max = PROGRESS_MAX

        sbColumnBackgroundAlpha.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (loadingBusy) return
                if (!fromUser) return
                column.columnBgImageAlpha = progress / PROGRESS_MAX.toFloat()
                showAlpha(updateText = true, updateSeek = false)
            }
        })

        etAlpha.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                if (loadingBusy) return
                try {
                    var f = NumberFormat.getInstance(defaultLocale(this@ActColumnCustomize))
                        .parse(etAlpha.text.toString())?.toFloat()
                    if (f != null && !f.isNaN()) {
                        if (f < 0f) f = 0f
                        if (f > 1f) f = 1f
                        column.columnBgImageAlpha = f
                        showAlpha(updateText = false, updateSeek = true)
                    }
                } catch (ex: Throwable) {
                    log.e(ex, "alpha parse failed.")
                }
            }
        })

        etAlpha.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    etAlpha.hideKeyboard()
                    true
                }

                else -> false
            }
        }
    }

    private fun show() {
        try {
            loadingBusy = true

            column.setHeaderBackground(llColumnHeader)

            val c = column.getHeaderNameColor()
            tvColumnName.setTextColor(c)
            ivColumnHeader.setImageResource(column.getIconId())
            ivColumnHeader.imageTintList = ColorStateList.valueOf(c)

            tvColumnName.text = column.getColumnName(false)

            when (column.columnBgColor) {
                0 -> flColumnBackground.background = null
                else -> flColumnBackground.setBackgroundColor(column.columnBgColor)
            }

            showAlpha(updateText = true, updateSeek = true)

            loadImage(ivColumnBackground, column.columnBgImage)

            tvSampleAcct.setTextColor(column.getAcctColor())
            tvSampleContent.setTextColor(column.getContentColor())
        } finally {
            loadingBusy = false
        }
    }

    private fun showAlpha(updateText: Boolean, updateSeek: Boolean) {
        var alpha = column.columnBgImageAlpha
        if (alpha.isNaN()) {
            alpha = 1f
            column.columnBgImageAlpha = alpha
        }
        ivColumnBackground.alpha = alpha
        val hasAlphaWarning = alpha < 0.3 && column.columnBgImage.isNotEmpty()
        tvBackgroundError.vg(hasAlphaWarning)?.text =
            getString(R.string.image_alpha_too_low)
        if (updateText) {
            etAlpha.setText("%.4f".format(column.columnBgImageAlpha))
        }
        if (updateSeek) {
            sbColumnBackgroundAlpha.progress = (0.5f + alpha * PROGRESS_MAX).toInt()
        }
    }

    private fun closeBitmaps() {
        try {
            ivColumnBackground.setImageDrawable(null)
            lastImageUri = null

            lastImageBitmap?.recycle()
            lastImageBitmap = null
        } catch (ex: Throwable) {
            log.e(ex, "closeBitmaps failed.")
        }
    }

    private fun loadImage(ivColumnBackground: ImageView, url: String) {
        try {
            if (url.isEmpty()) {
                closeBitmaps()
                return
            } else if (url == lastImageUri) {
                // 今表示してるのと同じ
                return
            }

            // 直前のBitmapを掃除する
            closeBitmaps()

            val uri = url.mayUri() ?: return

            // 画像をロードして、成功したら表示してURLを覚える
            val resizeMax = (0.5f + 64f * density).toInt()
            lastImageBitmap = createResizedBitmap(this, uri, resizeMax)
            if (lastImageBitmap != null) {
                ivColumnBackground.setImageBitmap(lastImageBitmap)
                lastImageUri = url
            }
        } catch (ex: Throwable) {
            log.e(ex, "loadImage failed.")
        }
    }
}
