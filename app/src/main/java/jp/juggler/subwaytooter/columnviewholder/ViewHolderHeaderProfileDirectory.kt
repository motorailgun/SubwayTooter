package jp.juggler.subwaytooter.columnviewholder

import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.column.Column
import jp.juggler.subwaytooter.column.getContentColor
import jp.juggler.util.ui.dp

internal class ViewHolderHeaderProfileDirectory(
    override val activity: ActMain,
    parent: ViewGroup,
) : ViewHolderHeaderBase(
    LinearLayout(parent.context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        orientation = LinearLayout.VERTICAL
        val pad = context.dp(12)
        setPaddingRelative(pad, 0, pad, 0)
    }
), CompoundButton.OnCheckedChangeListener {

    private var busy = false

    private val rbOrderActive: RadioButton
    private val rbOrderNew: RadioButton
    private val cbResolve: CheckBox

    init {
        val root = itemView as LinearLayout
        root.tag = this

        val radioGroup = RadioGroup(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = RadioGroup.VERTICAL
        }

        rbOrderActive = RadioButton(activity).apply {
            layoutParams = RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.MATCH_PARENT,
                RadioGroup.LayoutParams.WRAP_CONTENT
            )
            text = activity.getString(R.string.order_active)
            setOnCheckedChangeListener(this@ViewHolderHeaderProfileDirectory)
        }
        radioGroup.addView(rbOrderActive)

        rbOrderNew = RadioButton(activity).apply {
            layoutParams = RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.MATCH_PARENT,
                RadioGroup.LayoutParams.WRAP_CONTENT
            )
            text = activity.getString(R.string.order_new)
            setOnCheckedChangeListener(this@ViewHolderHeaderProfileDirectory)
        }
        radioGroup.addView(rbOrderNew)

        root.addView(radioGroup)

        cbResolve = CheckBox(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val p = context.dp(3)
            setPadding(0, p, 0, p)
            text = activity.getString(R.string.resolve_non_local_account)
            setOnCheckedChangeListener(this@ViewHolderHeaderProfileDirectory)
        }
        root.addView(cbResolve)
    }

    override fun showColor() {
        val c = column.getContentColor()
        rbOrderActive.setTextColor(c)
        rbOrderNew.setTextColor(c)
        cbResolve.setTextColor(c)
    }

    override fun bindData(column: Column) {
        super.bindData(column)

        busy = true
        try {
            cbResolve.isChecked = column.searchResolve

            if (column.searchQuery == "new") {
                rbOrderNew.isChecked = true
            } else {
                rbOrderActive.isChecked = true
            }
        } finally {
            busy = false
        }
    }

    override fun onViewRecycled() {
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (busy) return

        if (buttonView is RadioButton && !isChecked) return

        when (buttonView) {
            rbOrderActive -> column.searchQuery = "active"
            rbOrderNew -> column.searchQuery = "new"
            cbResolve -> column.searchResolve = isChecked
        }

        reloadBySettingChange()
    }
}
