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
package com.infomaniak.mail.utils

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.infomaniak.mail.R
import com.infomaniak.mail.data.cache.RealmDatabase
import com.infomaniak.mail.data.cache.mailboxContent.FolderController
import com.infomaniak.mail.data.cache.mailboxContent.MessageController
import com.infomaniak.mail.data.cache.mailboxContent.RefreshController
import com.infomaniak.mail.data.cache.mailboxContent.RefreshController.RefreshMode
import com.infomaniak.mail.data.cache.mailboxContent.ThreadController
import com.infomaniak.mail.data.models.Folder.FolderRole
import com.infomaniak.mail.data.models.mailbox.Mailbox
import com.infomaniak.mail.data.models.thread.Thread
import com.infomaniak.mail.utils.NotificationPayload.NotificationBehavior
import com.infomaniak.mail.utils.NotificationPayload.NotificationBehavior.NotificationType
import com.infomaniak.mail.utils.NotificationUtils.showMessageNotification
import io.realm.kotlin.Realm
import io.sentry.Sentry
import io.sentry.SentryLevel
import okhttp3.OkHttpClient
import javax.inject.Inject

class FetchMessagesManager @Inject constructor(
    private val appContext: Context,
    private val notificationManagerCompat: NotificationManagerCompat,
) {

    suspend fun execute(userId: Int, mailbox: Mailbox, sentryMessageUid: String? = null, mailboxContentRealm: Realm? = null) {

        // Don't launch sync if the Mailbox's notifications have been disabled by the user
        if (mailbox.notificationsIsDisabled(notificationManagerCompat)) return

        val realm = mailboxContentRealm ?: RealmDatabase.newMailboxContentInstance(userId, mailbox.mailboxId)
        val folder = FolderController.getFolder(FolderRole.INBOX, realm) ?: return
        if (folder.cursor == null) return
        val okHttpClient = AccountUtils.getHttpClient(userId)

        // Update Local with Remote
        val newMessagesThreads = RefreshController.refreshThreads(
            refreshMode = RefreshMode.REFRESH_FOLDER_WITH_ROLE,
            mailbox = mailbox,
            folder = folder,
            okHttpClient = okHttpClient,
            realm = realm,
            isFromService = true,
        ) ?: return

        Log.d(TAG, "launchWork: ${mailbox.email} has ${newMessagesThreads.count()} Threads with new Messages")

        // Notify Threads with new Messages
        val unReadThreadsCount = ThreadController.getUnreadThreadsCount(folder)
        newMessagesThreads.forEachIndexed { index, thread ->
            thread.showThreadNotification(
                userId = userId,
                mailbox = mailbox,
                realm = realm,
                unReadThreadsCount = unReadThreadsCount,
                isLastMessage = index == newMessagesThreads.lastIndex,
                sentryMessageUid = sentryMessageUid,
                okHttpClient = okHttpClient,
            )
        }

        realm.close()
    }

    private suspend fun Thread.showThreadNotification(
        userId: Int,
        mailbox: Mailbox,
        realm: Realm,
        unReadThreadsCount: Int,
        isLastMessage: Boolean,
        sentryMessageUid: String?,
        okHttpClient: OkHttpClient,
    ) {

        ThreadController.fetchIncompleteMessages(messages, mailbox, okHttpClient, realm)
        val message = MessageController.getThreadLastMessageInFolder(uid, realm) ?: run {
            ThreadController.getThread(uid, realm)?.let { thread ->
                Sentry.withScope { scope ->
                    scope.level = SentryLevel.ERROR
                    scope.setExtra("does Thread still exist ?", "[true]")
                    scope.setExtra("currentMailboxEmail", "[${AccountUtils.currentMailboxEmail}]")
                    scope.setExtra("mailbox.email", "[${mailbox.email}]")
                    scope.setExtra("messageUid", "[$sentryMessageUid]")
                    scope.setExtra("folderName", "[${thread.folder.name}]")
                    scope.setExtra("threadUid", "[${thread.uid}]")
                    scope.setExtra("messagesCount", "[${thread.messages.count()}]")
                    scope.setExtra("messagesFolder", "[${thread.messages.map { "${it.folder.name} (${it.folderId})" }}]")
                    Sentry.captureMessage("We are supposed to display a Notification, but we couldn't find the Message in the Thread.")
                }
            }
            return
        }
        if (message.isSeen) return // Ignore if it has already been seen

        val subject = appContext.formatSubject(message.subject)
        val preview = if (message.body?.value.isNullOrBlank()) {
            ""
        } else {
            message.body
                ?.let { "\n${MessageBodyUtils.splitBodyAndQuote(it).messageBody.htmlToText().trim()}" }
                ?: message.preview.ifBlank { null }?.let { "\n${it.trim()}" }
                ?: ""
        }
        val formattedPreview = preview.replace("\\n+\\s*".toRegex(), "\n") // Ignore multiple/start whitespaces
        val description = "$subject$formattedPreview"

        // Show Message notification
        showMessageNotification(
            context = appContext,
            notificationManagerCompat = notificationManagerCompat,
            payload = NotificationPayload(
                userId = userId,
                mailboxId = mailbox.mailboxId,
                threadUid = uid,
                messageUid = message.uid,
                payloadTitle = message.sender.displayedName(appContext),
                payloadContent = subject,
                payloadDescription = description,
            )
        )

        // Show Group Summary notification
        if (isLastMessage) {
            val summaryText = appContext.resources.getQuantityString(
                R.plurals.newMessageNotificationSummary,
                unReadThreadsCount,
                unReadThreadsCount,
            )
            showMessageNotification(
                context = appContext,
                notificationManagerCompat = notificationManagerCompat,
                payload = NotificationPayload(
                    userId = userId,
                    mailboxId = mailbox.mailboxId,
                    threadUid = uid,
                    behavior = NotificationBehavior(
                        type = NotificationType.SUMMARY,
                        behaviorContent = summaryText,
                    ),
                ),
            )
        }
    }

    private companion object {
        val TAG = FetchMessagesManager::class.simpleName
    }
}
