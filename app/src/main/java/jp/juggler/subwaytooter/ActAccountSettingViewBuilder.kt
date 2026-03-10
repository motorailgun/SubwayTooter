package jp.juggler.subwaytooter

import android.content.Context
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import jp.juggler.subwaytooter.view.MyNetworkImageView
import jp.juggler.util.ui.attrColor
import jp.juggler.util.ui.dp

// Style helper: setting_divider
private fun Context.settingDivider() = View(this).apply {
    layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
    ).apply {
        topMargin = dp(12)
        bottomMargin = dp(12)
    }
    setPadding(0, 0, 0, 0)
    setBackgroundColor(attrColor(R.attr.colorSettingDivider))
}

// Style helper: setting_row_label
private fun Context.settingRowLabel(textResId: Int, labelFor: Int = 0) = TextView(this).apply {
    layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
    setPaddingRelative(0, 0, 0, 0)
    textSize = 14f
    setText(textResId)
    if (labelFor != 0) setLabelFor(labelFor)
}

// Style helper: setting_row_label with layout_width=0dp, weight=1
private fun Context.settingRowLabelStretch(textResId: Int) = TextView(this).apply {
    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
    setPaddingRelative(0, 0, 0, 0)
    textSize = 14f
    gravity = Gravity.CENTER_VERTICAL
    setText(textResId)
}

// Style helper: setting_row_label_indent1
private fun Context.settingRowLabelIndent1(textResId: Int) = TextView(this).apply {
    layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        topMargin = dp(3)
    }
    setPaddingRelative(dp(48), 0, 0, 0)
    textSize = 14f
    setText(textResId)
}

// Style helper: setting_row_form (LinearLayout horizontal)
private fun Context.settingRowForm(block: LinearLayout.() -> Unit = {}) = LinearLayout(this).apply {
    layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        marginStart = dp(32)
        marginEnd = 0
    }
    orientation = LinearLayout.HORIZONTAL
    isBaselineAligned = true
    block()
}

// Style helper: setting_row_form_fields (inherits from setting_row_form, adds extra padding)
private fun Context.settingRowFormFields(block: LinearLayout.() -> Unit = {}) =
    LinearLayout(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = dp(32)
            marginEnd = 0
        }
        orientation = LinearLayout.HORIZONTAL
        isBaselineAligned = true
        setPaddingRelative(dp(8), 0, 0, 0)
        block()
    }

// Style helper: setting_horizontal_stretch (0dp width, weight=1)
private fun horizontalStretchParams() = LinearLayout.LayoutParams(
    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
)

// Style helper: setting_row_button
private fun Context.settingRowButton(textResId: Int, id: Int) = Button(this).apply {
    this.id = id
    layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        marginStart = dp(36)
        marginEnd = 0
    }
    background = resources.getDrawable(R.drawable.btn_bg_transparent_round6dp, theme)
    gravity = Gravity.START or Gravity.CENTER_VERTICAL
    isAllCaps = false
    setPaddingRelative(dp(12), paddingTop, paddingEnd, paddingBottom)
    setText(textResId)
}

// Style helper: setting_edit_text (inherits horizontal_stretch, adds imeOptions)
private fun Context.settingEditText(id: Int, inputTypeVal: Int = InputType.TYPE_CLASS_TEXT) =
    EditText(this).apply {
        this.id = id
        layoutParams = horizontalStretchParams()
        imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
        inputType = inputTypeVal
    }

// Style helper: setting_edit_text_indent1
private fun Context.settingEditTextIndent1(id: Int, inputTypeVal: Int = InputType.TYPE_CLASS_TEXT) =
    EditText(this).apply {
        this.id = id
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = dp(48)
            marginEnd = 0
        }
        imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
        gravity = Gravity.CENTER
        inputType = inputTypeVal
    }

// Style helper: setting_spinner_indent1
private fun Context.settingSpinnerIndent1(id: Int) = Spinner(this).apply {
    this.id = id
    layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        marginStart = dp(48)
        marginEnd = 0
    }
    minimumHeight = dp(40)
}

