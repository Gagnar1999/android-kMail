/*
 * Infomaniak Mail - Android
 * Copyright (C) 2023-2024 Infomaniak Network SA
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
package com.infomaniak.mail.ui.main.thread.actions

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.infomaniak.mail.data.cache.mailboxContent.AttachmentController
import com.infomaniak.mail.data.models.Attachment
import com.infomaniak.mail.di.IoDispatcher
import com.infomaniak.mail.utils.LocalStorageUtils
import com.infomaniak.mail.utils.Utils.runCatchingRealm
import com.infomaniak.mail.utils.coroutineContext
import com.infomaniak.mail.utils.extensions.appContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

@HiltViewModel
class DownloadAttachmentViewModel @Inject constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val attachmentController: AttachmentController,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AndroidViewModel(application) {

    private val ioCoroutineContext = viewModelScope.coroutineContext(ioDispatcher)

    private val attachmentLocalUuid
        inline get() = savedStateHandle.get<String>(DownloadAttachmentProgressDialogArgs::attachmentLocalUuid.name)!!

    /**
     * We keep the Attachment, in case the ViewModel is destroyed before it finishes downloading
     */
    private var attachment: Attachment? = null

    fun downloadAttachment() = liveData(ioCoroutineContext) {
        val attachment = runCatching {

            val localAttachment = attachmentController.getAttachment(attachmentLocalUuid).also { attachment = it }
            Log.e("TOTO", "local attachment = localuuid : ${localAttachment.localUuid} | resource = ${localAttachment.resource}")
            val attachmentFile = localAttachment.getCacheFile(appContext)
            Log.e("TOTO", "attachmentFile = $attachmentFile")

            var isAttachmentCached = localAttachment.hasUsableCache(appContext, attachmentFile)
            Log.e("TOTO", "isAttachmentCached = $isAttachmentCached")
            if (!isAttachmentCached) {
                isAttachmentCached = localAttachment.resource?.let { resource ->
                    Log.e("TOTO", "resource not null")
                    LocalStorageUtils.downloadThenSaveAttachmentToCacheDir(resource, attachmentFile)
                } ?: false
            }

            if (isAttachmentCached) {
                Log.e("TOTO", "emit attachment")
                attachment = null
                localAttachment
            } else {
                Log.e("TOTO", "emit null")
                null
            }
        }.getOrNull()

        emit(attachment)
    }

    override fun onCleared() = runCatchingRealm {
        // If we end up with an incomplete cached Attachment, we delete it
        attachment?.getCacheFile(appContext)?.apply { if (exists()) delete() }
        super.onCleared()
    }.getOrDefault(Unit)
}
