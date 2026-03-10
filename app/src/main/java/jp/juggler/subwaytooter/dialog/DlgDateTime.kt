package jp.juggler.subwaytooter.dialog

import android.app.Activity
import android.app.Dialog
import android.provider.Settings
import android.view.WindowManager
import android.widget.Button
import android.widget.DatePicker
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TimePicker
import jp.juggler.subwaytooter.R
import jp.juggler.util.ui.dismissSafe
import java.util.*

class DlgDateTime(
    val activity: Activity,
) {

    private lateinit var datePicker: DatePicker
    private lateinit var timePicker: TimePicker
    private lateinit var dialog: Dialog

    private lateinit var callback: (Long) -> Unit

    fun open(initialValue: Long, callback: (Long) -> Unit) {
        this.callback = callback

        datePicker = DatePicker(activity).apply {
            calendarViewShown = false
        }
        timePicker = TimePicker(activity)

        val innerLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(datePicker, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
            })
            addView(timePicker, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
            })
        }

        val scrollView = ScrollView(activity).apply {
            addView(innerLayout)
        }

        val btnCancel = Button(activity, null, android.R.attr.buttonBarButtonStyle).apply {
            setText(R.string.cancel)
        }
        val btnOk = Button(activity, null, android.R.attr.buttonBarButtonStyle).apply {
            setText(R.string.ok)
        }

        val buttonBar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(btnCancel, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(btnOk, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
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

        val c = GregorianCalendar.getInstance(TimeZone.getDefault())
        c.timeInMillis = when (initialValue) {
            0L -> System.currentTimeMillis() + 10 * 60000L
            else -> initialValue
        }
        datePicker.firstDayOfWeek = Calendar.MONDAY
        datePicker.init(
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH),
            null
        )

        timePicker.hour = c.get(Calendar.HOUR_OF_DAY)
        timePicker.minute = c.get(Calendar.MINUTE)

        timePicker.setIs24HourView(
            when (Settings.System.getString(activity.contentResolver, Settings.System.TIME_12_24)) {
                "12" -> false
                else -> true
            }
        )

        btnCancel.setOnClickListener { dialog.cancel() }
        btnOk.setOnClickListener {
            dialog.dismissSafe()
            this.callback(getTime())
        }

        dialog = Dialog(activity)
        dialog.setContentView(root)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.show()
    }

    private fun getTime(): Long {
        val c = GregorianCalendar.getInstance(TimeZone.getDefault())
        c.set(
            datePicker.year,
            datePicker.month,
            datePicker.dayOfMonth,
            timePicker.hour,
            timePicker.minute,
            0,
        )
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }
}
