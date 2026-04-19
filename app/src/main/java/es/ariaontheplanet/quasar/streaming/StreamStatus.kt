package es.ariaontheplanet.quasar.streaming

import es.ariaontheplanet.quasar.column.Column
import es.ariaontheplanet.quasar.column.canStreamingType

// ストリーミング接続の状態
enum class StreamStatus {
    Missing,
    Closed,
    Connecting,
    Open,
    Subscribed,
    ClosedNoRetry,
}

fun Column.getStreamingStatus() = when {
    canStreamingType() && !dontStreaming -> appState.streamManager.getStreamStatus(this)
    else -> StreamStatus.Missing
}
