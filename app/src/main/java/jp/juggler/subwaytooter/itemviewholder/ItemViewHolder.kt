package jp.juggler.subwaytooter.itemviewholder

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.BackgroundColorSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import com.google.android.flexbox.JustifyContent
import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.api.entity.TimelineItem
import jp.juggler.subwaytooter.api.entity.TootAccountRef
import jp.juggler.subwaytooter.api.entity.TootStatus
import jp.juggler.subwaytooter.column.Column
import jp.juggler.subwaytooter.columnviewholder.ItemListAdapter
import jp.juggler.subwaytooter.drawable.PreviewCardBorder
import jp.juggler.subwaytooter.pref.PrefB
import jp.juggler.subwaytooter.pref.PrefI
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.subwaytooter.util.NetworkEmojiInvalidator
import jp.juggler.subwaytooter.util.endMargin
import jp.juggler.subwaytooter.util.minHeightCompat
import jp.juggler.subwaytooter.util.setPaddingStartEnd
import jp.juggler.subwaytooter.util.startMargin
import jp.juggler.subwaytooter.util.startPadding
import jp.juggler.subwaytooter.view.BlurhashView
import jp.juggler.subwaytooter.view.MyLinkMovementMethod
import jp.juggler.subwaytooter.view.MyNetworkImageView
import jp.juggler.subwaytooter.view.MyTextView
import jp.juggler.subwaytooter.view.TagHistoryView
import jp.juggler.util.log.Benchmark
import jp.juggler.util.log.LogCategory
import jp.juggler.util.ui.applyAlphaMultiplier
import jp.juggler.util.ui.attrColor
import jp.juggler.util.ui.dp
import jp.juggler.util.ui.resDrawable