// Helper: CheckBox with setting_row_form style
private fun Context.settingCheckBox(id: Int, textResId: Int) = CheckBox(this).apply {
    this.id = id
    layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        marginStart = dp(32)
        marginEnd = 0
    }
    setText(textResId)
}

// Helper: SwitchCompat row (switch + spacer)
private fun Context.settingSwitchRow(switchId: Int) = settingRowForm {
    addView(SwitchCompat(this@settingSwitchRow).apply {
        id = switchId
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    })
    addView(View(this@settingSwitchRow).apply {
        layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
    })
}

// Helper: ImageButton with 48dp size, transparent round background
private fun Context.settingImageButton(
    id: Int,
    iconResId: Int,
    contentDescResId: Int,
) = ImageButton(this).apply {
    this.id = id
    layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply {
        marginStart = dp(4)
    }
    background = resources.getDrawable(R.drawable.btn_bg_transparent_round6dp, theme)
    contentDescription = getString(contentDescResId)
    setImageResource(iconResId)
    imageTintList = android.content.res.ColorStateList.valueOf(attrColor(R.attr.colorTextContent))
    scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
}

// Helper: setting_wrap label for fields
private fun Context.settingWrapLabel(textResId: Int, labelFor: Int = 0) = TextView(this).apply {
    layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
    setText(textResId)
    if (labelFor != 0) setLabelFor(labelFor)
}

/**
 * Build the entire act_account_setting view tree programmatically.
 * Returns the root LinearLayout with all child views and proper IDs assigned.
 */
