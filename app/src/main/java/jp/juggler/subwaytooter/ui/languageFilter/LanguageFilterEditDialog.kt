package jp.juggler.subwaytooter.ui.languageFilter

import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.core.widget.addTextChangedListener
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.api.entity.TootStatus
import jp.juggler.subwaytooter.dialog.actionsDialog
import jp.juggler.util.coroutine.cancellationException
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.ui.dismissSafe
import jp.juggler.util.ui.dp
import jp.juggler.util.ui.isEnabledAlpha
import jp.juggler.util.ui.setEnabledColor
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

sealed interface LanguageFilterEditResult {
    class Update(val code: String, val allow: Boolean) : LanguageFilterEditResult
    class Delete(val code: String) : LanguageFilterEditResult
}

/**
 * 言語コード1つを追加/編集/削除するダイアログ
 */
suspend fun ComponentActivity.dialogLanguageFilterEdit(
    // 既存項目の編集時は非null
    item: LanguageFilterItem?,
    // 言語コード→表示名のマップ
    nameMap: Map<String, LanguageInfo>,
    // 色スキーマ
    colorScheme: ColorScheme,
): LanguageFilterEditResult = suspendCancellableCoroutine { cont ->
    val dp12 = dp(12)

    val etLanguage = EditText(this).apply {
        inputType = android.text.InputType.TYPE_CLASS_TEXT
        hint = getString(R.string.language_code_hint)
        importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_NO
    }
    val tvLanguage = TextView(this).apply {
        textSize = 12f
    }
    val rbShow = RadioButton(this).apply {
        text = getString(R.string.language_show)
    }
    val rbHide = RadioButton(this).apply {
        text = getString(R.string.language_hide)
    }
    val btnPresets = ImageButton(this).apply {
        elevation = dp(3).toFloat()
        setImageResource(R.drawable.ic_edit)
        contentDescription = getString(R.string.presets)
    }

    fun updateDesc() {
        val code = etLanguage.text.toString().trim()
        tvLanguage.text = nameMap[code]?.displayName ?: getString(R.string.custom)
    }

    when (item?.allow ?: true) {
        true -> rbShow.isChecked = true
        else -> rbHide.isChecked = true
    }
    btnPresets.setOnClickListener {
        launchAndShowError {
            actionsDialog(getString(R.string.presets)) {
                val languageList = nameMap.map {
                    LanguageFilterItem(it.key, true)
                }.sortedWith(languageFilterItemComparator)
                for (a in languageList) {
                    action("${a.code} ${langDesc(a.code, nameMap)}") {
                        etLanguage.setText(a.code)
                        updateDesc()
                    }
                }
            }
        }
    }
    etLanguage.addTextChangedListener { updateDesc() }
    etLanguage.setText(item?.code ?: "")
    updateDesc()
    // 編集時は言語コードを変更できない
    etLanguage.isEnabledAlpha = item == null
    btnPresets.setEnabledColor(
        btnPresets.context,
        R.drawable.ic_edit,
        colorScheme.onSurface.toArgb(),
        item == null
    )

    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(dp(300), LinearLayout.LayoutParams.WRAP_CONTENT)

        // Language label
        addView(TextView(this@dialogLanguageFilterEdit).apply {
            text = getString(R.string.language)
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(dp12, dp12, dp12, 0) })

        // EditText + presets button row
        addView(LinearLayout(this@dialogLanguageFilterEdit).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(etLanguage, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(btnPresets, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(dp12, 0, dp12, 0) })

        // Language description
        addView(tvLanguage, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(dp12, 0, dp12, 0) })

        // Show/Hide label
        addView(TextView(this@dialogLanguageFilterEdit).apply {
            text = getString(R.string.show_hide)
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(dp12, dp12, dp12, 0) })

        // RadioGroup
        addView(RadioGroup(this@dialogLanguageFilterEdit).apply {
            orientation = RadioGroup.HORIZONTAL
            addView(rbShow)
            addView(rbHide, RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.WRAP_CONTENT,
                RadioGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp12 })
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(dp12, 0, dp12, 0) })
    }

    fun getCode() = etLanguage.text.toString().trim()
    fun isAllow() = rbShow.isChecked

    AlertDialog.Builder(this@dialogLanguageFilterEdit).apply {
        setView(root)
        setCancelable(true)
        setNegativeButton(R.string.cancel, null)
        setPositiveButton(R.string.ok) { _, _ ->
            if (cont.isActive) cont.resume(
                LanguageFilterEditResult.Update(getCode(), isAllow())
            ) { _, _, _ -> }
        }
        if (item != null && item.code != TootStatus.LANGUAGE_CODE_DEFAULT) {
            setNeutralButton(R.string.delete) { _, _ ->
                if (cont.isActive) cont.resume(
                    LanguageFilterEditResult.Delete(item.code)
                ) { _, _, _ -> }
            }
        }
    }.create().also { dialog ->
        dialog.setOnDismissListener {
            if (cont.isActive) cont.resumeWithException(cancellationException())
        }
        cont.invokeOnCancellation { dialog.dismissSafe() }
        dialog.show()
    }
}
