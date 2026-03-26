package jp.juggler.subwaytooter.dialog

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.ComponentDialog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import jp.juggler.subwaytooter.emoji.EmojiBase
import jp.juggler.subwaytooter.emoji.EmojiPickerScreen
import jp.juggler.subwaytooter.emoji.EmojiPickerViewModel
import jp.juggler.subwaytooter.emoji.PickerItemCustom
import jp.juggler.subwaytooter.emoji.PickerItemUnicode
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.ui.dismissSafe

private class EmojiPicker(
    private val activity: ComponentActivity,
    private val accessInfo: SavedAccount?,
    private val closeOnSelected: Boolean,
    private val onPicked: suspend (EmojiBase, bInstanceHasCustomEmoji: Boolean) -> Unit,
) {
    fun start() {
        val dialog = ComponentDialog(activity)
        dialog.window?.let { w ->
            w.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
            )
        }
        
        // Attach ViewTree owners to the DecorView (same as in ActPost fix)
        dialog.window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(dialog)
            decor.setViewTreeSavedStateRegistryOwner(dialog)
            decor.setViewTreeViewModelStoreOwner(activity)
        }
        
        val composeView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val viewModel: EmojiPickerViewModel = viewModel(activity)
                // Initialize should be called when dialog is shown
                LaunchedEffect(Unit) {
                    viewModel.initialize(accessInfo)
                }
                
                EmojiPickerScreen(
                    viewModel = viewModel,
                    accessInfo = accessInfo,
                    onPicked = { item ->
                        activity.launchAndShowError {
                            when (item) {
                                is PickerItemCustom -> {
                                    onPicked(item.customEmoji, true)
                                }
                                is PickerItemUnicode -> {
                                    onPicked(item.emoji, false)
                                }
                            }
                            if (closeOnSelected) {
                                dialog.dismissSafe()
                            }
                        }
                    }
                )
            }
        }
        
        dialog.setContentView(composeView)
        dialog.show()
    }
}

fun launchEmojiPicker(
    activity: ComponentActivity,
    accessInfo: SavedAccount?,
    closeOnSelected: Boolean,
    onPicked: suspend (EmojiBase, bInstanceHasCustomEmoji: Boolean) -> Unit,
) {
    EmojiPicker(
        activity = activity,
        accessInfo = accessInfo,
        closeOnSelected = closeOnSelected,
        onPicked = onPicked,
    ).start()
}
