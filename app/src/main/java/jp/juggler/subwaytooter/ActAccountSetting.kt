package jp.juggler.subwaytooter

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import jp.juggler.subwaytooter.actaccountsetting.AccountSettingScreen
import jp.juggler.subwaytooter.actaccountsetting.AccountSettingViewModel
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.subwaytooter.table.daoSavedAccount
import jp.juggler.util.log.LogCategory
import jp.juggler.util.long

class ActAccountSetting : ComponentActivity() {

    private val viewModel: AccountSettingViewModel by viewModels()

    companion object {

        internal val log = LogCategory("ActAccountSetting")

        internal const val KEY_ACCOUNT_DB_ID = "account_db_id"

        internal const val RESULT_INPUT_ACCESS_TOKEN = RESULT_FIRST_USER + 10
        internal const val EXTRA_DB_ID = "db_id"

        fun createIntent(activity: Activity, ai: SavedAccount) =
            Intent(activity, ActAccountSetting::class.java).apply {
                putExtra(KEY_ACCOUNT_DB_ID, ai.db_id)
            }
    }

    val account: SavedAccount? by lazy {
        intent.long(KEY_ACCOUNT_DB_ID)
            ?.let { daoSavedAccount.loadAccount(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            App1.setActivityTheme(this)

            val a = account
            if (a == null) {
                finish()
                return
            }
            
            viewModel.load(a.db_id)

            setContent {
                AccountSettingScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        } catch (ex: Throwable) {
            log.e(ex, "onCreate failed")
            finish()
        }
    }
}
