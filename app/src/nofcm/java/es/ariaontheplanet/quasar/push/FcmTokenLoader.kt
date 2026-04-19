package es.ariaontheplanet.quasar.push

import android.content.Context

@Suppress("RedundantSuspendModifier", "UNUSED_PARAMETER", "FunctionOnlyReturningConstant")
object FcmTokenLoader {
    suspend fun getToken(): String? = null
    suspend fun deleteToken() = Unit
    fun isPlayServiceAvailavle(context: Context) = false
}
