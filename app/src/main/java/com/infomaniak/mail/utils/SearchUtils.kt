/*
 * Infomaniak Mail - Android
 * Copyright (C) 2023-2025 Infomaniak Network SA
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
import com.infomaniak.lib.core.utils.SentryLog
import com.infomaniak.mail.data.cache.RealmDatabase
import com.infomaniak.mail.data.cache.mailboxContent.FolderController
import com.infomaniak.mail.data.cache.mailboxContent.MessageController
import com.infomaniak.mail.data.cache.mailboxContent.ThreadController
import com.infomaniak.mail.data.models.message.Message
import com.infomaniak.mail.data.models.thread.Thread
import com.infomaniak.mail.data.models.thread.Thread.ThreadFilter
import com.infomaniak.mail.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import io.realm.kotlin.TypedRealm
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchUtils @Inject constructor(
    private val mailboxContentRealm: RealmDatabase.MailboxContent,
    @ApplicationContext private val appContext: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    fun searchFilters(query: String?, filters: Set<ThreadFilter>, resource: String?): String {

        val filtersQuery = StringBuilder("severywhere=${if (filters.contains(ThreadFilter.FOLDER)) "0" else "1"}")

        if (query?.isNotBlank() == true) filtersQuery.append("&scontains=$query")

        with(filters) {
            if (contains(ThreadFilter.ATTACHMENTS)) filtersQuery.append("&sattachments=yes")

            if (resource == null) when {
                contains(ThreadFilter.SEEN) -> filtersQuery.append("&filters=seen")
                contains(ThreadFilter.UNSEEN) -> filtersQuery.append("&filters=unseen")
                contains(ThreadFilter.STARRED) -> filtersQuery.append("&filters=starred")
                else -> Unit
            }
        }

        return filtersQuery.toString()
    }

    fun selectFilter(filter: ThreadFilter, selectedFilters: MutableSet<ThreadFilter>): MutableSet<ThreadFilter> {
        val filtersToRemove = when (filter) {
            ThreadFilter.SEEN -> setOf(ThreadFilter.UNSEEN, ThreadFilter.STARRED)
            ThreadFilter.UNSEEN -> setOf(ThreadFilter.SEEN, ThreadFilter.STARRED)
            ThreadFilter.STARRED -> setOf(ThreadFilter.SEEN, ThreadFilter.UNSEEN)
            else -> emptySet()
        }

        return selectedFilters.apply {
            removeAll(filtersToRemove)
            add(filter)
        }
    }

    suspend fun deleteRealmSearchData() = withContext(ioDispatcher) {
        mailboxContentRealm().write {
            SentryLog.i(TAG, "SearchUtils>deleteRealmSearchData: remove old search data")
            MessageController.deleteSearchMessages(realm = this)
            ThreadController.deleteSearchThreads(realm = this)
            FolderController.deleteSearchFolderData(realm = this)
        }
    }

    fun convertToSearchThreads(searchMessages: List<Message>): List<Thread> {
        val cachedFolderNames = mutableMapOf<String, String>()

        return searchMessages.map { message ->
            message.toThread().apply {
                uid = "search-${message.uid}"
                isFromSearch = true
                recomputeThread()
                sharedThreadProcessing(appContext, cachedFolderNames, realm = mailboxContentRealm())
            }
        }
    }

    companion object {
        private val TAG = SearchUtils::class.java.simpleName

        /**
         * Thread processing that applies to both search threads from the api and from realm. Be careful, it relies on
         * [Thread.folderId] being set correctly.
         */
        fun Thread.sharedThreadProcessing(context: Context, cachedFolderNames: MutableMap<String, String>, realm: TypedRealm) {
            setFolderName(cachedFolderNames, realm, context)
        }

        private fun Thread.setFolderName(cachedFolderNames: MutableMap<String, String>, realm: TypedRealm, context: Context) {
            val computedFolderName = cachedFolderNames[folderId]
                ?: FolderController.getFolder(folderId, realm)
                    ?.getLocalizedName(context)
                    ?.also { cachedFolderNames[folderId] = it }

            computedFolderName?.let { folderName = it }
        }
    }
}
