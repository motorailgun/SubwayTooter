package es.ariaontheplanet.quasar.api

interface TootApiCallback {
    suspend fun isApiCancelled(): Boolean
    suspend fun publishApiProgress(s: String) {}
    suspend fun publishApiProgressRatio(value: Int, max: Int) {}
}
