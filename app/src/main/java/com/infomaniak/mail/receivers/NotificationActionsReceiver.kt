/*
 * Infomaniak ikMail - Android
 * Copyright (C) 2023 Infomaniak Network SA
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
package com.infomaniak.mail.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.core.app.NotificationManagerCompat
import com.infomaniak.mail.R
import com.infomaniak.mail.data.api.ApiRepository
import com.infomaniak.mail.data.cache.RealmDatabase
import com.infomaniak.mail.data.cache.mailboxContent.FolderController
import com.infomaniak.mail.data.cache.mailboxContent.MessageController
import com.infomaniak.mail.data.cache.mailboxInfo.MailboxController
import com.infomaniak.mail.data.models.Folder.FolderRole
import com.infomaniak.mail.data.models.mailbox.Mailbox
import com.infomaniak.mail.di.IoDispatcher
import com.infomaniak.mail.utils.NotificationUtils.NotificationPayload
import com.infomaniak.mail.utils.NotificationUtils.NotificationPayload.NotificationBehavior
import com.infomaniak.mail.utils.NotificationUtils.NotificationPayload.NotificationBehavior.NotificationType
import com.infomaniak.mail.utils.NotificationUtils.showNotification
import com.infomaniak.mail.utils.SharedUtils
import com.infomaniak.mail.utils.getUids
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationManagerCompat: NotificationManagerCompat

    @Inject
    lateinit var folderController: FolderController

    @Inject
    lateinit var sharedUtils: SharedUtils

    @Inject
    lateinit var notificationJobsBus: NotificationJobsBus

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    lateinit var globalCoroutineScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        globalCoroutineScope.launch(ioDispatcher) {
            val payload = intent.getSerializableExtra(EXTRA_PAYLOAD) as? NotificationPayload ?: return@launch
            val action = intent.action!!
            handleNotificationIntent(context, payload, action)
        }
    }

    private fun handleNotificationIntent(context: Context, payload: NotificationPayload, action: String) = with(payload) {

        // Undo action
        if (action == UNDO_ACTION) {
            executeUndoAction(context, payload)
            return
        }

        // Other actions
        val (folderRole, undoNotificationTitle) = when (action) {
            ARCHIVE_ACTION -> FolderRole.ARCHIVE to R.string.notificationTitleArchive
            DELETE_ACTION -> FolderRole.TRASH to R.string.notificationTitleDelete
            else -> null
        } ?: return
        executeAction(context, folderRole, undoNotificationTitle, payload)
    }

    private fun executeUndoAction(context: Context, payload: NotificationPayload) {

        // Cancel action
        notificationJobsBus.unregister(payload.notificationId)

        showNotification(
            context = context,
            notificationManagerCompat = notificationManagerCompat,
            payload = payload.apply { behavior = null },
        )
    }

    private fun executeAction(
        context: Context,
        folderRole: FolderRole,
        @StringRes undoNotificationTitle: Int,
        payload: NotificationPayload,
    ) = with(payload) {

        showNotification(
            context = context,
            notificationManagerCompat = notificationManagerCompat,
            payload = payload.apply {
                behavior = NotificationBehavior(
                    type = NotificationType.UNDO,
                    behaviorTitle = context.getString(undoNotificationTitle),
                )
            },
        )

        val job = globalCoroutineScope.launch(ioDispatcher) {

            delay(UNDO_TIMEOUT)
            ensureActive()

            val realm = RealmDatabase.newMailboxContentInstance(userId, mailboxId)
            val message = MessageController.getMessage(messageUid!!, realm) ?: return@launch
            val threads = message.threads.filter { it.folderId == message.folderId }

            val mailbox = MailboxController.getMailbox(userId, mailboxId) ?: return@launch
            val messages = sharedUtils.getMessagesToMove(threads, message)
            val destinationId = folderController.getFolder(folderRole)?.id ?: return@launch

            dismissNotification(context, mailbox, notificationId)
            ApiRepository.moveMessages(mailbox.uuid, messages.getUids(), destinationId)
        }

        notificationJobsBus.register(notificationId, job)
    }

    private fun dismissNotification(context: Context, mailbox: Mailbox, notificationId: Int) {
        if (notificationId == -1) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val count = notificationManager.activeNotifications.count { mailbox.notificationGroupKey in it.groupKey }

        val notificationIdToCancel = if (count <= 2) mailbox.notificationGroupId else notificationId
        notificationManagerCompat.cancel(notificationIdToCancel)
    }

    companion object {
        const val EXTRA_PAYLOAD = "extra_payload"
        const val ARCHIVE_ACTION = "archive_action"
        const val DELETE_ACTION = "delete_action"
        const val UNDO_ACTION = "undo_action"

        private const val UNDO_TIMEOUT = 6_000L
    }
}
