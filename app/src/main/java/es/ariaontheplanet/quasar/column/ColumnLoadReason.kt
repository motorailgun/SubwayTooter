package es.ariaontheplanet.quasar.column

enum class ColumnLoadReason {
    PageSelect,
    RefreshAfterPost,
    TokenUpdated,
    OpenPush,
    ContentInvalidated,
    PullToRefresh,
    SettingChange,
    ForceReload,
}
