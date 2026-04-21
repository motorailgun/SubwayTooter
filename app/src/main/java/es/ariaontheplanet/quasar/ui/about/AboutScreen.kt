package es.ariaontheplanet.quasar.ui.about

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.LocalActivity
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.util.openBrowser
import jp.juggler.util.getPackageInfoCompat
import jp.juggler.util.log.LogCategory

const val EXTRA_ABOUT_SEARCH = "search"

private const val DEVELOPER_ACCT = "tateisu@mastodon.juggler.jp"
private const val OFFICIAL_ACCT = "SubwayTooter@mastodon.juggler.jp"
private const val URL_RELEASE = "https://github.com/tateisu/SubwayTooter/releases"
private const val URL_WEBLATE = "https://hosted.weblate.org/projects/subway-tooter/"

private val log = LogCategory("AboutScreen")

private class Translator(val name: String, val acct: String?, val lang: String)

private val translators = arrayOf(
    Translator("Allan Nordhøy", null, "English, Norwegian Bokmål"),
    Translator("ayiniho", null, "French"),
    Translator("ButterflyOfFire", "@ButterflyOfFire@mstdn.fr", "Arabic, French, Kabyle"),
    Translator("Ch", null, "Korean"),
    Translator("chinnux", "@chinnux@neko.ci", "Chinese (Simplified)"),
    Translator("Dyxang", null, "Chinese (Simplified)"),
    Translator("Elizabeth Sherrock", null, "Chinese (Simplified)"),
    Translator("Gennady Archangorodsky", null, "Hebrew"),
    Translator("inqbs Siina", null, "Korean"),
    Translator("J. Lavoie", null, "French, German"),
    Translator("Jeong Arm", "@jarm@qdon.space", "Korean"),
    Translator("Joan Pujolar", "@jpujolar@mastodont.cat", "Catalan"),
    Translator("Kai Zhang", "@bearzk@mastodon.social", "Chinese (Simplified)"),
    Translator("koyu", null, "German"),
    Translator("Liaizon Wakest", null, "English"),
    Translator("lingcas", null, "Chinese (Traditional)"),
    Translator("Love Xu", null, "Chinese (Simplified)"),
    Translator("lptprjh", null, "Korean"),
    Translator("mv87", null, "German"),
    Translator("mynameismonkey", null, "Welsh"),
    Translator("Nathan", null, "French"),
    Translator("Niek Visser", null, "Dutch"),
    Translator("Owain Rhys Lewis", null, "Welsh"),
    Translator("Remi Rampin", null, "French"),
    Translator("Sachin", null, "Kannada"),
    Translator("Swann Martinet", null, "French"),
    Translator("takubunn", null, "Chinese (Simplified)"),
    Translator("Whod", null, "Bulgarian"),
    Translator("yucj", null, "Chinese (Traditional)"),
    Translator("邓志诚", null, "Chinese (Simplified)"),
    Translator("배태길", null, "Korea"),
)

@Composable
fun AboutScreen() {
    val activity = LocalActivity.current
    val context = LocalContext.current

    val versionName = remember(context) {
        try {
            context.packageManager.getPackageInfoCompat(context.packageName)?.versionName ?: "?"
        } catch (ex: Throwable) {
            log.e(ex, "can't get app version.")
            "?"
        }
    }

    fun searchAcct(acct: String) {
        activity ?: return
        activity.setResult(
            Activity.RESULT_OK,
            Intent().apply { putExtra(EXTRA_ABOUT_SEARCH, acct) },
        )
        activity.finish()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(text = context.getString(R.string.version_is, versionName))
        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { searchAcct(DEVELOPER_ACCT) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(context.getString(R.string.search_for, DEVELOPER_ACCT)) }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { searchAcct(OFFICIAL_ACCT) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(context.getString(R.string.search_for, OFFICIAL_ACCT)) }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { activity?.openBrowser(android.net.Uri.parse(URL_RELEASE)) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(URL_RELEASE) }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { activity?.openBrowser(android.net.Uri.parse(URL_WEBLATE)) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(context.getString(R.string.please_help_translation)) }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Contributors", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        for (who in translators) {
            val acct = who.acct ?: "@?@?"
            TextButton(onClick = { searchAcct(who.acct ?: who.name) }) {
                Text(
                    text = "${who.name}\n$acct\n${context.getString(R.string.thanks_for, who.lang)}",
                )
            }
        }
    }
}