class ItemViewHolder(
    val activity: ActMain,
) : View.OnClickListener, View.OnLongClickListener {

    companion object {
        const val MEDIA_VIEW_COUNT = 4

        val log = LogCategory("ItemViewHolder")
        var toot_color_unlisted: Int = 0
        var toot_color_follower: Int = 0
        var toot_color_direct_user: Int = 0
        var toot_color_direct_me: Int = 0
    }

    val viewRoot: View

    var bSimpleList: Boolean = false

    lateinit var column: Column

    internal lateinit var listAdapter: ItemListAdapter

    private val inflateBench = Benchmark(log, "Item-Inflate", 40L)
    val bindBenchmark = Benchmark(log, "Item-bind", 40L)

    lateinit var llBoosted: View
    lateinit var ivBoostAvatar: MyNetworkImageView
    lateinit var ivBoosted: ImageView
    lateinit var tvBoosted: MyTextView
    lateinit var tvBoostedAcct: MyTextView
    lateinit var tvBoostedTime: MyTextView

    lateinit var llReply: View
    lateinit var ivReplyAvatar: MyNetworkImageView
    lateinit var ivReply: ImageView
    lateinit var tvReply: MyTextView

    lateinit var llFollow: View
    lateinit var ivFollow: MyNetworkImageView
    lateinit var tvFollowerName: MyTextView
    lateinit var tvFollowerAcct: MyTextView
    lateinit var btnFollow: ImageButton
    lateinit var ivFollowedBy: ImageView

    lateinit var llStatus: View
    lateinit var ivAvatar: MyNetworkImageView
    lateinit var tvName: MyTextView
    lateinit var tvTime: MyTextView
    lateinit var tvAcct: MyTextView

    lateinit var llContentWarning: View
    lateinit var tvContentWarning: MyTextView
    lateinit var btnContentWarning: AppCompatImageButton

    lateinit var llContents: View
    lateinit var tvMentions: MyTextView
    internal lateinit var tvContent: MyTextView

    lateinit var flMedia: View
    lateinit var llMedia: View
    lateinit var btnShowMedia: BlurhashView
    lateinit var btnHideMedia: ImageButton
    lateinit var tvMediaCount: MyTextView
    val tvMediaDescriptions = ArrayList<AppCompatButton>()
    val ivMediaThumbnails = ArrayList<MyNetworkImageView>()

    lateinit var statusButtonsViewHolder: StatusButtonsViewHolder
    lateinit var llButtonBar: View

    lateinit var llSearchTag: View
    lateinit var btnSearchTag: AppCompatButton
    lateinit var btnGapHead: ImageButton
    lateinit var btnGapTail: ImageButton
    lateinit var llTrendTag: View
    lateinit var tvTrendTagName: MyTextView
    lateinit var tvTrendTagDesc: MyTextView
    lateinit var tvTrendTagCount: MyTextView
    lateinit var cvTagHistory: TagHistoryView

    lateinit var llList: View
    lateinit var btnListTL: AppCompatButton
    lateinit var btnListMore: ImageButton

    lateinit var llFollowRequest: View
    lateinit var btnFollowRequestAccept: ImageButton
    lateinit var btnFollowRequestDeny: ImageButton

    lateinit var llFilter: View
    lateinit var tvFilterPhrase: MyTextView
    lateinit var tvFilterDetail: MyTextView

    lateinit var llCardOuter: View
    lateinit var tvCardText: MyTextView
    lateinit var flCardImage: View
    lateinit var llCardImage: View
    lateinit var ivCardImage: MyNetworkImageView
    lateinit var btnCardImageHide: ImageButton
    lateinit var btnCardImageShow: BlurhashView

    lateinit var llExtra: LinearLayout

    lateinit var llConversationIcons: View
    lateinit var ivConversationIcon1: MyNetworkImageView
    lateinit var ivConversationIcon2: MyNetworkImageView
    lateinit var ivConversationIcon3: MyNetworkImageView
    lateinit var ivConversationIcon4: MyNetworkImageView
    lateinit var tvConversationIconsMore: MyTextView
    lateinit var tvConversationParticipants: MyTextView

    lateinit var tvApplication: MyTextView

    lateinit var tvMessageHolder: MyTextView

    lateinit var llOpenSticker: View
    lateinit var ivOpenSticker: MyNetworkImageView
    lateinit var tvOpenSticker: MyTextView

    @Suppress("MemberVisibilityCanBePrivate")
    lateinit var tvLastStatusAt: MyTextView

    lateinit var accessInfo: SavedAccount

    var buttonsForStatus: StatusButtons? = null

    var item: TimelineItem? = null

    var statusShowing: TootStatus? = null
    var statusReply: TootStatus? = null
    var statusAccount: TootAccountRef? = null
    var boostAccount: TootAccountRef? = null
    var followAccount: TootAccountRef? = null

    var boostTime = 0L

    var colorTextContent: Int = 0
    var acctColor = 0
    var contentColorCsl = ColorStateList.valueOf(0)

    val boostInvalidator: NetworkEmojiInvalidator
    val replyInvalidator: NetworkEmojiInvalidator
    val followInvalidator: NetworkEmojiInvalidator
    val nameInvalidator: NetworkEmojiInvalidator
    val contentInvalidator: NetworkEmojiInvalidator
    val spoilerInvalidator: NetworkEmojiInvalidator
    val lastActiveInvalidator: NetworkEmojiInvalidator
    val extraInvalidatorList = ArrayList<NetworkEmojiInvalidator>()

    var boostedAction: ItemViewHolder.() -> Unit = defaultBoostedAction

    init {
        this.viewRoot = inflate(activity)

        for (v in arrayOf(
            btnCardImageHide,
            btnCardImageShow,
            btnContentWarning,
            btnFollow,
            btnFollow,
            btnFollowRequestAccept,
            btnFollowRequestDeny,
            btnGapHead,
            btnGapTail,
            btnHideMedia,
            btnListMore,
            btnListTL,
            btnSearchTag,
            btnShowMedia,
            ivAvatar,
            ivCardImage,
            llBoosted,
            llFilter,
            llFollow,
            llReply,
            llTrendTag,
        )) {
            v.setOnClickListener(this)
        }
        ivMediaThumbnails.forEach { it.setOnClickListener(this) }
        tvMediaDescriptions.forEach {
            it.isClickable = true
            it.setOnClickListener(this)
        }

        for (v in arrayOf(
            btnSearchTag,
            btnFollow,
            ivCardImage,
            llBoosted,
            llReply,
            llFollow,
            llConversationIcons,
            ivAvatar,
            llTrendTag
        )) {
            v.setOnLongClickListener(this)
        }

        // リンク処理用のMyLinkMovementMethod
        for (v in arrayOf(
            tvContent,
            tvMentions,
            tvContentWarning,
            tvCardText,
            tvMessageHolder,
        )) {
            v.movementMethod = MyLinkMovementMethod
        }

        ActMain.timelineFontSizeSp.takeIf { it.isFinite() }?.let { f ->
            tvFollowerName.textSize = f
            tvName.textSize = f
            tvMentions.textSize = f
            tvContentWarning.textSize = f
            tvContent.textSize = f
            btnShowMedia.textSize = f
            btnCardImageShow.textSize = f
            tvApplication.textSize = f
            tvMessageHolder.textSize = f
            btnListTL.textSize = f
            tvTrendTagName.textSize = f
            tvTrendTagCount.textSize = f
            tvFilterPhrase.textSize = f
            tvCardText.textSize = f
            tvConversationIconsMore.textSize = f
            tvConversationParticipants.textSize = f

            tvMediaDescriptions.forEach { it.textSize = f }
        }

        var f: Float

        f = activity.notificationTlFontSizeSp
        if (!f.isNaN()) {
            tvBoosted.textSize = f
            tvReply.textSize = f
        }

        f = activity.acctFontSizeSp
        if (!f.isNaN()) {
            tvBoostedAcct.textSize = f
            tvBoostedTime.textSize = f
            tvFollowerAcct.textSize = f
            tvLastStatusAt.textSize = f
            tvAcct.textSize = f
            tvTime.textSize = f
            tvTrendTagDesc.textSize = f
            tvFilterDetail.textSize = f
        }

        ActMain.timelineSpacing?.let { spacing ->
            tvFollowerName.setLineSpacing(0f, spacing)
            tvName.setLineSpacing(0f, spacing)
            tvMentions.setLineSpacing(0f, spacing)
            tvContentWarning.setLineSpacing(0f, spacing)
            tvContent.setLineSpacing(0f, spacing)
            btnShowMedia.setLineSpacing(0f, spacing)
            btnCardImageShow.setLineSpacing(0f, spacing)
            tvApplication.setLineSpacing(0f, spacing)
            tvMessageHolder.setLineSpacing(0f, spacing)
            btnListTL.setLineSpacing(0f, spacing)
            tvTrendTagName.setLineSpacing(0f, spacing)
            tvTrendTagCount.setLineSpacing(0f, spacing)
            tvFilterPhrase.setLineSpacing(0f, spacing)
            tvMediaDescriptions.forEach { it.setLineSpacing(0f, spacing) }
            tvCardText.setLineSpacing(0f, spacing)
            tvConversationIconsMore.setLineSpacing(0f, spacing)
            tvConversationParticipants.setLineSpacing(0f, spacing)
            tvBoosted.setLineSpacing(0f, spacing)
            tvReply.setLineSpacing(0f, spacing)
            tvLastStatusAt.setLineSpacing(0f, spacing)
        }

        var s = activity.avatarIconSize
        ivAvatar.layoutParams.height = s
        ivAvatar.layoutParams.width = s
        ivFollow.layoutParams.width = s

        s = ActMain.replyIconSize
        ivReply.layoutParams.width = s
        ivReply.layoutParams.height = s
        ivReplyAvatar.layoutParams.width = s
        ivReplyAvatar.layoutParams.height = s

        s = activity.notificationTlIconSize
        ivBoosted.layoutParams.width = s
        ivBoosted.layoutParams.height = s
        ivBoostAvatar.layoutParams.width = s
        ivBoostAvatar.layoutParams.height = s

        this.contentInvalidator = NetworkEmojiInvalidator(activity.handler, tvContent)
        this.spoilerInvalidator = NetworkEmojiInvalidator(activity.handler, tvContentWarning)
        this.boostInvalidator = NetworkEmojiInvalidator(activity.handler, tvBoosted)
        this.replyInvalidator = NetworkEmojiInvalidator(activity.handler, tvReply)
        this.followInvalidator = NetworkEmojiInvalidator(activity.handler, tvFollowerName)
        this.nameInvalidator = NetworkEmojiInvalidator(activity.handler, tvName)
        this.lastActiveInvalidator = NetworkEmojiInvalidator(activity.handler, tvLastStatusAt)

        val cardBackground = llCardOuter.background
        if (cardBackground is PreviewCardBorder) {
            val density = activity.density
            cardBackground.round = (density * 8f)
            cardBackground.width = (density * 1f)
        }

        val textShowMedia = SpannableString(activity.getString(R.string.tap_to_show))
            .apply {
                val colorBg = activity.attrColor(R.attr.colorShowMediaBackground)
                    .applyAlphaMultiplier(0.5f)
                setSpan(
                    BackgroundColorSpan(colorBg),
                    0,
                    this.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

        btnShowMedia.text = textShowMedia
        btnCardImageShow.text = textShowMedia
    }

    override fun onClick(v: View?) = onClickImpl(v)
    override fun onLongClick(v: View?): Boolean = onLongClickImpl(v)

    fun onViewRecycled() {
    }

    fun getAccount() = statusAccount ?: boostAccount ?: followAccount

    /////////////////////////////////////////////////////////////////////

    private fun LinearLayout.inflateBoosted() {
        llBoosted = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.btn_bg_transparent_round6dp)
            gravity = Gravity.CENTER_VERTICAL

            ivBoosted = ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }.also {
                addView(it, LinearLayout.LayoutParams(context.dp(32), context.dp(32)))
            }

            ivBoostAvatar = MyNetworkImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }.also {
                addView(it, LinearLayout.LayoutParams(context.dp(32), context.dp(32)))
            }

            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL

                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL

                    tvBoostedAcct = MyTextView(context).apply {
                        ellipsize = TextUtils.TruncateAt.END
                        gravity = Gravity.END
                        maxLines = 1
                        textSize = 12f // textSize の単位はSP
                    }.also {
                        addView(it, LinearLayout.LayoutParams(context.dp(0), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                            weight = 1f
                        })
                    }

                    tvBoostedTime = MyTextView(context).apply {
                        startPadding = context.dp(2)
                        gravity = Gravity.END
                        textSize = 12f // textSize の単位はSP
                    }.also {
                        addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                    }
                }.also {
                    addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                }

                tvBoosted = MyTextView(context).apply {
                }.also {
                    addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        gravity = Gravity.CENTER_VERTICAL
                    })
                }
            }.also {
                addView(it, LinearLayout.LayoutParams(context.dp(0), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    weight = 1f
                    startMargin = context.dp(4)
                })
            }
        }.also {
            addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = context.dp(6)
            })
        }
    }

    private fun LinearLayout.inflateFollowed() {
        llFollow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            background = resDrawable(R.drawable.btn_bg_transparent_round6dp)
            gravity = Gravity.CENTER_VERTICAL

            ivFollow = MyNetworkImageView(context).apply {
                contentDescription = context.getString(R.string.thumbnail)
                scaleType = ImageView.ScaleType.FIT_END
            }.also {
                addView(it, LinearLayout.LayoutParams(context.dp(48), context.dp(40)).apply {
                    endMargin = context.dp(4)
                })
            }

            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL

                tvFollowerName = MyTextView(context).apply {
                }.also {
                    addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                }

                tvFollowerAcct = MyTextView(context).apply {
                    setPaddingStartEnd(context.dp(4), context.dp(4))
                    textSize = 12f // SP
                }.also {
                    addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                }

                tvLastStatusAt = MyTextView(context).apply {
                    setPaddingStartEnd(context.dp(4), context.dp(4))
                    textSize = 12f // SP
                }.also {
                    addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                }
            }.also {
                addView(it, LinearLayout.LayoutParams(context.dp(0), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    weight = 1f
                })
            }

            FrameLayout(context).apply {
                btnFollow = ImageButton(context).apply {
                    background =
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.btn_bg_transparent_round6dp
                        )
                    contentDescription = context.getString(R.string.follow)
                    scaleType = ImageView.ScaleType.CENTER
                }.also {
                    addView(it, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                }

                ivFollowedBy = ImageView(context).apply {
                    scaleType = ImageView.ScaleType.CENTER
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }.also {
                    addView(it, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                }
            }.also {
                addView(it, LinearLayout.LayoutParams(context.dp(40), context.dp(40)).apply {
                    startMargin = context.dp(4)
                })
            }
        }.also {
            addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    private fun LinearLayout.inflateVerticalMedia(thumbnailHeight: Int) =
        FrameLayout(context).apply {
            llMedia = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL

                btnHideMedia = ImageButton(context).apply {
                    background = resDrawable(R.drawable.btn_bg_transparent_round6dp)
                    contentDescription = context.getString(R.string.hide)
                    setImageResource(R.drawable.ic_close)
                }.also {
                    addView(it, LinearLayout.LayoutParams(context.dp(32), context.dp(32)).apply {
                        gravity = Gravity.END
                    })
                }
                ivMediaThumbnails.clear()
                repeat(MEDIA_VIEW_COUNT) {
                    MyNetworkImageView(context).apply {
                        background = resDrawable(R.drawable.bg_thumbnail)
                        contentDescription = context.getString(R.string.thumbnail)
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }.also { iv ->
                        addView(iv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, thumbnailHeight).apply {
                            topMargin = context.dp(3)
                        })
                        ivMediaThumbnails.add(iv)
                    }
                }
            }.also {
                addView(it, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            }

            btnShowMedia = BlurhashView(context).apply {
                errorColor = context.attrColor(R.attr.colorShowMediaBackground)
                gravity = Gravity.CENTER
                setTextColor(context.attrColor(R.attr.colorShowMediaText))
                minHeightCompat = context.dp(48)
            }.also {
                addView(it, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, thumbnailHeight))
            }
        }.also {
            addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dp(3)
            })
        }

    private fun LinearLayout.inflateHorizontalMedia(thumbnailHeight: Int) =
        FrameLayout(context).apply {
            llMedia = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                ivMediaThumbnails.clear()

                repeat(MEDIA_VIEW_COUNT) { idx ->
                    MyNetworkImageView(context).apply {
                        background = resDrawable(R.drawable.bg_thumbnail)
                        contentDescription = context.getString(R.string.thumbnail)
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }.also { iv ->
                        addView(iv, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                            weight = 1f
                            if (idx > 0) startMargin = context.dp(8)
                        })
                        ivMediaThumbnails.add(iv)
                    }
                }

                btnHideMedia = ImageButton(context).apply {
                    background = ContextCompat.getDrawable(
                        context,
                        R.drawable.btn_bg_transparent_round6dp
                    )
                    contentDescription = context.getString(R.string.hide)
                    setImageResource(R.drawable.ic_close)
                }.also {
                    addView(it, LinearLayout.LayoutParams(context.dp(32), ViewGroup.LayoutParams.MATCH_PARENT).apply {
                        startMargin = context.dp(8)
                    })
                }
            }.also {
                addView(it, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            }

            btnShowMedia = BlurhashView(context).apply {
                errorColor = context.attrColor(R.attr.colorShowMediaBackground)
                setTextColor(context.attrColor(R.attr.colorShowMediaText))
                gravity = Gravity.CENTER
            }.also {
                addView(it, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            }
        }.also {
            addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, thumbnailHeight).apply {
                topMargin = context.dp(3)
            })
        }

    private fun LinearLayout.inflateCard(actMain: ActMain) {
        llCardOuter = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val p = context.dp(3)
            setPadding(p, p, p, p)
            setPadding(paddingLeft, paddingTop, paddingRight, context.dp(6))

            background = PreviewCardBorder()

            tvCardText = MyTextView(context).apply {
            }.also {
                addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }

            flCardImage = FrameLayout(context).apply {

                llCardImage = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL

                    ivCardImage = MyNetworkImageView(context).apply {
                        contentDescription = context.getString(R.string.thumbnail)
                        scaleType = when {
                            PrefB.bpDontCropMediaThumb.value -> ImageView.ScaleType.FIT_CENTER
                            else -> ImageView.ScaleType.CENTER_CROP
                        }
                    }.also {
                        addView(it, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                            weight = 1f
                        })
                    }
                    btnCardImageHide = ImageButton(context).apply {
                        background =
                            ContextCompat.getDrawable(
                                context,
                                R.drawable.btn_bg_transparent_round6dp
                            )
                        contentDescription = context.getString(R.string.hide)
                        setImageResource(R.drawable.ic_close)
                    }.also {
                        addView(it, LinearLayout.LayoutParams(context.dp(32), ViewGroup.LayoutParams.MATCH_PARENT).apply {
                            startMargin = context.dp(4)
                        })
                    }
                }.also {
                    addView(it, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                }

                btnCardImageShow = BlurhashView(context).apply {
                    errorColor = context.attrColor(R.attr.colorShowMediaBackground)
                    setTextColor(context.attrColor(R.attr.colorShowMediaText))
                    gravity = Gravity.CENTER
                }.also {
                    addView(it, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                }
            }.also {
                addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, actMain.appState.mediaThumbHeight).apply {
                    topMargin = context.dp(3)
                })
            }
        }.also {
            addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dp(3)
                startMargin = context.dp(12)
                endMargin = context.dp(6)
            })
        }
    }

    private fun LinearLayout.inflateStatusReplyInfo() {
        llReply = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            minimumHeight = context.dp(40)
            val p = context.dp(4)
            setPadding(p, p, p, p)
            background = resDrawable(R.drawable.btn_bg_transparent_round6dp)
            gravity = Gravity.CENTER_VERTICAL
            ivReply = ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }.also {
                addView(it, LinearLayout.LayoutParams(context.dp(32), context.dp(32)))
            }

            ivReplyAvatar = MyNetworkImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }.also {
                addView(it, LinearLayout.LayoutParams(context.dp(32), context.dp(32)).apply {
                    startMargin = context.dp(2)
                })
            }

            tvReply = MyTextView(context).apply {
            }.also {
                addView(it, LinearLayout.LayoutParams(context.dp(0), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    startMargin = context.dp(4)
                    weight = 1f
                })
            }
        }.also {
            addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    private fun LinearLayout.inflateStatusContentWarning() {
        llContentWarning = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isBaselineAligned = false

            btnContentWarning = AppCompatImageButton(context).apply {
                background = resDrawable(R.drawable.bg_button_cw)
                contentDescription = context.getString(R.string.show)
                setImageResource(R.drawable.ic_eye)
                imageTintList = ColorStateList.valueOf(context.attrColor(R.attr.colorTextContent))
            }.also {
                addView(it, LinearLayout.LayoutParams(context.dp(40), context.dp(40)).apply {
                    endMargin = context.dp(8)
                })
            }

            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL

                tvMentions = MyTextView(context).apply {}.also {
                    addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                }

                tvContentWarning = MyTextView(context).apply {
                }.also {
                    addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        topMargin = context.dp(3)
                    })
                }
            }.also {
                addView(it, LinearLayout.LayoutParams(context.dp(0), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    weight = 1f
                })
            }
        }.also {
            addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dp(3)
            })
        }
    }

    private fun LinearLayout.inflateStatusContents(actMain: ActMain) {
        llContents = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL

            tvContent = MyTextView(context).apply {
                setLineSpacing(lineSpacingExtra, 1.1f)
            }.also {
                addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = context.dp(3)
                })
            }

            val thumbnailHeight = actMain.appState.mediaThumbHeight
            flMedia = when (PrefB.bpVerticalArrangeThumbnails.value) {
                true -> inflateVerticalMedia(thumbnailHeight)
                else -> inflateHorizontalMedia(thumbnailHeight)
            }

            tvMediaCount = MyTextView(context).apply {
                gravity = Gravity.END
                includeFontPadding = false
            }.also {
                addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = context.dp(3)
                    bottomMargin = context.dp(3)
                })
            }

            tvMediaDescriptions.clear()
            repeat(MEDIA_VIEW_COUNT) {
                tvMediaDescriptions.add(
                    AppCompatButton(context).apply {
                        gravity = Gravity.START or Gravity.CENTER_VERTICAL
                        isAllCaps = false
                        background =
                            ContextCompat.getDrawable(
                                context,
                                R.drawable.btn_bg_transparent_round6dp
                            )
                        minHeightCompat = context.dp(48)
                        val p = context.dp(4)
                        setPadding(p, p, p, p)
                    }.also {
                        addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                    }
                )
            }

            inflateCard(actMain)

            llExtra = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }.also {
                addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = context.dp(0)
                })
            }
        }.also {
            addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    private fun LinearLayout.inflateStatusButtons(actMain: ActMain) {
        // compatButton bar
        statusButtonsViewHolder = StatusButtonsViewHolder(
            actMain,
            ViewGroup.LayoutParams.MATCH_PARENT,
            3f,
            justifyContent = when (PrefI.ipBoostButtonJustify.value) {
                0 -> JustifyContent.FLEX_START
                1 -> JustifyContent.CENTER
                else -> JustifyContent.FLEX_END
            }
        )
        llButtonBar = statusButtonsViewHolder.viewRoot
        addView(llButtonBar)
    }

    private fun LinearLayout.inflateOpenSticker() {

        llOpenSticker = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            isBaselineAligned = false

            ivOpenSticker = MyNetworkImageView(context).apply {
            }.also {
                addView(it, LinearLayout.LayoutParams(context.dp(16), context.dp(16)))
            }

            tvOpenSticker = MyTextView(context).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10f)
                gravity = Gravity.CENTER_VERTICAL
                setPaddingStartEnd(context.dp(4f), context.dp(4f))
            }.also {
                addView(it, LinearLayout.LayoutParams(0, context.dp(16)).apply {
                    weight = 1f
                })
            }
        }.also {
            addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    private fun LinearLayout.inflateStatusAcctTime() {
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            tvAcct = MyTextView(context).apply {
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.END
                maxLines = 1
                textSize = 12f // SP
            }.also {
                addView(it, LinearLayout.LayoutParams(context.dp(0), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    weight = 1f
                })
            }

            tvTime = MyTextView(context).apply {
                gravity = Gravity.END
                startPadding = context.dp(2)
                textSize = 12f // SP
            }.also {
                addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
        }.also {
            addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    private fun LinearLayout.inflateStatusAvatar() {
        ivAvatar = MyNetworkImageView(context).apply {
            background = resDrawable(R.drawable.btn_bg_transparent_round6dp)
            contentDescription = context.getString(R.string.thumbnail)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }.also {
            addView(it, LinearLayout.LayoutParams(context.dp(48), context.dp(48)).apply {
                topMargin = context.dp(4)
                endMargin = context.dp(4)
            })
        }
    }

    private fun LinearLayout.inflateStatus(actMain: ActMain) {
        llStatus = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL

            inflateStatusAcctTime()

            // horizontal split : avatar and other
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                inflateStatusAvatar()
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL

                    tvName = MyTextView(context).apply {}
                        .also {
                            addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                        }

                    inflateOpenSticker()
                    inflateStatusReplyInfo()
                    inflateStatusContentWarning()
                    inflateStatusContents(actMain)
                    inflateStatusButtons(actMain)

                    tvApplication = MyTextView(context).apply {
                        gravity = Gravity.END
                    }.also {
                        addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                    }
                }.also {
                    addView(it, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply { weight = 1f })
                }
            }.also {
                addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
        }.also {
            addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    private fun LinearLayout.inflateConversationIconOne() =
        MyNetworkImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
        }.also {
            addView(it, LinearLayout.LayoutParams(context.dp(24), context.dp(24)).apply {
                endMargin = context.dp(3)
            })
        }

    private fun LinearLayout.inflateConversationIcons() {
        llConversationIcons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL

            isBaselineAligned = false
            gravity = Gravity.START or Gravity.CENTER_VERTICAL

            tvConversationParticipants = MyTextView(context).apply {
                text = context.getString(R.string.participants)
            }.also {
                addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    endMargin = context.dp(3)
                })
            }

            ivConversationIcon1 = inflateConversationIconOne()
            ivConversationIcon2 = inflateConversationIconOne()
            ivConversationIcon3 = inflateConversationIconOne()
            ivConversationIcon4 = inflateConversationIconOne()

            tvConversationIconsMore = MyTextView(context).apply {}.also {
                addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
        }.also {
            addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dp(40)))
        }
    }

    private fun LinearLayout.inflateSearchTag() {
        llSearchTag = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            isBaselineAligned = false
            gravity = Gravity.CENTER_VERTICAL or Gravity.START

            btnSearchTag = AppCompatButton(context).apply {
                background =
                    resDrawable(R.drawable.btn_bg_transparent_round6dp)
                isAllCaps = false
            }.also {
                addView(it, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    weight = 1f
                })
            }

            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                btnGapHead = ImageButton(context).apply {
                    background = ContextCompat.getDrawable(
                        context,
                        R.drawable.btn_bg_transparent_round6dp
                    )
                    contentDescription = context.getString(R.string.read_gap_head)
                    setImageResource(R.drawable.ic_arrow_drop_down)
                }.also {
                    addView(it, LinearLayout.LayoutParams(context.dp(36), context.dp(36)))
                }
                btnGapTail = ImageButton(context).apply {
                    background = ContextCompat.getDrawable(
                        context,
                        R.drawable.btn_bg_transparent_round6dp
                    )
                    contentDescription = context.getString(R.string.read_gap_tail)
                    setImageResource(R.drawable.ic_arrow_drop_up)
                }.also {
                    addView(it, LinearLayout.LayoutParams(context.dp(36), context.dp(36)))
                }
            }.also {
                addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    startMargin = context.dp(8)
                    topMargin = context.dp(3)
                })
            }
        }.also {
            addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    private fun LinearLayout.inflateTrendTag() {
        llTrendTag = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL

            gravity = Gravity.CENTER_VERTICAL
            background = resDrawable(R.drawable.btn_bg_transparent_round6dp)

            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL

                tvTrendTagName = MyTextView(context).apply {}.also {
                    addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                }

                tvTrendTagDesc = MyTextView(context).apply {
                    textSize = 12f // SP
                }.also {
                    addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                }
            }.also {
                addView(it, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    weight = 1f
                })
            }
            tvTrendTagCount = MyTextView(context).apply {}.also {
                addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    startMargin = context.dp(6)
                    endMargin = context.dp(6)
                })
            }

            cvTagHistory = TagHistoryView(context).apply {}.also {
                addView(it, LinearLayout.LayoutParams(context.dp(64), context.dp(32)))
            }
        }.also {
            addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    private fun LinearLayout.inflateList() {
        llList = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL

            gravity = Gravity.CENTER_VERTICAL
            isBaselineAligned = false
            minimumHeight = context.dp(40)

            btnListTL = AppCompatButton(context).apply {
                background = resDrawable(R.drawable.btn_bg_transparent_round6dp)
                isAllCaps = false
            }.also {
                addView(it, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    weight = 1f
                })
            }

            btnListMore = ImageButton(context).apply {
                background = resDrawable(R.drawable.btn_bg_transparent_round6dp)
                setImageResource(R.drawable.ic_more)
                contentDescription = context.getString(R.string.more)
            }.also {
                addView(it, LinearLayout.LayoutParams(context.dp(40), ViewGroup.LayoutParams.MATCH_PARENT).apply {
                    startMargin = context.dp(4)
                })
            }
        }.also {
            addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    private fun LinearLayout.inflateMessageHolder() {
        tvMessageHolder = MyTextView(context).apply {
            val p = context.dp(4)
            setPadding(p, p, p, p)
            compoundDrawablePadding = context.dp(4)
        }.also {
            addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    private fun LinearLayout.inflateFollowRequest() {
        llFollowRequest = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END

            btnFollowRequestAccept = ImageButton(context).apply {
                background = resDrawable(R.drawable.btn_bg_transparent_round6dp)
                contentDescription = context.getString(R.string.follow_accept)
                setImageResource(R.drawable.ic_check)
                setPadding(0, 0, 0, 0)
            }.also {
                addView(it, LinearLayout.LayoutParams(context.dp(48f), context.dp(32f)))
            }

            btnFollowRequestDeny = ImageButton(context).apply {
                background = resDrawable(R.drawable.btn_bg_transparent_round6dp)
                contentDescription = context.getString(R.string.follow_deny)
                setImageResource(R.drawable.ic_close)
                setPadding(0, 0, 0, 0)
            }.also {
                addView(it, LinearLayout.LayoutParams(context.dp(48f), context.dp(32f)).apply {
                    startMargin = context.dp(4)
                })
            }
        }.also {
            addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dp(6)
            })
        }
    }

    private fun LinearLayout.inflateFilter() {
        llFilter = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            minimumHeight = context.dp(40)

            tvFilterPhrase = MyTextView(context).apply {
                typeface = Typeface.DEFAULT_BOLD
            }.also {
                addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }

            tvFilterDetail = MyTextView(context).apply {
                textSize = 12f // SP
            }.also {
                addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
        }.also {
            addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    fun inflate(actMain: ActMain): View {
        return inflateBench.bench {
            LinearLayout(actMain).apply {
                orientation = LinearLayout.VERTICAL
                // トップレベルのViewGroupのlparamsはイニシャライザ内部に置く
                layoutParams = androidx.recyclerview.widget.RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    marginStart = context.dp(8)
                    marginEnd = context.dp(8)
                    topMargin = context.dp(2f)
                    bottomMargin = context.dp(1f)
                }

                setPaddingRelative(context.dp(4), context.dp(1f), context.dp(4), context.dp(2f))

                descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

                inflateBoosted()
                inflateFollowed()
                inflateStatus(actMain)
                inflateConversationIcons()
                inflateSearchTag()
                inflateTrendTag()
                inflateList()
                inflateMessageHolder()
                inflateFollowRequest()
                inflateFilter()
            }
        }
    }
}
