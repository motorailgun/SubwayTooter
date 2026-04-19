package es.ariaontheplanet.quasar.actmain

import es.ariaontheplanet.quasar.AppState
import es.ariaontheplanet.quasar.column.Column
import es.ariaontheplanet.quasar.column.ColumnEncoder
import es.ariaontheplanet.quasar.column.ColumnType
import es.ariaontheplanet.quasar.table.SavedAccount

/**
 * The fixed set of columns shown by ActMain after the 4-column refactor.
 * Order here determines the order in the NavigationBar.
 */
val fixedColumnTypes: List<ColumnType> = listOf(
    ColumnType.HOME,
    ColumnType.NOTIFICATIONS,
    ColumnType.LOCAL,
    ColumnType.FEDERATE,
)

/**
 * Rebuild the in-memory column list to exactly [fixedColumnTypes], each bound to
 * [account]. Disposes any existing columns first so streams and refresh timers stop.
 *
 * The list is not persisted; it is rebuilt every process start and every account
 * switch. See AppState.saveColumnList (no-op after the fixed-columns refactor).
 */
fun AppState.rebuildFixedColumns(account: SavedAccount) {
    editColumnList(save = false) { list ->
        list.forEach { it.dispose() }
        list.clear()
        fixedColumnTypes.forEach { type ->
            list.add(
                Column(
                    appState = this,
                    context = context,
                    accessInfo = account,
                    typeId = type.id,
                    columnId = ColumnEncoder.generateColumnId(),
                )
            )
        }
    }
}
