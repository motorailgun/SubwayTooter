package es.ariaontheplanet.quasar.actpost

import es.ariaontheplanet.quasar.table.SavedAccount

/**
 * Stub replacement for CompletionHelper.
 * 
 * The original CompletionHelper provided autocomplete functionality for legacy View-based
 * text editing using MyEditText. Since ActPost has been migrated to Compose and uses
 * PostCompletionLogic instead, and ActMain never actually attached an EditText to this helper,
 * all methods are now no-ops.
 * 
 * This stub is kept temporarily to avoid breaking existing code that calls closeAcctPopup(),
 * setInstance(), or onDestroy().
 */
class CompletionHelper {
    
    /**
     * No-op: There's no popup to close since no EditText was ever attached.
     */
    fun closeAcctPopup() {
        // No-op
    }
    
    /**
     * No-op: AccessInfo was only used to load emoji for the autocomplete popup,
     * which is never shown.
     */
    fun setInstance(accessInfo: SavedAccount?) {
        // No-op
    }
    
    /**
     * No-op: There are no handlers or popups to clean up.
     */
    fun onDestroy() {
        // No-op
    }
}
