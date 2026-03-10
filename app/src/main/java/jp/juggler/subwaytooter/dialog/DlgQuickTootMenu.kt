package jp.juggler.subwaytooter.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import jp.juggler.subwaytooter.*
import jp.juggler.subwaytooter.api.entity.TootVisibility
import jp.juggler.subwaytooter.pref.PrefS
import jp.juggler.util.data.notEmpty
import jp.juggler.util.ui.dismissSafe
import java.lang.ref.WeakReference

class DlgQuickTootMenu(
    internal val activity: ActMain,
    internal val callback: Callback,
) {
    companion object {
        val visibilityList = arrayOf(
            TootVisibility.AccountSetting,
            TootVisibility.WebSetting,
            TootVisibility.Public,
            TootVisibility.UnlistedHome,
            TootVisibility.PrivateFollowers,
            TootVisibility.DirectSpecified,
        )
    }

    interface Callback {
        fun onMacro(text: String)
        var visibility: TootVisibility
    }

    private data class MemoViews(
        val editText: EditText,
        val btnUse: Button,
    )

    private var refDialog: WeakReference<Dialog>? = null

    private fun loadStrings() =
        PrefS.spQuickTootMacro.value.split("\n")

    private fun saveStrings(newValue: String) {
        PrefS.spQuickTootMacro.value = newValue
    }

    private fun show() {
        val strings = loadStrings()
        val dialog = Dialog(activity).also { refDialog = WeakReference(it) }
        val density = activity.resources.displayMetrics.density
        val dp3 = (3 * density + 0.5f).toInt()
        val dp6 = (6 * density + 0.5f).toInt()
        val dp12 = (12 * density + 0.5f).toInt()
        val dp48 = (48 * density + 0.5f).toInt()

        val innerLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp12, dp3, dp12, dp3)
        }

        // Visibility label
        innerLayout.addView(
            TextView(activity).apply { setText(R.string.visibility) },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        // Visibility button
        val btnVisibility = Button(activity).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            isAllCaps = false
        }
        innerLayout.addView(
            btnVisibility,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        // Fixed phrase label
        innerLayout.addView(
            TextView(activity).apply {
                setText(R.string.fixed_phrase)
                setPadding(0, dp6, 0, 0)
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        // Create 6 memo rows
        val memoList = (0..5).map { i ->
            val et = EditText(activity).apply {
                importantForAutofill = EditText.IMPORTANT_FOR_AUTOFILL_NO
                inputType = android.text.InputType.TYPE_CLASS_TEXT
                setText(strings.elementAtOrNull(i) ?: "")
            }
            val btn = Button(activity).apply {
                setText(R.string.input)
                minimumWidth = dp48
            }
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(et, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp6
                })
                addView(btn, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ))
            }
            innerLayout.addView(
                row,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
            MemoViews(et, btn)
        }

        memoList.forEach { m ->
            m.btnUse.setOnClickListener {
                m.editText.text?.toString()?.notEmpty()?.let {
                    dialog.dismissSafe()
                    callback.onMacro(it)
                }
            }
        }

        dialog.setOnDismissListener {
            saveStrings(
                memoList.map { it.editText.text?.toString()?.replace("\n", " ") ?: "" }
                    .joinToString("\n")
            )
        }

        fun showVisibility() {
            btnVisibility.text = getVisibilityCaption(activity, false, callback.visibility)
        }

        fun changeVisivility(newVisibility: TootVisibility?) {
            newVisibility ?: return
            callback.visibility = newVisibility
            showVisibility()
        }

        showVisibility()

        btnVisibility.setOnClickListener {
            val captionList = visibilityList
                .map { getVisibilityCaption(activity, false, it) }
                .toTypedArray()

            AlertDialog.Builder(activity)
                .setTitle(R.string.choose_visibility)
                .setItems(captionList) { _, which ->
                    changeVisivility(visibilityList.elementAtOrNull(which))
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        val scrollView = ScrollView(activity).apply {
            addView(innerLayout)
        }

        // Close button
        val btnCancel = Button(activity, null, android.R.attr.buttonBarButtonStyle).apply {
            setText(R.string.close)
            setOnClickListener { dialog.dismissSafe() }
        }
        val buttonBar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(btnCancel, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(scrollView, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f,
            ))
            addView(buttonBar, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ))
        }

        dialog.apply {
            setContentView(root)
            setCanceledOnTouchOutside(true)
            window?.apply {
                attributes = attributes.apply {
                    gravity = Gravity.BOTTOM or Gravity.START
                    flags = flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
                }
                setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                )
            }
            show()
        }
    }

    fun toggle() {
        val dialog = refDialog?.get()
        when {
            dialog?.isShowing == true -> dialog.dismissSafe()
            else -> show()
        }
    }
}
