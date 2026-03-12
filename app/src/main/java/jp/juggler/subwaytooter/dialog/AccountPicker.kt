package jp.juggler.subwaytooter.dialog

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.compose.StThemedContent
import jp.juggler.subwaytooter.table.*
import jp.juggler.util.data.notEmpty
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import jp.juggler.util.ui.attrColor
import jp.juggler.util.ui.dismissSafe
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private val log = LogCategory("pickAccount")

@Composable
private fun AppTheme(content: @Composable () -> Unit) {
    StThemedContent(content = content)
}

@SuppressLint("InflateParams")
suspend fun Activity.pickAccount(
    bAllowPseudo: Boolean = false,
    bAllowMisskey: Boolean = true,
    bAllowMastodon: Boolean = true,
    bAuto: Boolean = false,
    message: String? = null,
    accountListArg: List<SavedAccount>? = null,
    dismissCallback: (dialog: DialogInterface) -> Unit = {},
    extraCallback: (LinearLayout, Int, Int) -> Unit = { _, _, _ -> },
): SavedAccount? {
    val activity = this
    var removeMastodon = 0
    var removedMisskey = 0
    var removedPseudo = 0

    fun SavedAccount.checkMastodon() = when {
        !bAllowMastodon && !isMisskey -> ++removeMastodon
        else -> 0
    }

    fun SavedAccount.checkMisskey() = when {
        !bAllowMisskey && isMisskey -> ++removedMisskey
        else -> 0
    }

    fun SavedAccount.checkPseudo() = when {
        !bAllowPseudo && isPseudo -> ++removedPseudo
        else -> 0
    }

    val accountList = accountListArg
        ?: daoSavedAccount.loadAccountList()
            .filter { 0 == it.checkMastodon() + it.checkMisskey() + it.checkPseudo() }
            .sortedByNickname()

    if (accountList.isEmpty()) {

        val sb = java.lang.StringBuilder()

        if (removedPseudo > 0) {
            sb.append(activity.getString(R.string.not_available_for_pseudo_account))
        }

        if (removedMisskey > 0) {
            if (sb.isNotEmpty()) sb.append('\n')
            sb.append(activity.getString(R.string.not_available_for_misskey_account))
        }
        if (removeMastodon > 0) {
            if (sb.isNotEmpty()) sb.append('\n')
            sb.append(activity.getString(R.string.not_available_for_mastodon_account))
        }

        if (sb.isEmpty()) {
            sb.append(activity.getString(R.string.account_empty))
        }

        activity.showToast(false, sb.toString())
        return null
    }

    if (bAuto && accountList.size == 1) {
        return accountList[0]
    }

    return suspendCoroutine { continuation ->
        val dialog = Dialog(activity)
        val isResumed = AtomicBoolean(false)

        dialog.setOnDismissListener {
            dismissCallback(it)
            if (isResumed.compareAndSet(false, true)) {
                continuation.resume(null)
            }
        }

        val density = activity.resources.displayMetrics.density
        val padX = (0.5f + 12f * density).toInt()
        val padY = (0.5f + 6f * density).toInt()

        val composeView = ComposeView(activity).apply {
            setContent {
                AppTheme {
                    Surface {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!message.isNullOrEmpty()) {
                                Text(
                                    text = message,
                                    fontSize = 16.sp,
                                    color = Color(activity.attrColor(android.R.attr.textColorPrimary)),
                                    modifier = Modifier.padding(12.dp, 6.dp, 12.dp, 6.dp)
                                )
                            }
                            
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .fillMaxWidth()
                            ) {
                                item {
                                    AndroidView(
                                        factory = { context ->
                                            LinearLayout(context).apply {
                                                orientation = LinearLayout.VERTICAL
                                                layoutParams = LinearLayout.LayoutParams(
                                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                                )
                                                extraCallback(this, padX, padY)
                                            }
                                        }
                                    )
                                }

                                items(accountList) { a ->
                                    val ac = daoAcctColor.load(a)
                                    val hasBg = daoAcctColor.hasColorBackground(ac)
                                    val hasFg = daoAcctColor.hasColorForeground(ac)

                                    val bgColor = if (hasBg) Color(ac.colorBg) else Color.Transparent
                                    val fgColor = if (hasFg) Color(ac.colorFg) else Color.Unspecified
                                    
                                    val errorMsg = try {
                                        val status = daoAccountNotificationStatus.load(a.acct)
                                        val lastNotificationError = status?.lastNotificationError?.notEmpty()
                                        val lastSubscriptionError = status?.lastSubscriptionError?.notEmpty()
                                        lastNotificationError ?: lastSubscriptionError
                                    } catch (ex: Throwable) {
                                        log.e(ex, "can't get notification status for ${a.acct}")
                                        null
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                            .background(color = bgColor, shape = RoundedCornerShape(6.dp))
                                            .clickable {
                                                if (isResumed.compareAndSet(false, true)) {
                                                    continuation.resume(a)
                                                }
                                                dialog.dismissSafe()
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = buildAnnotatedString {
                                                append(ac.nickname)
                                                if (errorMsg != null) {
                                                    append("\n")
                                                    withStyle(style = SpanStyle(fontSize = 11.sp)) {
                                                        append(errorMsg)
                                                    }
                                                }
                                            },
                                            color = fgColor,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                            }
                            
                            HorizontalDivider(
                                color = Color(activity.attrColor(R.attr.colorSettingDivider)),
                                thickness = 1.dp
                            )
                            
                            TextButton(
                                onClick = { dialog.cancel() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    }
                }
            }
        }

        dialog.setContentView(composeView)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.show()
    }
}
