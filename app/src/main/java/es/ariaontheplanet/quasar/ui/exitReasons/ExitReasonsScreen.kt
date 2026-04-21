package es.ariaontheplanet.quasar.ui.exitReasons

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import es.ariaontheplanet.quasar.api.entity.TootStatus
import jp.juggler.util.data.decodeUTF8
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.withCaption

private val log = LogCategory("ExitReasonsScreen")

@Composable
fun ExitReasonsScreen() {
    val context = LocalContext.current
    val exitInfoList = remember(context) { loadExitInfo(context) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(exitInfoList) { info ->
            ExitReasonItem(context, info)
            HorizontalDivider()
        }
    }
}

private fun loadExitInfo(context: Context): List<ApplicationExitInfo> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()
    val am = context.getSystemService(ActivityManager::class.java) ?: run {
        log.e("can't find ActivityManager")
        return emptyList()
    }
    return am.getHistoricalProcessExitReasons(null, 0, 200)
        .filterNotNull()
        .toList()
}

@Composable
private fun ExitReasonItem(context: Context, info: ApplicationExitInfo) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

    val trace = try {
        info.traceInputStream?.use { it.readBytes().decodeUTF8() } ?: "(null)"
    } catch (ex: Throwable) {
        ex.withCaption("can't read traceInputStream")
    }

    Text(
        text = """
timestamp=${TootStatus.formatTime(context, info.timestamp, bAllowRelative = false)}
importance=${info.importance}
pss=${info.pss}
rss=${info.rss}
reason=${reasonString(info.reason)}
status=${info.status}
description=${info.description}
trace=$trace
        """.trimIndent(),
        modifier = Modifier.padding(12.dp),
    )
}

internal fun reasonString(v: Int) = when (v) {
    ApplicationExitInfo.REASON_ANR ->
        "REASON_ANR Application process was killed due to being unresponsive (ANR)."

    ApplicationExitInfo.REASON_CRASH ->
        "REASON_CRASH Application process died because of an unhandled exception in Java code."

    ApplicationExitInfo.REASON_CRASH_NATIVE ->
        "REASON_CRASH_NATIVE Application process died because of a native code crash."

    ApplicationExitInfo.REASON_DEPENDENCY_DIED ->
        "REASON_DEPENDENCY_DIED Application process was killed because its dependency was going away."

    ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE ->
        "REASON_EXCESSIVE_RESOURCE_USAGE Application process was killed by the system due to excessive resource usage."

    ApplicationExitInfo.REASON_EXIT_SELF ->
        "REASON_EXIT_SELF Application process exit normally by itself."

    ApplicationExitInfo.REASON_INITIALIZATION_FAILURE ->
        "REASON_INITIALIZATION_FAILURE Application process was killed because of initialization failure."

    ApplicationExitInfo.REASON_LOW_MEMORY ->
        "REASON_LOW_MEMORY Application process was killed by the system low memory killer."

    ApplicationExitInfo.REASON_OTHER ->
        "REASON_OTHER Application process was killed by the system for various other reasons."

    ApplicationExitInfo.REASON_PERMISSION_CHANGE ->
        "REASON_PERMISSION_CHANGE Application process was killed due to a runtime permission change."

    ApplicationExitInfo.REASON_SIGNALED ->
        "REASON_SIGNALED Application process died due to the result of an OS signal."

    ApplicationExitInfo.REASON_UNKNOWN ->
        "REASON_UNKNOWN Application process died due to unknown reason."

    ApplicationExitInfo.REASON_USER_REQUESTED ->
        "REASON_USER_REQUESTED Application process was killed because of the user request."

    ApplicationExitInfo.REASON_USER_STOPPED ->
        "REASON_USER_STOPPED Application process was killed, because the user it is running as on devices with multiple users, was stopped."

    else -> "?($v)"
}
