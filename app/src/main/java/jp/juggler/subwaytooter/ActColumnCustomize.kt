package jp.juggler.subwaytooter

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import jp.juggler.subwaytooter.compose.ColorPickerDialog
import jp.juggler.util.backPressed
import jp.juggler.util.coroutine.launchMain
import jp.juggler.util.data.checkMimeTypeAndGrant
import jp.juggler.util.data.intentGetContent
import jp.juggler.util.data.mayUri
import jp.juggler.util.data.notZero
import jp.juggler.util.int
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import jp.juggler.util.log.withCaption
import jp.juggler.util.media.createResizedBitmap
import jp.juggler.util.ui.ActivityResultHandler
import jp.juggler.util.ui.isNotOk
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

class ActColumnCustomize : ComponentActivity() {

    companion object {
        internal val log = LogCategory("ActColumnCustomize")
        internal const val EXTRA_COLUMN_INDEX = "column_index"

        fun createIntent(activity: ActMain, idx: Int) =
            Intent(activity, ActColumnCustomize::class.java).apply {
                putExtra(EXTRA_COLUMN_INDEX, idx)
            }
    }

    private var columnIndex: Int = 0
    private lateinit var column: Column
    private lateinit var appState: AppState
    private var density: Float = 0f

    // Compose state triggers
    private var revision = mutableIntStateOf(0)

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

    private fun refreshUi() {
        revision.intValue++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backPressed {
            makeResult()
            finish()
        }
        arColumnBackgroundImage.register(this)
        App1.setActivityTheme(this)

        appState = App1.getAppState(this)
        density = appState.density
        columnIndex = intent.int(EXTRA_COLUMN_INDEX) ?: 0
        column = appState.column(columnIndex)!!

        setContent {
            ColumnCustomizeContent(
                modifier = Modifier,
            )
        }
    }

    override fun onDestroy() {
        closeBitmaps()
        makeResult()
        finish()
        super.onDestroy()
    }

    // ---- Color picker dialog state ----

    private enum class ColorTarget {
        HeaderBg, HeaderFg, ColumnBg, Acct, Content
    }

    @Composable
    private fun ColumnCustomizeContent(modifier: Modifier = Modifier) {
        // Read revision to trigger recomposition
        val rev = revision.intValue

        var colorPickerTarget by remember { mutableStateOf<ColorTarget?>(null) }

        // Alpha state
        var alphaText by remember { mutableStateOf("%.4f".format(column.columnBgImageAlpha)) }
        var alphaSlider by remember { mutableFloatStateOf(column.columnBgImageAlpha) }

        fun syncAlpha(newAlpha: Float, updateText: Boolean, updateSlider: Boolean) {
            val a = if (newAlpha.isNaN()) 1f else newAlpha.coerceIn(0f, 1f)
            column.columnBgImageAlpha = a
            if (updateText) alphaText = "%.4f".format(a)
            if (updateSlider) alphaSlider = a
            refreshUi()
        }

        // Color picker dialogs
        colorPickerTarget?.let { target ->
            val (initialColor, alphaEnabled) = when (target) {
                ColorTarget.HeaderBg -> column.getHeaderBackgroundColor() to false
                ColorTarget.HeaderFg -> column.getHeaderNameColor() to false
                ColorTarget.ColumnBg -> (column.columnBgColor.notZero()
                    ?: Column.defaultColorHeaderBg) to false
                ColorTarget.Acct -> column.getAcctColor() to true
                ColorTarget.Content -> column.getContentColor() to true
            }
            ColorPickerDialog(
                colorInitial = initialColor,
                alphaEnabled = alphaEnabled,
                onDismiss = { colorPickerTarget = null },
                onColorSelected = { color ->
                    when (target) {
                        ColorTarget.HeaderBg -> column.headerBgColor = Color.BLACK or color
                        ColorTarget.HeaderFg -> column.headerFgColor = Color.BLACK or color
                        ColorTarget.ColumnBg -> column.columnBgColor = Color.BLACK or color
                        ColorTarget.Acct -> column.acctColor = color.notZero() ?: 1
                        ColorTarget.Content -> column.contentColor = color.notZero() ?: 1
                    }
                    colorPickerTarget = null
                    refreshUi()
                },
            )
        }

        val scrollState = rememberScrollState()

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(12.dp),
        ) {
            // ---- Column Header Section ----
            HorizontalDivider()
            SectionLabel(stringResource(R.string.column_header))

            // Header preview
            val headerBgColor = column.getHeaderBackgroundColor()
            val headerFgColor = column.getHeaderNameColor()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Color(headerBgColor))
                    .padding(12.dp, 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(column.getIconId()),
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color(headerFgColor),
                    modifier = Modifier.size(32.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = column.getColumnName(false),
                    color = androidx.compose.ui.graphics.Color(headerFgColor),
                )
            }
            Spacer(Modifier.height(6.dp))

            // Header background color
            IndentedLabel(stringResource(R.string.background_color))
            ColorEditRow(
                onEdit = { colorPickerTarget = ColorTarget.HeaderBg },
                onReset = {
                    column.headerBgColor = 0
                    refreshUi()
                },
            )

            // Header foreground color
            IndentedLabel(stringResource(R.string.foreground_color))
            ColorEditRow(
                onEdit = { colorPickerTarget = ColorTarget.HeaderFg },
                onReset = {
                    column.headerFgColor = 0
                    refreshUi()
                },
            )