fun buildAccountSettingView(context: Context): View {
    val ctx = context

    val innerContent = LinearLayout(ctx).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        orientation = LinearLayout.VERTICAL
        setPadding(0, ctx.dp(12), 0, ctx.dp(128))

        // === Instance section ===
        addView(ctx.settingDivider())
        addView(ctx.settingRowLabel(R.string.instance))
        addView(ctx.settingRowForm {
            addView(TextView(ctx).apply {
                id = R.id.tvInstance
                layoutParams = horizontalStretchParams()
            })
        })

        // === User section ===
        addView(ctx.settingDivider())
        addView(ctx.settingRowLabel(R.string.user))
        addView(ctx.settingRowForm {
            addView(TextView(ctx).apply {
                id = R.id.tvUser
                layoutParams = horizontalStretchParams()
                ellipsize = android.text.TextUtils.TruncateAt.START
            })
        })

        // === Nickname section ===
        addView(ctx.settingDivider())
        addView(ctx.settingRowLabel(R.string.nickname_label))
        addView(ctx.settingRowForm {
            addView(TextView(ctx).apply {
                id = R.id.tvUserCustom
                layoutParams = horizontalStretchParams().apply {
                    gravity = Gravity.CENTER_VERTICAL
                }
                setPadding(ctx.dp(4), ctx.dp(4), ctx.dp(4), ctx.dp(4))
            })
            addView(ctx.settingImageButton(R.id.btnUserCustom, R.drawable.ic_edit, R.string.edit))
        })

        // === Default text section ===
        addView(ctx.settingDivider())
        addView(ctx.settingRowLabel(R.string.toot_default_text, labelFor = R.id.etDefaultText))
        addView(ctx.settingRowForm {
            addView(EditText(ctx).apply {
                id = R.id.etDefaultText
                layoutParams = horizontalStretchParams().apply {
                    gravity = Gravity.CENTER_VERTICAL
                }
                inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE
                setPadding(ctx.dp(4), ctx.dp(4), ctx.dp(4), ctx.dp(4))
            })
        })

        // === Public profile section ===
        addView(ctx.settingDivider())
        addView(ctx.settingRowLabel(R.string.public_profile))
        addView(ctx.settingRowForm {
            addView(FrameLayout(ctx).apply {
                layoutParams = horizontalStretchParams().apply {
                    height = ctx.dp(64)
                }
                addView(MyNetworkImageView(ctx).apply {
                    id = R.id.ivProfileHeader
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                })
                addView(MyNetworkImageView(ctx).apply {
                    id = R.id.ivProfileAvatar
                    layoutParams = FrameLayout.LayoutParams(ctx.dp(48), ctx.dp(48)).apply {
                        gravity = Gravity.CENTER
                    }
                    scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                })
            })
        })
        addView(ctx.settingRowForm {
            addView(Button(ctx).apply {
                id = R.id.btnProfileAvatar
                layoutParams = horizontalStretchParams()
                setText(R.string.change_avatar)
                isAllCaps = false
            })
        })
        addView(ctx.settingRowForm {
            addView(Button(ctx).apply {
                id = R.id.btnProfileHeader
                layoutParams = horizontalStretchParams()
                setText(R.string.change_header)
                isAllCaps = false
            })
        })

        // Display name
        addView(ctx.settingRowForm {
            addView(ctx.settingRowLabel(R.string.display_name, labelFor = R.id.etDisplayName))
        })
        addView(ctx.settingRowForm {
            addView(EditText(ctx).apply {
                id = R.id.etDisplayName
                layoutParams = horizontalStretchParams()
                inputType = InputType.TYPE_CLASS_TEXT
            })
            addView(ctx.settingImageButton(R.id.btnDisplayName, R.drawable.ic_send, R.string.post))
        })

        // Note
        addView(ctx.settingRowForm {
            addView(ctx.settingRowLabel(R.string.note, labelFor = R.id.etNote))
        })
        addView(ctx.settingRowForm {
            addView(EditText(ctx).apply {
                id = R.id.etNote
                layoutParams = horizontalStretchParams()
                inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE
            })
            addView(ctx.settingImageButton(R.id.btnNote, R.drawable.ic_send, R.string.post))
        })

        // Locked
        addView(ctx.settingRowForm {
            addView(CheckBox(ctx).apply {
                id = R.id.cbLocked
                layoutParams = horizontalStretchParams()
                setText(R.string.locked_account)
            })
        })

        // === Profile metadata fields ===
        addView(ctx.settingDivider())
        addView(LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isBaselineAligned = false
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            addView(ctx.settingRowLabelStretch(R.string.profile_metadata))
            addView(ctx.settingImageButton(R.id.btnFields, R.drawable.ic_send, R.string.post))
        })

        // Field 1-4 name/value pairs
        for (i in 1..4) {
            val nameId = when (i) {
                1 -> R.id.etFieldName1
                2 -> R.id.etFieldName2
                3 -> R.id.etFieldName3
                else -> R.id.etFieldName4
            }
            val valueId = when (i) {
                1 -> R.id.etFieldValue1
                2 -> R.id.etFieldValue2
                3 -> R.id.etFieldValue3
                else -> R.id.etFieldValue4
            }
            val nameLabelRes = when (i) {
                1 -> R.string.field_name1
                2 -> R.string.field_name2
                3 -> R.string.field_name3
                else -> R.string.field_name4
            }
            val valueLabelRes = when (i) {
                1 -> R.string.field_value1
                2 -> R.string.field_value2
                3 -> R.string.field_value3
                else -> R.string.field_value4
            }
            addView(ctx.settingRowFormFields {
                addView(ctx.settingWrapLabel(nameLabelRes, labelFor = nameId))
                addView(EditText(ctx).apply {
                    id = nameId
                    layoutParams = horizontalStretchParams()
                    inputType = InputType.TYPE_CLASS_TEXT
                })
            })
            addView(ctx.settingRowFormFields {
                addView(ctx.settingWrapLabel(valueLabelRes, labelFor = valueId))
                addView(EditText(ctx).apply {
                    id = valueId
                    layoutParams = horizontalStretchParams()
                    inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE
                })
            })
        }

        // === Actions section ===
        addView(ctx.settingDivider())
        addView(ctx.settingRowLabel(R.string.actions))
        addView(Button(ctx).apply {
            id = R.id.btnOpenBrowser
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = ctx.dp(36)
                marginEnd = 0
            }
            background = resources.getDrawable(R.drawable.btn_bg_transparent_round6dp, context.theme)
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            isAllCaps = false
            setPaddingRelative(ctx.dp(12), paddingTop, paddingEnd, paddingBottom)
            ellipsize = android.text.TextUtils.TruncateAt.START
        })
        addView(ctx.settingRowButton(R.string.update_access_token, R.id.btnAccessToken).apply {
            ellipsize = android.text.TextUtils.TruncateAt.START
        })
        addView(ctx.settingRowButton(R.string.input_access_token, R.id.btnInputAccessToken).apply {
            ellipsize = android.text.TextUtils.TruncateAt.START
        })
        addView(ctx.settingRowButton(R.string.load_preference_from_web_ui, R.id.btnLoadPreference).apply {
            ellipsize = android.text.TextUtils.TruncateAt.START
        })
        addView(ctx.settingRowButton(R.string.account_remove, R.id.btnAccountRemove).apply {
            ellipsize = android.text.TextUtils.TruncateAt.START
        })

        // === Default visibility ===
        addView(ctx.settingDivider())
        addView(ctx.settingRowLabel(R.string.default_status_visibility))
        addView(ctx.settingRowForm {
            addView(Button(ctx).apply {
                id = R.id.btnVisibility
                layoutParams = horizontalStretchParams()
            })
        })

        // === Language code ===
        addView(ctx.settingDivider())
        addView(ctx.settingRowLabel(R.string.post_language_code, labelFor = R.id.etLanguageCode))
        addView(Spinner(ctx).apply {
            id = R.id.spLanguageCode
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = ctx.dp(36)
                marginEnd = 0
            }
            minimumHeight = ctx.dp(40)
        })

        // === Mark sensitive ===
        addView(ctx.settingDivider())
        addView(ctx.settingRowLabel(R.string.mark_sensitive_by_default))
        addView(ctx.settingSwitchRow(R.id.swMarkSensitive))

        // === NSFW open ===
        addView(ctx.settingDivider())
        addView(ctx.settingRowLabel(R.string.sensitive_content_default_open))
        addView(ctx.settingSwitchRow(R.id.swNSFWOpen))

        // === CW expand ===
        addView(ctx.settingDivider())
        addView(ctx.settingRowLabel(R.string.cw_default_open))
        addView(ctx.settingSwitchRow(R.id.swExpandCW))

        // === Confirmation section ===
        addView(ctx.settingDivider())
        addView(ctx.settingRowLabel(R.string.confirmation))
        addView(ctx.settingCheckBox(R.id.cbConfirmFollow, R.string.follow))
        addView(ctx.settingCheckBox(R.id.cbConfirmFollowLockedUser, R.string.follow_locked_user))
        addView(ctx.settingCheckBox(R.id.cbConfirmUnfollow, R.string.unfollow))
        addView(ctx.settingCheckBox(R.id.cbConfirmBoost, R.string.boost))
        addView(ctx.settingCheckBox(R.id.cbConfirmUnboost, R.string.unboost))
        addView(ctx.settingCheckBox(R.id.cbConfirmFavourite, R.string.favourite))
        addView(ctx.settingCheckBox(R.id.cbConfirmUnfavourite, R.string.unfavourite))
        addView(ctx.settingCheckBox(R.id.cbConfirmUnbookmark, R.string.unbookmark))
        addView(ctx.settingCheckBox(R.id.cbConfirmToot, R.string.act_post))
        addView(ctx.settingCheckBox(R.id.cbConfirmReaction, R.string.reaction))

        // === Notifications section ===
        addView(ctx.settingDivider())
        addView(ctx.settingRowLabel(R.string.notifications))
        addView(ctx.settingCheckBox(R.id.cbNotificationMention, R.string.mention2))
        addView(ctx.settingCheckBox(R.id.cbNotificationBoost, R.string.boost))
        addView(ctx.settingCheckBox(R.id.cbNotificationFavourite, R.string.favourite))
        addView(ctx.settingCheckBox(R.id.cbNotificationFollow, R.string.follow))
        addView(ctx.settingCheckBox(R.id.cbNotificationFollowRequest, R.string.follow_request))
        addView(ctx.settingCheckBox(R.id.cbNotificationReaction, R.string.reaction))
        addView(ctx.settingCheckBox(R.id.cbNotificationVote, R.string.vote_polls))
        addView(ctx.settingCheckBox(R.id.cbNotificationPost, R.string.notification_type_post))
        addView(ctx.settingCheckBox(R.id.cbNotificationUpdate, R.string.notification_type_update))
        addView(ctx.settingCheckBox(R.id.cbNotificationStatusReference, R.string.notification_type_status_reference_fedibird))
        addView(ctx.settingCheckBox(R.id.cbNotificationSeveredRelationships, R.string.notification_type_severed_relationships))

        // === Push notification ===
        addView(ctx.settingDivider())
        addView(ctx.settingRowLabel(R.string.push_notification_use))
        addView(ctx.settingSwitchRow(R.id.swNotificationPushEnabled))

        // Notification accent color
        addView(TextView(ctx).apply {
            id = R.id.tvNotificationAccentColor
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPaddingRelative(0, 0, 0, 0)
            textSize = 14f
            setText(R.string.notification_accent_color)
        })
        addView(LinearLayout(ctx).apply {
            id = R.id.llNotificationAccentColor
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = ctx.dp(32)
                marginEnd = 0
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(Button(ctx).apply {
                id = R.id.btnNotificationAccentColorEdit
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setText(R.string.edit)
                isAllCaps = false
            })
            addView(Button(ctx).apply {
                id = R.id.btnNotificationAccentColorReset
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setText(R.string.reset)
                isAllCaps = false
            })
            addView(View(ctx).apply {
                id = R.id.vNotificationAccentColorColor
                layoutParams = LinearLayout.LayoutParams(ctx.dp(32), ctx.dp(32)).apply {
                    marginStart = ctx.dp(8)
                }
            })
        })

        // Push policy
        addView(TextView(ctx).apply {
            id = R.id.tvPushPolicyDesc
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPaddingRelative(0, 0, 0, 0)
            textSize = 14f
            setText(R.string.push_notification_filter)
        })
        addView(Spinner(ctx).apply {
            id = R.id.spPushPolicy
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = ctx.dp(36)
                marginEnd = 0
            }
            minimumHeight = ctx.dp(40)
        })

        // Push actions
        addView(TextView(ctx).apply {
            id = R.id.tvPushActions
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPaddingRelative(0, 0, 0, 0)
            textSize = 14f
            setText(R.string.actions)
        })
        addView(ctx.settingRowButton(R.string.update_push_subscription, R.id.btnPushSubscription).apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = ctx.dp(12)
            ellipsize = android.text.TextUtils.TruncateAt.START
        })
        addView(ctx.settingRowButton(R.string.update_push_subscription_not_force, R.id.btnPushSubscriptionNotForce).apply {
            ellipsize = android.text.TextUtils.TruncateAt.START
            visibility = View.GONE
        })

        // === Pull notification ===
        addView(ctx.settingDivider())
        addView(ctx.settingRowLabel(R.string.pull_notification_use))
        addView(ctx.settingSwitchRow(R.id.swNotificationPullEnabled))

        // Dont show timeout
        addView(TextView(ctx).apply {
            id = R.id.tvDontShowTimeout
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPaddingRelative(0, 0, 0, 0)
            textSize = 14f
            setText(R.string.dont_show_timeout)
        })
        addView(ctx.settingSwitchRow(R.id.swDontShowTimeout))

        // Pull actions
        addView(TextView(ctx).apply {
            id = R.id.tvPullActions
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPaddingRelative(0, 0, 0, 0)
            textSize = 14f
            setText(R.string.actions)
        })
        addView(ctx.settingRowButton(R.string.reset_notification_tracking_status, R.id.btnResetNotificationTracking).apply {
            ellipsize = android.text.TextUtils.TruncateAt.START
        })

        // === Max toot chars ===
        addView(ctx.settingDivider())
        addView(ctx.settingRowLabel(R.string.max_toot_chars, labelFor = R.id.etMaxTootChars))
        addView(EditText(ctx).apply {
            id = R.id.etMaxTootChars
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            gravity = Gravity.CENTER
            inputType = InputType.TYPE_CLASS_NUMBER
        })

        // === Media size max ===
        addView(ctx.settingDivider())
        addView(ctx.settingRowLabel(R.string.media_attachment_max_byte_size, labelFor = R.id.etMediaSizeMax))
        addView(EditText(ctx).apply {
            id = R.id.etMediaSizeMax
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            gravity = Gravity.CENTER
            inputType = InputType.TYPE_CLASS_NUMBER
        })

        // === Resize image ===
        addView(ctx.settingDivider())
        addView(ctx.settingRowLabel(R.string.resize_image))
        addView(Spinner(ctx).apply {
            id = R.id.spResizeImage
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = ctx.dp(36)
                marginEnd = 0
            }
        })

        // === Movie size max ===
        addView(ctx.settingDivider())
        addView(ctx.settingRowLabel(R.string.media_attachment_max_byte_size_movie, labelFor = R.id.etMovieSizeMax))
        addView(ctx.settingRowLabel(R.string.option_deprecated_mastodon342))
        addView(EditText(ctx).apply {
            id = R.id.etMovieSizeMax
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            gravity = Gravity.CENTER
            inputType = InputType.TYPE_CLASS_NUMBER
        })

        // === Movie transcode ===
        addView(ctx.settingDivider())
        addView(ctx.settingRowLabel(R.string.movie_transcode))

        // Transcode mode
        addView(ctx.settingRowLabelIndent1(R.string.movie_transcode_mode))
        addView(ctx.settingSpinnerIndent1(R.id.spMovieTranscodeMode))

        // Bitrate
        addView(ctx.settingRowLabelIndent1(R.string.movie_transcode_max_bitrate))
        addView(ctx.settingEditTextIndent1(R.id.etMovieBitrate, InputType.TYPE_CLASS_NUMBER))

        // Frame rate
        addView(ctx.settingRowLabelIndent1(R.string.movie_transcode_max_frame_rate))
        addView(ctx.settingEditTextIndent1(R.id.etMovieFrameRate, InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_CLASS_NUMBER))

        // Square pixels
        addView(ctx.settingRowLabelIndent1(R.string.movie_transcode_max_square_pixels))
        addView(ctx.settingEditTextIndent1(R.id.etMovieSquarePixels, InputType.TYPE_CLASS_NUMBER))
    }

    val svContent = ScrollView(ctx).apply {
        id = R.id.svContent
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0, 1f
        )
        setBackgroundColor(ctx.attrColor(R.attr.colorMainBackground))
        isFillViewport = true
        isVerticalFadingEdgeEnabled = true
        setFadingEdgeLength(ctx.dp(20))
        scrollBarStyle = View.SCROLLBARS_OUTSIDE_OVERLAY
        addView(innerContent)
    }

    val tv = android.util.TypedValue()
    ctx.theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)
    val actionBarHeight = ctx.resources.getDimensionPixelSize(tv.resourceId)

    val toolbar = Toolbar(ctx).apply {
        id = R.id.toolbar
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            actionBarHeight
        )
        background = ctx.resources.getDrawable(R.drawable.action_bar_bg, ctx.theme)
        elevation = ctx.dp(4).toFloat()
    }

    return LinearLayout(ctx).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        orientation = LinearLayout.VERTICAL
        addView(toolbar)
        addView(svContent)
    }
}
