/*
 * Infomaniak Mail - Android
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
package com.infomaniak.mail.di

import com.infomaniak.mail.data.cache.RealmDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RealmDatabaseModule {

    @Provides
    @Singleton
    @AppSettingsRealm
    fun providesAppSettingsRealm() = RealmDatabase.appSettings() // TODO: Waiting for AccountUtils injection

    @Provides
    @UserInfoRealm
    fun providesUserInfoRealm() = RealmDatabase.userInfo()

    @Provides
    @Singleton
    @MailboxInfoRealm
    fun providesMailboxInfoRealm() = RealmDatabase.mailboxInfo

    @Provides
    @Singleton
    fun providesMailboxContent() = RealmDatabase.MailboxContent()
}
