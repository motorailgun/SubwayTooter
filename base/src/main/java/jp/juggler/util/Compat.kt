package jp.juggler.util

import android.annotation.SuppressLint
import android.content.Context
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.content.pm.PackageManager.ResolveInfoFlags
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.annotation.AnimRes
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.CoroutineContext

/**
 * API 33 で Bundle.get() が deprecatedになる。
 * type safeにするべきだが、過去の使い方にもよるかな…
 */
private fun Bundle.getRaw(key: String) =
    @Suppress("DEPRECATION")
    get(key)

fun Intent.getUriExtra(key: String) =
    extras?.getRaw(key) as? Uri

fun Intent.getStreamUriExtra() =
    if (Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(Intent.EXTRA_STREAM) as? Uri?
    }

fun Intent.getStreamUriListExtra() =
    if (Build.VERSION.SDK_INT >= 33) {
        getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableArrayListExtra(Intent.EXTRA_STREAM)
    }

fun Intent.getIntentExtra(key: String) =
    if (Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(key, Intent::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key)
    }

/**
 * Bundleからキーを指定してint値またはnullを得る
 */
fun Bundle.boolean(key: String) =
    when (val v = getRaw(key)) {
        is Boolean -> v
        else -> null
    }

fun Bundle.string(key: String) =
    when (val v = getRaw(key)) {
        is String -> v
        else -> null
    }

/**
 * Bundleからキーを指定してint値またはnullを得る
 */
fun Bundle.int(key: String) =
    when (val v = getRaw(key)) {
        null -> null
        is Number -> v.toInt()
        is String -> v.toIntOrNull()
        else -> null
    }

/**
 * Bundleからキーを指定してlong値またはnullを得る
 */
fun Bundle.long(key: String) =
    when (val v = getRaw(key)) {
        is Number -> v.toLong()
        is String -> v.toLongOrNull()
        else -> null
    }

/**
 * IntentのExtrasからキーを指定してboolean値またはnullを得る
 */
fun Intent.boolean(key: String) = extras?.boolean(key)

/**
 * IntentのExtrasからキーを指定してint値またはnullを得る
 */
fun Intent.int(key: String) = extras?.int(key)

/**
 * IntentのExtrasからキーを指定してlong値またはnullを得る
 */
fun Intent.long(key: String) = extras?.long(key)

fun Intent.string(key: String) = extras?.string(key)

fun PackageManager.getPackageInfoCompat(
    pakageName: String,
    flags: Int = 0,
): PackageInfo? = if (Build.VERSION.SDK_INT >= 33) {
    getPackageInfo(pakageName, PackageInfoFlags.of(flags.toLong()))
} else {
    getPackageInfo(pakageName, flags)
}

@SuppressLint("QueryPermissionsNeeded")
fun PackageManager.queryIntentActivitiesCompat(
    intent: Intent,
    queryFlag: Int = 0,
): List<ResolveInfo> = if (Build.VERSION.SDK_INT >= 33) {
    queryIntentActivities(intent, ResolveInfoFlags.of(queryFlag.toLong()))
} else {
    queryIntentActivities(intent, queryFlag)
}

fun PackageManager.resolveActivityCompat(
    intent: Intent,
    queryFlag: Int = 0,
): ResolveInfo? = if (Build.VERSION.SDK_INT >= 33) {
    resolveActivity(intent, ResolveInfoFlags.of(queryFlag.toLong()))
} else {
    resolveActivity(intent, queryFlag)
}

fun ComponentActivity.backPressed(block: () -> Unit) {
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() = block()
    })
}

// 型推論できる文脈だと型名を書かずにすむ
inline fun <reified T> systemService(context: Context): T? =
    /* ContextCompat. */ ContextCompat.getSystemService(context, T::class.java)

enum class TransitionOverrideType { Open, Close, }

/**
 *
 * @param overrideType one of OVERRIDE_TRANSITION_OPEN, OVERRIDE_TRANSITION_CLOSE .
 */
fun ComponentActivity.overrideActivityTransitionCompat(
    overrideType: TransitionOverrideType,
    @AnimRes animEnter: Int,
    @AnimRes animExit: Int,
) {
    when {
        Build.VERSION.SDK_INT >= 34 -> {
            overrideActivityTransition(
                when (overrideType) {
                    TransitionOverrideType.Open ->
                        Activity.OVERRIDE_TRANSITION_OPEN

                    TransitionOverrideType.Close ->
                        Activity.OVERRIDE_TRANSITION_CLOSE
                },
                animEnter,
                animExit
            )
        }

        else -> {
            @Suppress("DEPRECATION")
            overridePendingTransition(
                animEnter,
                animExit,
            )
        }
    }
}


/**
 * suspendCancellableCoroutine 内部で使う cont.resume() の互換関数。
 */
fun <T> CancellableContinuation<T>.resumeCompat(
    value: T,
    onCancellation: ((cause: Throwable, value: T, context: CoroutineContext) -> Unit)? = null,
) = resume(value, onCancellation)

/**
 * Java 19 で deprecated になった Thread.getId() の五感関数。
 * Java 19 の threadId() が推奨されているが、
 * API 26 のエミュで使うと例外が出ていた
 * > NoSuchMethodError: No virtual method threadId()J in class Ljava/lang/Thread;
 * https://developer.android.com/build/jdks
 * によると Android 14 (API 34)で Java 17 なので、当面は Thread.getId() を使うことになる。
 */
@Suppress("DEPRECATION")
val Thread.idCompat: Long
    get() = id
