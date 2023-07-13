/*
 * Infomaniak ikMail - Android
 * Copyright (C) 2022-2023 Infomaniak Network SA
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
@file:UseSerializers(RealmListSerializer::class, RealmInstantSerializer::class)

package com.infomaniak.mail.data.models.draft

import android.content.Context
import android.util.Log
import com.infomaniak.lib.core.api.ApiController
import com.infomaniak.lib.core.utils.Utils.enumValueOfOrNull
import com.infomaniak.mail.data.api.RealmInstantSerializer
import com.infomaniak.mail.data.api.RealmListSerializer
import com.infomaniak.mail.data.cache.mailboxContent.SignatureController
import com.infomaniak.mail.data.models.Attachment
import com.infomaniak.mail.data.models.correspondent.Recipient
import com.infomaniak.mail.data.models.mailbox.Mailbox
import com.infomaniak.mail.utils.MessageBodyUtils
import com.infomaniak.mail.utils.SharedViewModelUtils
import io.realm.kotlin.MutableRealm
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.Ignore
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.*
import java.util.*

@Serializable
class Draft : RealmObject {

    //region Remote data
    @SerialName("uuid")
    var remoteUuid: String? = null

    var to: RealmList<Recipient> = realmListOf()
    var cc: RealmList<Recipient> = realmListOf()
    var bcc: RealmList<Recipient> = realmListOf()

    var subject: String? = null
    var body: String = ""
    var attachments: RealmList<Attachment> = realmListOf()

    @SerialName("mime_type")
    var mimeType: String = ""
    @SerialName("identity_id")
    var identityId: String? = null

    @SerialName("action")
    private var _action: String? = null

    @SerialName("in_reply_to")
    var inReplyTo: String? = null
    @SerialName("in_reply_to_uid")
    var inReplyToUid: String? = null
    @SerialName("forwarded_uid")
    var forwardedUid: String? = null

    var references: String? = null
    @SerialName("st_uuid")
    var swissTransferUuid: String? = null

    /**
     * This `delay` should NOT be removed. If we remove it, we won't receive any `etop` from the API when sending an Email.
     */
    var delay: Int = 0
    //endregion

    //region Local data (Transient)
    @Transient
    @PrimaryKey
    var localUuid: String = UUID.randomUUID().toString()
    @Transient
    var messageUid: String? = null
    //endregion

    //region UI data (Transient & Ignore)
    @Transient
    @Ignore
    var uiBody: String = ""
        set(value) {
            field = value
            Log.i("gibranlast", "uiBody got set to [$value]", );
        }
    @Transient
    @Ignore
    var uiSignature: String? = null
    @Transient
    @Ignore
    var uiQuote: String? = null
    //endregion

    var action
        get() = enumValueOfOrNull<DraftAction>(_action)
        set(value) {
            _action = value?.apiCallValue
        }

    fun initLocalValues(messageUid: String? = null, mimeType: String? = null) {
        messageUid?.let { this.messageUid = it }
        mimeType?.let { this.mimeType = it }
    }

    fun addMissingSignatureData(mailbox: Mailbox, realm: MutableRealm, context: Context) {
        Log.e("gibranlast", "addMissingSignatureData: ", );
        initSignature(mailbox, realm, addContent = false, context = context)
    }

    fun initSignature(mailbox: Mailbox, realm: MutableRealm, addContent: Boolean = true, context: Context) {

        SharedViewModelUtils.updateSignatures(mailbox, realm, context)
        val signature = SignatureController.getSignature(realm)

        identityId = signature.id.toString()

        if (addContent && signature.content.isNotEmpty()) {
            body += """<div class="${MessageBodyUtils.INFOMANIAK_SIGNATURE_HTML_CLASS_NAME}">${signature.content}</div>"""
        }
    }

    fun getJsonRequestBody(): MutableMap<String, JsonElement> {
        return draftJson.encodeToJsonElement(this).jsonObject.toMutableMap().apply {
            this[Draft::attachments.name] = JsonArray(attachments.map { JsonPrimitive(it.uuid) })
        }
    }

    enum class DraftAction(val apiCallValue: String, val matomoValue: String) {
        SAVE("save", "saveDraft"),
        SEND("send", "sendMail"),
    }

    enum class DraftMode {
        NEW_MAIL,
        REPLY,
        REPLY_ALL,
        FORWARD,
    }

    companion object {
        val actionPropertyName get() = Draft::_action.name
        private val draftJson = Json(ApiController.json) { encodeDefaults = true }
    }
}
