package jp.juggler.subwaytooter.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
import jp.juggler.util.coroutine.AppDispatchers
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.coroutine.withProgress
import jp.juggler.util.log.LogCategory
import jp.juggler.util.ui.attrColor
import jp.juggler.util.ui.resDrawable
import kotlinx.coroutines.withContext

private val log = LogCategory("DlgQRCode")

val UInt.int get() = toInt()

fun AppCompatActivity.dialogQrCode(
    message: CharSequence,
    url: String,
) = launchAndShowError("dialogQrCode failed.") {
    val drawable = withProgress(
        caption = getString(R.string.generating_qr_code),
    ) {
        withContext(AppDispatchers.DEFAULT) {
            QrCodeDrawable(data = QrData.Url(url), options = qrCodeOptions())
        }
    }
    val dialog = Dialog(this@dialogQrCode)

    val density = resources.displayMetrics.density
    val dp6 = (6 * density + 0.5f).toInt()
    val dp10 = (10 * density + 0.5f).toInt()
    val dp12 = (12 * density + 0.5f).toInt()
    val dp280 = (280 * density + 0.5f).toInt()
    val dp1 = (1 * density + 0.5f).toInt()

    val root = LinearLayout(this@dialogQrCode).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp6, 0, 0)
    }

    root.addView(
        TextView(this@dialogQrCode).apply {
            text = message
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(dp12, 0, dp12, 0)
        },
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ),
    )

    root.addView(
        TextView(this@dialogQrCode).apply {
            text = "[ $url ]" // なぜか素のURLだと@以降が表示されない
            textSize = 10f
            gravity = Gravity.CENTER
            setPadding(dp12, 0, dp12, 0)
        },
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ),
    )

    root.addView(
        ImageView(this@dialogQrCode).apply {
            setImageDrawable(drawable)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        },
        LinearLayout.LayoutParams(dp280, dp280).apply {
            setMargins(dp10, dp10, dp10, dp10)
        },
    )

    root.addView(
        View(this@dialogQrCode).apply {
            setBackgroundColor(attrColor(R.attr.colorSettingDivider))
        },
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp1,
        ),
    )

    root.addView(
        Button(this@dialogQrCode).apply {
            setText(R.string.close)
            setBackgroundResource(R.drawable.btn_bg_transparent_round6dp)
            isAllCaps = false
            setOnClickListener { dialog.cancel() }
        },
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ),
    )

    dialog.apply {
        setContentView(root)
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        show()
    }
}

private fun AppCompatActivity.qrCodeOptions() = createQrVectorOptions {
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
