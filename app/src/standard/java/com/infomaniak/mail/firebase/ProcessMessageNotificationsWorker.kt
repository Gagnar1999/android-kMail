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
package com.infomaniak.mail.firebase

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.infomaniak.mail.data.cache.RealmDatabase
import com.infomaniak.mail.data.cache.mailboxContent.MessageController
import com.infomaniak.mail.data.cache.mailboxInfo.MailboxController
import com.infomaniak.mail.di.IoDispatcher
import com.infomaniak.mail.utils.FetchMessagesManager
import com.infomaniak.mail.workers.BaseProcessMessageNotificationsWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.sentry.Sentry
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process Firebase notifications.
 */
@HiltWorker
class ProcessMessageNotificationsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val fetchMessagesManager: FetchMessagesManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BaseProcessMessageNotificationsWorker(appContext, params) {

    private val mailboxInfoRealm by lazy { RealmDatabase.newMailboxInfoInstance }

    override suspend fun launchWork(): Result = with(ioDispatcher) {
        Log.i(TAG, "Work started")
        val userId = inputData.getIntOrNull(USER_ID_KEY) ?: return@with Result.success()
        val mailboxId = inputData.getIntOrNull(MAILBOX_ID_KEY) ?: return@with Result.success()
        val messageUid = inputData.getString(MESSAGE_UID_KEY) ?: return@with Result.success()
        val mailbox = MailboxController.getMailbox(userId, mailboxId, mailboxInfoRealm) ?: run {
            val mailboxes = MailboxController.getMailboxes(realm = mailboxInfoRealm)
            Sentry.withScope { scope ->
                scope.setExtra("userId", userId.toString())
                scope.setExtra("mailboxId", mailboxId.toString())
                scope.setExtra("mailboxesId", "${mailboxes.map { "mailboxId:[${it.mailboxId}] (userId:[${it.userId}])" }}")
                scope.setExtra("messageUid", messageUid)
                Sentry.captureMessage("We should not have received this notification")
            }
            return@with Result.success()
        }

        val mailboxContentRealm = RealmDatabase.newMailboxContentInstance(userId, mailbox.mailboxId)
        MessageController.getMessage(messageUid, mailboxContentRealm)?.let { return@with Result.success() }
        fetchMessagesManager.execute(userId, mailbox, messageUid, mailboxContentRealm)

        Log.i(TAG, "Work finished")
        Result.success()
    }

    override fun onFinish() {
        mailboxInfoRealm.close()
    }

    @Singleton
    class Scheduler @Inject constructor(private val workManager: WorkManager) {

        fun scheduleWork(userId: Int, mailboxId: Int, messageUid: String) {
            Log.i(TAG, "Work scheduled")

            val workName = workName(userId, mailboxId)
            val workData = workDataOf(USER_ID_KEY to userId, MAILBOX_ID_KEY to mailboxId, MESSAGE_UID_KEY to messageUid)
            val workRequest = OneTimeWorkRequestBuilder<ProcessMessageNotificationsWorker>()
                .addTag(TAG)
                .setInputData(workData)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()

            workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.APPEND_OR_REPLACE, workRequest)
        }

    }

    companion object {
        private const val USER_ID_KEY = "userIdKey"
        private const val MAILBOX_ID_KEY = "mailboxIdKey"
        private const val MESSAGE_UID_KEY = "messageUidKey"

        private fun workName(userId: Int, mailboxId: Int) = "${userId}_$mailboxId"
    }
}
