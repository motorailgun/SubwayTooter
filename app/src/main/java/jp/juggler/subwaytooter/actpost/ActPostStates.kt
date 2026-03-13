package jp.juggler.subwaytooter.actpost

import android.os.Bundle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import jp.juggler.subwaytooter.ActPost
import jp.juggler.subwaytooter.api.TootParser
import jp.juggler.subwaytooter.api.entity.EntityId
import jp.juggler.subwaytooter.api.entity.EntityIdSerializer
import jp.juggler.subwaytooter.api.entity.TootScheduled
import jp.juggler.subwaytooter.api.entity.TootVisibility
import jp.juggler.subwaytooter.api.entity.parseItem
import jp.juggler.subwaytooter.kJson
import jp.juggler.subwaytooter.util.AttachmentPicker
import jp.juggler.subwaytooter.util.PostAttachment
import jp.juggler.util.data.decodeJsonObject
import jp.juggler.util.data.toJsonArray
import jp.juggler.util.log.LogCategory
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

private val log = LogCategory("ActPostStates")

// Data class for serialization only
@Serializable
data class ActPostStatesData(
    ////////////
    // states requires special handling
    var accountDbId: Long? = null,
    var pickerState: String? = null,
    var attachmentListEncoded: String? = null,
    var scheduledStatusEncoded: String? = null,
    ////////////

    var visibility: TootVisibility? = null,

    @Serializable(with = EntityIdSerializer::class)
    var redraftStatusId: EntityId? = null,

    @Serializable(with = EntityIdSerializer::class)
    var editStatusId: EntityId? = null,

    var mushroomInput: Int = 0,
    var mushroomStart: Int = 0,
    var mushroomEnd: Int = 0,

    var timeSchedule: Long = 0L,

    @Serializable(with = EntityIdSerializer::class)
    var inReplyToId: EntityId? = null,
    var inReplyToText: String? = null,
    var inReplyToImage: String? = null,
    var inReplyToUrl: String? = null,
)

// Wrapper class with mutableStateOf properties
class ActPostStates(
    ////////////
    // states requires special handling
    var accountDbId: Long? = null,
    var pickerState: String? = null,
    var attachmentListEncoded: String? = null,
    var scheduledStatusEncoded: String? = null,
    ////////////

    var visibility: TootVisibility? = null,

    @Serializable(with = EntityIdSerializer::class)
    var redraftStatusId: EntityId? = null,

    @Serializable(with = EntityIdSerializer::class)
    var editStatusId: EntityId? = null,

    var mushroomInput: Int = 0,
    var mushroomStart: Int = 0,
    var mushroomEnd: Int = 0,

    var timeSchedule: Long = 0L,

    inReplyToIdInitial: EntityId? = null,
    inReplyToTextInitial: String? = null,
    inReplyToImageInitial: String? = null,
    inReplyToUrlInitial: String? = null,
) {
    // Wrap reply properties in mutableStateOf for Compose reactivity
    var inReplyToId: EntityId? by mutableStateOf(inReplyToIdInitial)
    var inReplyToText: String? by mutableStateOf(inReplyToTextInitial)
    var inReplyToImage: String? by mutableStateOf(inReplyToImageInitial)
    var inReplyToUrl: String? by mutableStateOf(inReplyToUrlInitial)

    // Convert to data class for serialization
    fun toData(): ActPostStatesData = ActPostStatesData(
        accountDbId = accountDbId,
        pickerState = pickerState,
        attachmentListEncoded = attachmentListEncoded,
        scheduledStatusEncoded = scheduledStatusEncoded,
        visibility = visibility,
        redraftStatusId = redraftStatusId,
        editStatusId = editStatusId,
        mushroomInput = mushroomInput,
        mushroomStart = mushroomStart,
        mushroomEnd = mushroomEnd,
        timeSchedule = timeSchedule,
        inReplyToId = inReplyToId,
        inReplyToText = inReplyToText,
        inReplyToImage = inReplyToImage,
        inReplyToUrl = inReplyToUrl,
    )

    // Convert from data class
    companion object {
        fun fromData(data: ActPostStatesData): ActPostStates = ActPostStates(
            accountDbId = data.accountDbId,
            pickerState = data.pickerState,
            attachmentListEncoded = data.attachmentListEncoded,
            scheduledStatusEncoded = data.scheduledStatusEncoded,
            visibility = data.visibility,
            redraftStatusId = data.redraftStatusId,
            editStatusId = data.editStatusId,
            mushroomInput = data.mushroomInput,
            mushroomStart = data.mushroomStart,
            mushroomEnd = data.mushroomEnd,
            timeSchedule = data.timeSchedule,
            inReplyToIdInitial = data.inReplyToId,
            inReplyToTextInitial = data.inReplyToText,
            inReplyToImageInitial = data.inReplyToImage,
            inReplyToUrlInitial = data.inReplyToUrl,
        )
    }
}

// 画面状態の保存
fun ActPost.saveState(outState: Bundle) {
    states.accountDbId = account?.db_id
    states.pickerState = attachmentPicker.encodeState()

    states.scheduledStatusEncoded = scheduledStatus?.encodeSimple()?.toString()

    // アップロード完了したものだけ保持する
    states.attachmentListEncoded = attachmentList
        .filter { it.status == PostAttachment.Status.Ok }
        .mapNotNull { it.attachment?.encodeJson() }
        .toJsonArray()
        .toString()

    val encoded = kJson.encodeToString(states.toData())
    log.d("onSaveInstanceState: $encoded")
    outState.putString(ActPost.STATE_ALL, encoded)

    // test decoding
    kJson.decodeFromString<AttachmentPicker.States>(encoded)
}

// 画面状態の復元
suspend fun ActPost.restoreState(savedInstanceState: Bundle) {

    resetText() // also load account list

    savedInstanceState.getString(ActPost.STATE_ALL)?.let { jsonText ->
        val statesData = kJson.decodeFromString<ActPostStatesData>(jsonText)
        states = ActPostStates.fromData(statesData)
        states.pickerState?.let { attachmentPicker.restoreState(it) }
        this.account = null // いちど選択を外してから再選択させる
        accountList.find { it.db_id == states.accountDbId }?.let { selectAccount(it) }

        account?.let { a ->
            states.scheduledStatusEncoded?.let { jsonText ->
                scheduledStatus = parseItem(jsonText.decodeJsonObject()) {
                    TootScheduled(TootParser(this, a), it)
                }
            }
        }
        val stateAttachmentList = appState.attachmentList
        if (!isMultiWindowPost && stateAttachmentList != null) {
            // static なデータが残ってるならそれを使う
            this.attachmentList = stateAttachmentList
            // コールバックを新しい画面に差し替える
            for (pa in attachmentList) {
                pa.callback = this
            }
        } else {
            // state から復元する
            states.attachmentListEncoded?.let {
                decodeAttachments(it)
                saveAttachmentList()
            }
        }
    }

    afterUpdateText()
}
