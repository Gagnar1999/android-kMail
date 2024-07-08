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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.infomaniak.mail.data.cache.mailboxContent.AttachmentController
import com.infomaniak.mail.data.cache.mailboxContent.SwissTransferFileController
import com.infomaniak.mail.data.models.Attachable
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AttachmentActionsViewModel @Inject constructor(
    attachmentController: AttachmentController,
    swissTransferFileController: SwissTransferFileController,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val isSwissTransferFile
        inline get() = savedStateHandle.get<Boolean>(AttachmentActionsBottomSheetDialogArgs::isSwissTransferFile.name)!!

    private val attachmentLocalUuid
        inline get() = savedStateHandle.get<String>(AttachmentActionsBottomSheetDialogArgs::attachmentLocalUuid.name)!!

    val attachment: Attachable? = runCatching {
        if (isSwissTransferFile) {
            swissTransferFileController.getSwissTransferFile(attachmentLocalUuid)
        } else {
            attachmentController.getAttachment(attachmentLocalUuid)
        }
    }.getOrNull()
}
