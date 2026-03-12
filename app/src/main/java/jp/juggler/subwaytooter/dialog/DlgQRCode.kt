package jp.juggler.subwaytooter.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.github.alexzhirkevich.customqrgenerator.QrData
import com.github.alexzhirkevich.customqrgenerator.vector.QrCodeDrawable
import com.github.alexzhirkevich.customqrgenerator.vector.createQrVectorOptions
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorBallShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColor
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorFrameShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorLogoPadding
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorLogoShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.compose.StThemedContent
import jp.juggler.util.coroutine.AppDispatchers
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.coroutine.withProgress
import jp.juggler.util.log.LogCategory
import jp.juggler.util.ui.attrColor
import jp.juggler.util.ui.resDrawable
import kotlinx.coroutines.withContext

private val log = LogCategory("DlgQRCode")

val UInt.int get() = toInt()

fun ComponentActivity.dialogQrCode(
    message: CharSequence,
    url: String,
) = launchAndShowError("dialogQrCode failed.") {
    val bitmap = withProgress(
        caption = getString(R.string.generating_qr_code),
    ) {
        withContext(AppDispatchers.DEFAULT) {
            val drawable = QrCodeDrawable(data = QrData.Url(url), options = qrCodeOptions())
            drawable.toBitmap(512, 512)
        }
    }

    val dialog = Dialog(this@dialogQrCode)

    val composeView = ComposeView(this@dialogQrCode).apply {
        setContent {
            StThemedContent {
                Surface {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    ) {
                        Text(
                            text = message.toString(),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        
                        Text(
                            text = "[ $url ]",
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(280.dp)
                        )

                        val context = LocalContext.current
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = androidx.compose.ui.graphics.Color(context.attrColor(R.attr.colorSettingDivider))
                        )

                        TextButton(
                            onClick = { dialog.cancel() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(id = R.string.close))
                        }
                    }
                }
            }
        }
    }

    dialog.setContentView(composeView)
    dialog.window?.setLayout(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT
    )
    dialog.setCancelable(true)
    dialog.setCanceledOnTouchOutside(true)
    dialog.show()
}

private fun ComponentActivity.qrCodeOptions() = createQrVectorOptions {
    background {
        drawable = ColorDrawable(Color.WHITE)
    }

    padding = .125f

    logo {
        drawable = resDrawable(R.drawable.qr_code_center)
        size = .25f
        shape = QrVectorLogoShape.Default
        padding = QrVectorLogoPadding.Natural(.1f)
    }
    shapes {
        // 市松模様のドット
        darkPixel = QrVectorPixelShape.RoundCorners(.5f)
        // 3隅の真ん中の大きめドット
        ball = QrVectorBallShape.RoundCorners(.25f)
        // 3隅の枠
        frame = QrVectorFrameShape.RoundCorners(.25f)
    }
    colors {
        val cobalt = 0xFF0088FFU.int
        val cobaltDark = 0xFF004488U.int
        // 市松模様のドット
        dark = QrVectorColor.Solid(cobaltDark)
        // 3隅の真ん中の大きめドット
        ball = QrVectorColor.RadialGradient(
            colors = listOf(
                0f to cobaltDark,
                1f to cobalt,
            ),
            radius = 2f,
        )
        // 3隅の枠
        frame = QrVectorColor.LinearGradient(
            colors = listOf(
                0f to cobaltDark,
                1f to cobalt,
            ),
            orientation = QrVectorColor.LinearGradient
                .Orientation.Vertical
        )
    }
}
