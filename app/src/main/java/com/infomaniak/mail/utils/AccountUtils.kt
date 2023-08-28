/*
 * Infomaniak Mail - Android
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
package com.infomaniak.mail.utils

import android.app.NotificationManager
import android.content.Context
import androidx.work.WorkManager
import com.infomaniak.lib.core.InfomaniakCore
import com.infomaniak.lib.core.auth.CredentialManager
import com.infomaniak.lib.core.models.user.User
import com.infomaniak.lib.core.networking.HttpClient
import com.infomaniak.lib.core.room.UserDatabase
import com.infomaniak.lib.core.utils.SentryLog
import com.infomaniak.mail.data.LocalSettings
import com.infomaniak.mail.data.cache.RealmDatabase
import com.infomaniak.mail.data.cache.appSettings.AppSettingsController
import com.infomaniak.mail.data.models.AppSettings
import com.infomaniak.mail.data.models.mailbox.Mailbox
import io.sentry.Sentry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import io.sentry.protocol.User as SentryUser

object AccountUtils : CredentialManager() {

    override lateinit var userDatabase: UserDatabase

    var reloadApp: (suspend () -> Unit)? = null

    fun init(context: Context) {
        userDatabase = UserDatabase.getDatabase(context)

        Sentry.setUser(SentryUser().apply { id = currentUserId.toString() })
    }

    override var currentUser: User? = null
        set(user) {
            field = user
            currentUserId = user?.id ?: AppSettings.DEFAULT_ID
            Sentry.setUser(SentryUser().apply {
                id = currentUserId.toString()
                email = user?.email
            })
            InfomaniakCore.bearerToken = user?.apiToken?.accessToken.toString()
        }

    override var currentUserId: Int = AppSettingsController.getAppSettings().currentUserId
        set(userId) {
            field = userId
            RealmDatabase.resetUserInfo()
            AppSettingsController.updateAppSettings { appSettings -> appSettings.currentUserId = userId }
        }

    var currentMailboxId: Int = AppSettingsController.getAppSettings().currentMailboxId
        set(mailboxId) {
            field = mailboxId
            RealmDatabase.resetMailboxContent()
            AppSettingsController.updateAppSettings { appSettings -> appSettings.currentMailboxId = mailboxId }
        }

    var currentMailboxEmail: String? = null

    suspend fun switchToMailbox(mailboxId: Int) {
        RealmDatabase.backUpPreviousMailboxContent()
        currentMailboxId = mailboxId
        reloadApp?.invoke()
    }

    suspend fun manageMailboxesEdgeCases(context: Context, mailboxes: List<Mailbox>): Boolean {

        val shouldStop = when {
            mailboxes.isEmpty() -> {
                Dispatchers.Main { context.launchNoMailboxActivity(shouldStartLoginActivity = true) }
                true
            }
            mailboxes.none { it.isValid } -> {
                Dispatchers.Main { context.launchNoValidMailboxesActivity() }
                true
            }
            mailboxes.none { it.mailboxId == currentMailboxId } -> {
                reloadApp?.invoke()
                true
            }
            else -> false
        }

        return shouldStop
    }

    suspend fun requestCurrentUser(): User? {
        return (getUserById(currentUserId) ?: userDatabase.userDao().getFirst()).also { currentUser = it }
    }

    suspend fun addUser(user: User) {
        currentUser = user
        userDatabase.userDao().insert(user)
    }

    suspend fun removeUser(context: Context, user: User, playServicesUtils: PlayServicesUtils, shouldReload: Boolean = true) {

        fun logoutUserToken() {
            CoroutineScope(Dispatchers.IO).launch {
                context.getInfomaniakLogin().deleteToken(
                    okHttpClient = HttpClient.okHttpClientNoTokenInterceptor,
                    token = user.apiToken,
                    onError = { SentryLog.e("DeleteTokenError", "API response error: $it") },
                )
            }
        }

        logoutUserToken()

        userDatabase.userDao().delete(user)
        RealmDatabase.removeUserData(context, user.id)
        val localSettings = LocalSettings.getInstance(context)
        localSettings.removeRegisteredFirebaseUser(userId = user.id)

        if (user.id == currentUserId) {
            if (getAllUsersCount() == 0) {
                resetSettings(context, localSettings)
                playServicesUtils.deleteFirebaseToken()
            }
            if (shouldReload) reloadApp?.invoke()
        }
    }

    private fun resetSettings(context: Context, localSettings: LocalSettings) {
        AppSettingsController.removeAppSettings()
        localSettings.removeSettings()
        with(WorkManager.getInstance(context)) {
            cancelAllWork()
            pruneWork()
        }
        // Dismiss all current notifications
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
    }

    fun getAllUsersSync(): List<User> = userDatabase.userDao().getAllSync()

}