            // ---- Column Section ----
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            SectionLabel(stringResource(R.string.column))

            // Column background preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (column.columnBgColor != 0) {
                            Modifier.background(
                                androidx.compose.ui.graphics.Color(column.columnBgColor)
                            )
                        } else Modifier
                    ),
            ) {
                // Background image (AndroidView for bitmap)
                val bgImage = column.columnBgImage
                val bgAlpha = column.columnBgImageAlpha
                if (bgImage.isNotEmpty()) {
                    AndroidView(
                        factory = { ctx ->
                            ImageView(ctx).apply {
                                scaleType = ImageView.ScaleType.CENTER_CROP
                            }
                        },
                        modifier = Modifier.matchParentSize(),
                        update = { iv ->
                            iv.alpha = if (bgAlpha.isNaN()) 1f else bgAlpha
                            loadImage(iv, bgImage)
                        },
                    )
                }
                // Sample content
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.acct_sample),
                        color = androidx.compose.ui.graphics.Color(column.getAcctColor()),
                        fontSize = 12.sp,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = stringResource(R.string.content_sample),
                        color = androidx.compose.ui.graphics.Color(column.getContentColor()),
                        lineHeight = 20.sp,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            // Column background color
            FormLabel(stringResource(R.string.background_color))
            ColorEditRow(
                onEdit = { colorPickerTarget = ColorTarget.ColumnBg },
                onReset = {
                    column.columnBgColor = 0
                    refreshUi()
                },
            )

            // Column background image
            FormLabel(stringResource(R.string.background_image))
            Row(modifier = Modifier.padding(start = 32.dp)) {
                Button(onClick = {
                    val intent = intentGetContent(
                        false,
                        getString(R.string.pick_image),
                        arrayOf("image/*")
                    )
                    arColumnBackgroundImage.launch(intent)
                }) {
                    Text(stringResource(R.string.pick_image))
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = {
                    column.columnBgImage = ""
                    refreshUi()
                }) {
                    Text(stringResource(R.string.reset))
                }
            }

            // Background image alpha
            FormLabel(stringResource(R.string.background_image_alpha))
            Row(
                modifier = Modifier.padding(start = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val keyboardController = LocalSoftwareKeyboardController.current
                TextField(
                    value = alphaText,
                    onValueChange = { text ->
                        alphaText = text
                        try {
                            val f = NumberFormat.getInstance(Locale.getDefault())
                                .parse(text)?.toFloat()
                            if (f != null && !f.isNaN()) {
                                syncAlpha(f, updateText = false, updateSlider = true)
                            }
                        } catch (_: Throwable) {
                        }
                    },
                    modifier = Modifier.width(100.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { keyboardController?.hide() },
                    ),
                )
                Spacer(Modifier.width(8.dp))
                Slider(
                    value = alphaSlider,
                    onValueChange = { v ->
                        syncAlpha(v, updateText = true, updateSlider = false)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            // Alpha warning
            val alpha = column.columnBgImageAlpha
            if (alpha < 0.3f && column.columnBgImage.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.image_alpha_too_low),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 32.dp),
                )
            }

            // Acct color
            FormLabel(stringResource(R.string.acct_color))
            ColorEditRow(
                onEdit = { colorPickerTarget = ColorTarget.Acct },
                onReset = {
                    column.acctColor = 0
                    refreshUi()
                },
            )

            // Content color
            FormLabel(stringResource(R.string.content_color))
            ColorEditRow(
                onEdit = { colorPickerTarget = ColorTarget.Content },
                onReset = {
                    column.contentColor = 0
                    refreshUi()
                },
            )

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
        }
    }

    @Composable
    private fun SectionLabel(text: String) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }

    @Composable
    private fun IndentedLabel(text: String) {
        Text(
            text = text,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 48.dp, top = 3.dp),
        )
    }

    @Composable
    private fun FormLabel(text: String) {
        Text(
            text = text,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 32.dp, top = 4.dp),
        )
    }

    @Composable
    private fun ColorEditRow(
        onEdit: () -> Unit,
        onReset: () -> Unit,
    ) {
        Row(modifier = Modifier.padding(start = 32.dp)) {
            Button(onClick = onEdit) {
                Text(stringResource(R.string.edit))
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onReset) {
                Text(stringResource(R.string.reset))
            }
        }
    }

    // ---- Background image handling ----

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
                        refreshUi()
                    }
                }
            }
        }
    }

    private fun closeBitmaps() {
        try {
            lastImageUri = null
            lastImageBitmap?.recycle()
            lastImageBitmap = null
        } catch (ex: Throwable) {
            log.e(ex, "closeBitmaps failed.")
        }
    }

    private fun loadImage(iv: ImageView, url: String) {
        try {
            if (url.isEmpty()) {
                iv.setImageDrawable(null)
                closeBitmaps()
                return
            } else if (url == lastImageUri) {
                if (lastImageBitmap != null) {
                    iv.setImageBitmap(lastImageBitmap)
                }
                return
            }

            closeBitmaps()

            val uri = url.mayUri() ?: return

            val resizeMax = (0.5f + 64f * density).toInt()
            lastImageBitmap = createResizedBitmap(this, uri, resizeMax)
            if (lastImageBitmap != null) {
                iv.setImageBitmap(lastImageBitmap)
                lastImageUri = url
            }
        } catch (ex: Throwable) {
            log.e(ex, "loadImage failed.")
        }
    }
}
