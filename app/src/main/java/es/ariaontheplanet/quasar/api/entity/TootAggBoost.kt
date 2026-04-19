package es.ariaontheplanet.quasar.api.entity

class TootAggBoost(
    val originalStatus: TootStatus,
    val boosterStatuses : List<TootStatus>
) : TimelineItem()