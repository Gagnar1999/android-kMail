/*
 * Infomaniak kMail - Android
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
import android.util.Log
import androidx.work.WorkManager
import com.infomaniak.lib.core.InfomaniakCore
import com.infomaniak.lib.core.auth.CredentialManager
import com.infomaniak.lib.core.auth.TokenAuthenticator
import com.infomaniak.lib.core.models.user.User
import com.infomaniak.lib.core.networking.HttpClient
import com.infomaniak.lib.core.networking.HttpClient.okHttpClient
import com.infomaniak.lib.core.room.UserDatabase
import com.infomaniak.mail.GplayUtils
import com.infomaniak.mail.data.LocalSettings
import com.infomaniak.mail.data.api.ApiRepository
import com.infomaniak.mail.data.cache.RealmDatabase
import com.infomaniak.mail.data.cache.appSettings.AppSettingsController
import com.infomaniak.mail.data.cache.mailboxInfo.MailboxController
import com.infomaniak.mail.data.models.AppSettings
import io.sentry.Sentry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
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
            RealmDatabase.closeUserInfo()
            AppSettingsController.updateAppSettings { appSettings -> appSettings.currentUserId = userId }
        }

    var currentMailboxId: Int = AppSettingsController.getAppSettings().currentMailboxId
        set(mailboxId) {
            field = mailboxId
            RealmDatabase.closeMailboxContent()
            AppSettingsController.updateAppSettings { appSettings -> appSettings.currentMailboxId = mailboxId }
        }

    var currentMailboxEmail: String? = null

    suspend fun switchToMailbox(mailboxId: Int) {
        currentMailboxId = mailboxId
        RealmDatabase.close()
        reloadApp?.invoke()
    }

    suspend fun requestCurrentUser(): User? {
        return (getUserById(currentUserId) ?: userDatabase.userDao().getFirst()).also { currentUser = it }
    }

    suspend fun addUser(user: User) {
        currentUser = user
        userDatabase.userDao().insert(user)
    }

    fun updateUserAndMailboxes(context: Context) = CoroutineScope(Dispatchers.IO).launch {
        val user = ApiRepository.getUserProfile(okHttpClient).data ?: return@launch
        updateMailboxes(context, user)
    }

    private suspend fun updateMailboxes(context: Context, user: User) {

        val apiResponse = ApiRepository.getMailboxes(okHttpClient)
        val mailboxes = apiResponse.data

        when {
            !apiResponse.isSuccess() -> return
            mailboxes.isNullOrEmpty() -> removeUser(context, user)
            else -> {
                requestUser(user)
                MailboxController.updateMailboxes(context, mailboxes)
            }
        }
    }

    private suspend fun requestUser(remoteUser: User) {
        TokenAuthenticator.mutex.withLock {
            if (remoteUser.id == currentUserId) {
                remoteUser.organizations = arrayListOf()
                requestCurrentUser()?.let { localUser ->
                    setUserToken(remoteUser, localUser.apiToken)
                    currentUser = remoteUser
                }
            }
        }
    }

    suspend fun removeUser(context: Context, user: User) {

        fun logoutUserToken() {
            CoroutineScope(Dispatchers.IO).launch {
                context.getInfomaniakLogin().deleteToken(
                    okHttpClient = HttpClient.okHttpClientNoInterceptor,
                    token = user.apiToken,
                    onError = { Log.e("DeleteTokenError", "API response error: $it") },
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
                GplayUtils.deleteFirebaseToken()
            }
            reloadApp?.invoke()
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
