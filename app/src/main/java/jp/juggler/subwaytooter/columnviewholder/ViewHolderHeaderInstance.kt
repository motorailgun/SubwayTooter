package jp.juggler.subwaytooter.columnviewholder

import android.content.Intent
import android.text.SpannableStringBuilder
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.action.serverProfileDirectoryFromInstanceInformation
import jp.juggler.subwaytooter.action.timeline
import jp.juggler.subwaytooter.actmain.nextPosition
import jp.juggler.subwaytooter.api.entity.Host
import jp.juggler.subwaytooter.api.entity.TootInstance
import jp.juggler.subwaytooter.column.Column
import jp.juggler.subwaytooter.column.ColumnType
import jp.juggler.subwaytooter.util.DecodeOptions
import jp.juggler.subwaytooter.util.emojiSizeMode
import jp.juggler.subwaytooter.util.openBrowser
import jp.juggler.subwaytooter.util.openCustomTab
import jp.juggler.subwaytooter.view.MyLinkMovementMethod
import jp.juggler.subwaytooter.view.MyNetworkImageView
import jp.juggler.subwaytooter.view.MyTextView
import jp.juggler.util.data.anyArrayOf
import jp.juggler.util.data.neatSpaces
import jp.juggler.util.data.notEmpty
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import jp.juggler.util.ui.attrColor
import jp.juggler.util.ui.dp
import jp.juggler.util.ui.isEnabledAlpha
import org.conscrypt.OpenSSLX509Certificate

