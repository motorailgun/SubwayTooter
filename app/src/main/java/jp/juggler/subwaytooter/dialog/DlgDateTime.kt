package jp.juggler.subwaytooter.dialog

import android.app.Activity
import android.app.Dialog
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.compose.StThemedContent
import jp.juggler.util.ui.dismissSafe
import java.util.*

class DlgDateTime(val activity: Activity) {

    private lateinit var dialog: Dialog
    private lateinit var callback: (Long) -> Unit

    fun open(initialValue: Long, callback: (Long) -> Unit) {
        this.callback = callback

        val c = GregorianCalendar.getInstance(TimeZone.getDefault())
        c.timeInMillis = when (initialValue) {
            0L -> System.currentTimeMillis() + 10 * 60000L
            else -> initialValue
        }

        val is24Hour = when (Settings.System.getString(activity.contentResolver, Settings.System.TIME_12_24)) {
            "12" -> false
            else -> true
        }

        dialog = Dialog(activity)

        val composeView = ComposeView(activity).apply {
            setContent {
                StThemedContent {
                    var year by remember { mutableStateOf(c.get(Calendar.YEAR)) }
                    var month by remember { mutableStateOf(c.get(Calendar.MONTH)) }
                    var dayOfMonth by remember { mutableStateOf(c.get(Calendar.DAY_OF_MONTH)) }
                    var hour by remember { mutableStateOf(c.get(Calendar.HOUR_OF_DAY)) }
                    var minute by remember { mutableStateOf(c.get(Calendar.MINUTE)) }

                    Surface {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AndroidView(
                                    factory = { context ->
                                        android.widget.DatePicker(context).apply {
                                            calendarViewShown = false
                                            firstDayOfWeek = Calendar.MONDAY
                                            init(year, month, dayOfMonth) { _, y, m, d ->
                                                year = y
                                                month = m
                                                dayOfMonth = d
                                            }
                                        }
                                    }
                                )
                                AndroidView(
                                    factory = { context ->
                                        android.widget.TimePicker(context).apply {
                                            setIs24HourView(is24Hour)
                                            this.hour = hour
                                            this.minute = minute
                                            setOnTimeChangedListener { _, h, m ->
                                                hour = h
                                                minute = m
                                            }
                                        }
                                    }
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { dialog.cancel() }) {
                                    Text(stringResource(R.string.cancel))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = {
                                    dialog.dismissSafe()
                                    val resultCal = GregorianCalendar.getInstance(TimeZone.getDefault())
                                    resultCal.set(year, month, dayOfMonth, hour, minute, 0)
                                    resultCal.set(Calendar.MILLISECOND, 0)
                                    this@DlgDateTime.callback(resultCal.timeInMillis)
                                }) {
                                    Text(stringResource(R.string.ok))
                                }
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
        dialog.show()
    }
}
