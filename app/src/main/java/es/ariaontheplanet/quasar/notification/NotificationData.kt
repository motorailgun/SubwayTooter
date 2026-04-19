package es.ariaontheplanet.quasar.notification

import es.ariaontheplanet.quasar.api.entity.TootNotification
import es.ariaontheplanet.quasar.table.SavedAccount

// 通知領域に表示したいデータ
class NotificationData(
    val accessInfo: SavedAccount,
    val notification: TootNotification,
)