internal class ViewHolderHeaderInstance(
    override val activity: ActMain,
    parent: ViewGroup,
) : ViewHolderHeaderBase(
    LinearLayout(parent.context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        orientation = LinearLayout.VERTICAL
        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        val ctx = context
        val p12 = ctx.dp(12)
        setPadding(p12, p12, p12, ctx.dp(128))
    }
), View.OnClickListener {

    companion object {
        private val log = LogCategory("ViewHolderHeaderInstance")
    }

    private var instance: TootInstance? = null

    private lateinit var btnInstance: Button
    private lateinit var tvVersion: TextView
    private lateinit var tvTitle: TextView
    private lateinit var btnEmail: Button
    private lateinit var btnContact: Button
    private lateinit var tvLanguages: TextView
    private lateinit var tvInvitesEnabled: TextView
    private lateinit var tvUserCount: TextView
    private lateinit var tvTootCount: TextView
    private lateinit var tvDomainCount: TextView
    private lateinit var ivThumbnail: MyNetworkImageView
    private lateinit var tvDescription: MyTextView
    private lateinit var tvDescriptionLong: MyTextView
    private lateinit var btnAbout: Button
    private lateinit var btnAboutMore: Button
    private lateinit var btnExplore: Button
    private lateinit var tvConfiguration: MyTextView
    private lateinit var tvFedibirdCapacities: MyTextView
    private lateinit var tvPlelomaFeatures: MyTextView
    private lateinit var tvHandshake: MyTextView

    private fun LinearLayout.addDivider() {
        addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                context.dp(1)
            ).apply {
                topMargin = context.dp(12)
                bottomMargin = context.dp(12)
            }
            setBackgroundColor(context.attrColor(R.attr.colorSettingDivider))
        })
    }

    private fun LinearLayout.addLabel(stringResId: Int) {
        addView(TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 14f
            text = context.getString(stringResId)
        })
    }

    private fun LinearLayout.addSettingButton(text: String? = null, textResId: Int = 0): Button {
        val btn = Button(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = context.dp(36)
            }
            setBackgroundResource(R.drawable.btn_bg_transparent_round6dp)
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            isAllCaps = false
            setPaddingRelative(context.dp(12), 0, 0, 0)
            if (textResId != 0) setText(textResId)
            if (text != null) this.text = text
        }
        addView(btn)
        return btn
    }

    private fun LinearLayout.addFormRow(init: LinearLayout.() -> Unit): LinearLayout {
        val row = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = context.dp(32)
            }
            orientation = LinearLayout.HORIZONTAL
            isBaselineAligned = true
            init()
        }
        addView(row)
        return row
    }

    private fun LinearLayout.addStretchTextView(): TextView {
        val tv = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        addView(tv)
        return tv
    }

    private fun LinearLayout.addFormTextView(): MyTextView {
        val tv = MyTextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = context.dp(32)
            }
        }
        addView(tv)
        return tv
    }

    init {
        val root = itemView as LinearLayout
        root.tag = this

        // Instance
        root.addDivider()
        root.addLabel(R.string.instance)
        btnInstance = root.addSettingButton()

        // Version
        root.addDivider()
        root.addLabel(R.string.version)
        root.addFormRow {
            tvVersion = addStretchTextView()
        }

        // Title
        root.addDivider()
        root.addLabel(R.string.title)
        root.addFormRow {
            tvTitle = addStretchTextView()
        }

        // Email
        root.addDivider()
        root.addLabel(R.string.email)
        btnEmail = root.addSettingButton()

        // Contact
        root.addDivider()
        root.addLabel(R.string.contact)
        btnContact = root.addSettingButton()

        // Languages
        root.addDivider()
        root.addLabel(R.string.languages)
        root.addFormRow {
            tvLanguages = addStretchTextView()
            tvLanguages.setBackgroundResource(R.drawable.btn_bg_transparent_round6dp)
            tvLanguages.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            tvLanguages.isAllCaps = false
        }

        // Invites Enabled
        root.addDivider()
        root.addLabel(R.string.invites_enabled)
        root.addFormRow {
            tvInvitesEnabled = addStretchTextView()
            tvInvitesEnabled.setBackgroundResource(R.drawable.btn_bg_transparent_round6dp)
            tvInvitesEnabled.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            tvInvitesEnabled.isAllCaps = false
        }

        // User Count
        root.addDivider()
        root.addLabel(R.string.user_count)
        root.addFormRow {
            tvUserCount = addStretchTextView()
        }

        // Toot Count
        root.addDivider()
        root.addLabel(R.string.toot_count)
        root.addFormRow {
            tvTootCount = addStretchTextView()
        }

        // Domain Count
        root.addDivider()
        root.addLabel(R.string.domain_count)
        tvDomainCount = root.addFormTextView()

        // Thumbnail
        root.addDivider()
        root.addLabel(R.string.thumbnail)
        root.addFormRow {
            ivThumbnail = MyNetworkImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    context.dp(120)
                )
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            addView(ivThumbnail)
        }

        // Description
        root.addDivider()
        root.addLabel(R.string.description)
        tvDescription = root.addFormTextView()
        tvDescription.movementMethod = MyLinkMovementMethod

        // Description Long
        root.addDivider()
        root.addLabel(R.string.description_long)
        tvDescriptionLong = root.addFormTextView()
        tvDescriptionLong.movementMethod = MyLinkMovementMethod

        // Links
        root.addDivider()
        root.addLabel(R.string.links)
        btnAbout = root.addSettingButton(textResId = R.string.top_page)
        btnAboutMore = root.addSettingButton(textResId = R.string.about_this_instance)
        btnExplore = root.addSettingButton(textResId = R.string.profile_directory)

        // Configuration
        root.addDivider()
        root.addLabel(R.string.server_configuration)
        tvConfiguration = root.addFormTextView()

        // Fedibird Capacities
        root.addDivider()
        root.addLabel(R.string.fedibird_capacities)
        tvFedibirdCapacities = root.addFormTextView()

        // Pleroma Features
        root.addDivider()
        root.addLabel(R.string.pleroma_features)
        tvPlelomaFeatures = root.addFormTextView()

        // TLS Handshake
        root.addDivider()
        root.addLabel(R.string.tls_handshake)
        tvHandshake = root.addFormTextView()
        root.addDivider()

        // Click listeners
        arrayOf(
            btnInstance, btnEmail, btnContact, ivThumbnail,
            btnAbout, btnAboutMore, btnExplore,
        ).forEach { it.setOnClickListener(this) }
    }

    override fun showColor() {
        //
    }

    override fun bindData(column: Column) {
        super.bindData(column)
        val instance = column.instanceInformation
        this@ViewHolderHeaderInstance.instance = instance

        if (instance == null) {
            btnInstance.text = "?"
            tvVersion.text = "?"
            tvTitle.text = "?"
            btnEmail.text = "?"
            btnEmail.isEnabledAlpha = false
            tvDescription.text = "?"
            tvDescriptionLong.text = "?"
            ivThumbnail.setImageUrl(0f, null)
            tvLanguages.text = "?"
            tvInvitesEnabled.text = "?"
            btnContact.text = "?"
            btnContact.isEnabledAlpha = false
            btnAbout.isEnabledAlpha = false
            btnAboutMore.isEnabledAlpha = false
            btnExplore.isEnabledAlpha = false
            tvConfiguration.text = ""
            tvFedibirdCapacities.text = ""
            tvPlelomaFeatures.text = ""
        } else {
            val domain = instance.apDomain
            btnInstance.text = when {
                domain.pretty != domain.ascii -> "${domain.pretty}\n${domain.ascii}"
                else -> domain.ascii
            }

            btnInstance.isEnabledAlpha = true
            btnAbout.isEnabledAlpha = true
            btnAboutMore.isEnabledAlpha = true
            btnExplore.isEnabledAlpha = true

            tvVersion.text = instance.version ?: ""
            tvTitle.text = instance.title ?: ""

            val email = instance.email ?: ""
            btnEmail.text = email
            btnEmail.isEnabledAlpha = email.isNotEmpty()

            val contactAcct =
                instance.contact_account?.let { who -> "@${who.username}@${who.apDomain.pretty}" }
                    ?: ""
            btnContact.text = contactAcct
            btnContact.isEnabledAlpha = contactAcct.isNotEmpty()

            tvLanguages.text = instance.languages?.joinToString(", ") ?: ""
            tvInvitesEnabled.text = when (instance.invites_enabled) {
                null -> "?"
                true -> activity.getString(R.string.yes)
                false -> activity.getString(R.string.no)
            }

            val options = DecodeOptions(
                activity,
                accessInfo,
                decodeEmoji = true,
                authorDomain = accessInfo,
                emojiSizeMode = accessInfo.emojiSizeMode(),
            )

            tvDescription.text = options
                .decodeHTML("<p>${instance.description ?: ""}</p>")
                .neatSpaces()

            tvDescriptionLong.text = options
                .decodeHTML("<p>${instance.descriptionOld ?: ""}</p>")
                .neatSpaces()

            val stats = instance.stats
            if (stats == null) {
                tvUserCount.setText(R.string.not_provided_mastodon_under_1_6)
                tvTootCount.setText(R.string.not_provided_mastodon_under_1_6)
                tvDomainCount.setText(R.string.not_provided_mastodon_under_1_6)
            } else {
                tvUserCount.text = stats.user_count.toString(10)
                tvTootCount.text = stats.status_count.toString(10)
                tvDomainCount.text = stats.domain_count.toString(10)
            }

            val thumbnail = instance.thumbnail?.let {
                if (it.startsWith("/")) {
                    "https://${instance.apiHost.ascii}$it"
                } else {
                    it
                }
            }.notEmpty()
            ivThumbnail.setImageUrl(0f, thumbnail, thumbnail)

            tvConfiguration.text =
                instance.configuration?.toString(1, sort = true) ?: ""
            tvFedibirdCapacities.text =
                instance.fedibirdCapabilities?.sorted()?.joinToString("\n") ?: ""
            tvPlelomaFeatures.text =
                instance.pleromaFeatures?.sorted()?.joinToString("\n") ?: ""
        }

        tvHandshake.text = when (val handshake = column.handshake) {
            null -> ""
            else -> {
                val sb = SpannableStringBuilder(
                    "${handshake.tlsVersion}, ${handshake.cipherSuite}"
                )
                val certs = handshake.peerCertificates.joinToString("\n") { cert ->
                    "\n============================\n" +
                            if (cert is OpenSSLX509Certificate) {

                                log.d(cert.toString())

                                """
                                Certificate : ${cert.type}
                                subject : ${cert.subjectDN}
                                subjectAlternativeNames : ${
                                    cert.subjectAlternativeNames
                                        ?.joinToString(", ") {
                                            try {
                                                it?.last()
                                            } catch (ignored: Throwable) {
                                                it
                                            }
                                                ?.toString() ?: "null"
                                        }
                                }
                                issuer : ${cert.issuerX500Principal}
                                end : ${cert.notAfter}
                                """.trimIndent()
                            } else {
                                cert.javaClass.name + "\n" + cert.toString()
                            }
                }
                if (certs.isNotEmpty()) {
                    sb.append('\n')
                    sb.append(certs)
                }
                sb
            }
        }
    }

    override fun onClick(v: View) {
        val host = Host.parse(column.instanceUri)
        when (v) {
            btnEmail -> instance?.email?.let { email ->
                try {
                    if (email.contains("://")) {
                        activity.openCustomTab(email)
                    } else {
                        val intent = Intent(Intent.ACTION_SEND)
                        intent.type = "text/plain"
                        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                        intent.putExtra(Intent.EXTRA_TEXT, email)
                        activity.startActivity(intent)
                    }
                } catch (ex: Throwable) {
                    log.e(ex, "startActivity failed. mail=$email")
                    activity.showToast(true, R.string.missing_mail_app)
                }
            }

            btnContact -> instance?.contact_account?.let { who ->
                activity.timeline(
                    activity.nextPosition(column),
                    ColumnType.SEARCH,
                    args = anyArrayOf("@${who.username}@${who.apDomain.ascii}", true)
                )
            }

            btnInstance ->
                activity.openBrowser("https://${host.ascii}/about")

            ivThumbnail ->
                activity.openBrowser(instance?.thumbnail)

            btnAbout ->
                activity.openBrowser("https://${host.ascii}/about")

            btnAboutMore ->
                activity.openBrowser("https://${host.ascii}/about/more")

            btnExplore -> activity.serverProfileDirectoryFromInstanceInformation(
                column,
                host,
                instance = instance
            )
        }
    }

    override fun onViewRecycled() {
    }
}
