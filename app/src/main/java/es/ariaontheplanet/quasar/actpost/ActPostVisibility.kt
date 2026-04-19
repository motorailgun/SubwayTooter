package es.ariaontheplanet.quasar.actpost

import androidx.appcompat.app.AlertDialog
import es.ariaontheplanet.quasar.ActPost
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.api.entity.InstanceCapability
import es.ariaontheplanet.quasar.api.entity.TootInstance
import es.ariaontheplanet.quasar.api.entity.TootVisibility
import es.ariaontheplanet.quasar.getVisibilityCaption
import es.ariaontheplanet.quasar.getVisibilityIconId

fun ActPost.showVisibility() {
    visibilityIconRes = (states.visibility ?: TootVisibility.Public)
        .getVisibilityIconId(account?.isMisskey == true)
}

fun ActPost.openVisibilityPicker() {
    val ti = TootInstance.getCached(account)

    val list = when {
        account?.isMisskey == true -> arrayOf(
            //	TootVisibility.WebSetting,
            TootVisibility.Public,
            TootVisibility.UnlistedHome,
            TootVisibility.PrivateFollowers,
            TootVisibility.LocalPublic,
            TootVisibility.LocalHome,
            TootVisibility.LocalFollowers,
            TootVisibility.DirectSpecified,
            TootVisibility.DirectPrivate
        )

        InstanceCapability.visibilityMutual(ti) -> arrayOf(
            TootVisibility.WebSetting,
            TootVisibility.Public,
            TootVisibility.UnlistedHome,
            TootVisibility.PrivateFollowers,
            TootVisibility.Limited,
            TootVisibility.Mutual,
            TootVisibility.DirectSpecified
        )

        InstanceCapability.visibilityLimited(ti) -> arrayOf(
            TootVisibility.WebSetting,
            TootVisibility.Public,
            TootVisibility.UnlistedHome,
            TootVisibility.PrivateFollowers,
            TootVisibility.Limited,
            TootVisibility.DirectSpecified
        )

        else -> arrayOf(
            TootVisibility.WebSetting,
            TootVisibility.Public,
            TootVisibility.UnlistedHome,
            TootVisibility.PrivateFollowers,
            TootVisibility.DirectSpecified
        )
    }
    val captionList = list
        .map { getVisibilityCaption(this, account?.isMisskey == true, it) }
        .toTypedArray()

    AlertDialog.Builder(this)
        .setTitle(R.string.choose_visibility)
        .setNegativeButton(R.string.cancel, null)
        .setItems(captionList) { _, which ->
            list.elementAtOrNull(which)?.let {
                states.visibility = it
                showVisibility()
            }
        }
        .show()
}
