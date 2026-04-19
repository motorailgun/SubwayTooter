package es.ariaontheplanet.quasar

object ReleaseType {
    val isDebug = "true".toBoolean()
    val isRelease = !isDebug
}
