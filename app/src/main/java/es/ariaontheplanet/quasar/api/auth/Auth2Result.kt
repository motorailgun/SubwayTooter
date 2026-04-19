package es.ariaontheplanet.quasar.api.auth

import es.ariaontheplanet.quasar.api.entity.TootAccount
import es.ariaontheplanet.quasar.api.entity.TootInstance
import jp.juggler.util.data.JsonObject

/**
 * ブラウザで認証してコールバックURLで戻ってきて、
 * そのURLを使って認証した結果
 */
class Auth2Result(
    // サーバ情報
    val tootInstance: TootInstance,

    // アクセストークンを含むJsonObject
    val tokenJson: JsonObject,

    // TootAccountユーザ情報の元となるJSONデータ
    val accountJson: JsonObject,

    // AccountJsonのパース結果
    val tootAccount: TootAccount,
) {
    // 対象サーバのAPIホスト
    val apiHost get() = tootInstance.apiHost

    // サーバ情報から取得したActivityPubドメイン
    val apDomain get() = tootInstance.apDomain
}
