package jp.juggler.subwaytooter

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jp.juggler.subwaytooter.compose.StScreen
import jp.juggler.subwaytooter.util.StColorScheme
import jp.juggler.subwaytooter.util.getStColorTheme
import jp.juggler.subwaytooter.util.openBrowser
import jp.juggler.util.getPackageInfoCompat
import jp.juggler.util.log.LogCategory

class ActAbout : ComponentActivity() {

    class Translators(
        val name: String,
        val acct: String?,
        val lang: String,
    )

    companion object {

        val log = LogCategory("ActAbout")

        const val EXTRA_SEARCH = "search"

        const val developer_acct = "tateisu@mastodon.juggler.jp"
        const val official_acct = "SubwayTooter@mastodon.juggler.jp"

        const val url_release = "https://github.com/tateisu/SubwayTooter/releases"

        const val url_weblate = "https://hosted.weblate.org/projects/subway-tooter/"

        val translators = arrayOf(
            Translators("Allan Nordhøy", null, "English, Norwegian Bokmål"),
            Translators("ayiniho", null, "French"),
            Translators("ButterflyOfFire", "@ButterflyOfFire@mstdn.fr", "Arabic, French, Kabyle"),
            Translators("Ch", null, "Korean"),
            Translators("chinnux", "@chinnux@neko.ci", "Chinese (Simplified)"),
            Translators("Dyxang", null, "Chinese (Simplified)"),
            Translators("Elizabeth Sherrock", null, "Chinese (Simplified)"),
            Translators("Gennady Archangorodsky", null, "Hebrew"),
            Translators("inqbs Siina", null, "Korean"),
            Translators("J. Lavoie", null, "French, German"),
            Translators("Jeong Arm", "@jarm@qdon.space", "Korean"),
            Translators("Joan Pujolar", "@jpujolar@mastodont.cat", "Catalan"),
            Translators("Kai Zhang", "@bearzk@mastodon.social", "Chinese (Simplified)"),
            Translators("koyu", null, "German"),
            Translators("Liaizon Wakest", null, "English"),
            Translators("lingcas", null, "Chinese (Traditional)"),
            Translators("Love Xu", null, "Chinese (Simplified)"),
            Translators("lptprjh", null, "Korean"),
            Translators("mv87", null, "German"),
            Translators("mynameismonkey", null, "Welsh"),
            Translators("Nathan", null, "French"),
            Translators("Niek Visser", null, "Dutch"),
            Translators("Owain Rhys Lewis", null, "Welsh"),
            Translators("Remi Rampin", null, "French"),
            Translators("Sachin", null, "Kannada"),
            Translators("Swann Martinet", null, "French"),
            Translators("takubunn", null, "Chinese (Simplified)"),
            Translators("Whod", null, "Bulgarian"),
            Translators("yucj", null, "Chinese (Traditional)"),
            Translators("邓志诚", null, "Chinese (Simplified)"),
            Translators("배태길", null, "Korea"),
        )
    }

    private fun searchAcct(acct: String) {
        setResult(RESULT_OK, Intent().apply { putExtra(EXTRA_SEARCH, acct) })
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App1.setActivityTheme(this)
        val stColorScheme = getStColorTheme()

        val versionName = try {
            packageManager.getPackageInfoCompat(packageName)?.versionName ?: "?"
        } catch (ex: Throwable) {
            log.e(ex, "can't get app version.")
            "?"
        }

        setContent {
            AboutScreen(stColorScheme, versionName)
        }
    }

    @Composable
    private fun AboutScreen(stColorScheme: StColorScheme, versionName: String) {
        StScreen(
            stColorScheme = stColorScheme,
            title = stringResource(R.string.app_name),
            onBack = { finish() },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = getString(R.string.version_is, versionName),
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { searchAcct(developer_acct) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(getString(R.string.search_for, developer_acct))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { searchAcct(official_acct) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(getString(R.string.search_for, official_acct))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { openBrowser(url_release) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(url_release)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { openBrowser(url_weblate) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(getString(R.string.please_help_translation))
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Contributors",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))

                for (who in translators) {
                    val acct = who.acct ?: "@?@?"
                    TextButton(
                        onClick = {
                            val data = Intent()
                            data.putExtra(EXTRA_SEARCH, who.acct ?: who.name)
                            setResult(RESULT_OK, data)
                            finish()
                        },
                    ) {
                        Text(
                            text = "${who.name}\n$acct\n${getString(R.string.thanks_for, who.lang)}",
                        )
                    }
                }
            }
        }
    }
}
