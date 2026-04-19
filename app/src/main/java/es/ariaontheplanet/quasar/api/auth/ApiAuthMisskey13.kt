package es.ariaontheplanet.quasar.api.auth

import android.net.Uri
import androidx.core.net.toUri
import es.ariaontheplanet.quasar.api.TootApiClient
import es.ariaontheplanet.quasar.api.entity.Host
import es.ariaontheplanet.quasar.api.send
import es.ariaontheplanet.quasar.column.encodeQuery
import jp.juggler.util.data.JsonObject
import jp.juggler.util.data.jsonObjectOf
import jp.juggler.util.network.toPostRequestBuilder

class ApiAuthMisskey13(val client: TootApiClient) {

    /**
     * miauth のブラウザ認証URLを作成する
     */
    fun createAuthUrl(
        apiHost: Host,
        clientName: String,
        iconUrl: String,
        callbackUrl: String,
        permission: String,
        sessionId: String,
    ): Uri = jsonObjectOf(
        "name" to clientName,
        "icon" to iconUrl,
        "callback" to callbackUrl,
        "permission" to permission
    ).encodeQuery()
        .let { "https://${apiHost.ascii}/miauth/$sessionId?$it" }
        .toUri()

    /**
     * miauthの認証結果を確認する
     */
    suspend fun checkAuthSession(
        apiHost: Host,
        sessionId: String,
    ): JsonObject = JsonObject(/*empty*/)
        .toPostRequestBuilder()
        .url("https://${apiHost.ascii}/api/miauth/$sessionId/check")
        .build()
        .send(client, errorSuffix = apiHost.pretty)
        .readJsonObject()
}
