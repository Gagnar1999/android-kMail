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
package com.infomaniak.mail.ui.main.thread.actions

import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.infomaniak.mail.MatomoMail.ACTION_ARCHIVE_NAME
import com.infomaniak.mail.MatomoMail.ACTION_FAVORITE_NAME
import com.infomaniak.mail.MatomoMail.ACTION_FORWARD_NAME
import com.infomaniak.mail.MatomoMail.ACTION_MARK_AS_SEEN_NAME
import com.infomaniak.mail.MatomoMail.ACTION_MOVE_NAME
import com.infomaniak.mail.MatomoMail.ACTION_POSTPONE_NAME
import com.infomaniak.mail.MatomoMail.ACTION_PRINT_NAME
import com.infomaniak.mail.MatomoMail.ACTION_REPLY_ALL_NAME
import com.infomaniak.mail.MatomoMail.ACTION_REPLY_NAME
import com.infomaniak.mail.MatomoMail.ACTION_SPAM_NAME
import com.infomaniak.mail.MatomoMail.ACTION_TRASH_NAME
import com.infomaniak.mail.R
import com.infomaniak.mail.data.models.Folder.FolderRole
import com.infomaniak.mail.data.models.draft.Draft.DraftMode
import com.infomaniak.mail.ui.main.menu.MoveFragmentArgs
import com.infomaniak.mail.utils.animatedNavigation
import com.infomaniak.mail.utils.notYetImplemented
import com.infomaniak.mail.utils.safeNavigateToNewMessageActivity

class ThreadActionsBottomSheetDialog : MailActionsBottomSheetDialog() {

    private val navigationArgs: ThreadActionsBottomSheetDialogArgs by navArgs()
    private val threadActionsViewModel: ThreadActionsViewModel by viewModels()

    private val currentClassName: String by lazy { ThreadActionsBottomSheetDialog::class.java.name }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(navigationArgs) {
        super.onViewCreated(view, savedInstanceState)

        threadActionsViewModel.threadLive(threadUid).observe(viewLifecycleOwner) { thread ->
            setMarkAsReadUi(thread.unseenMessagesCount == 0)
            setFavoriteUi(thread.isFavorite)
        }

        binding.postpone.isGone = true
        setSpamUi()

        threadActionsViewModel.getThreadAndMessageUidToReplyTo(
            threadUid,
            messageUidToReplyTo,
        ).observe(viewLifecycleOwner) { (thread, messageUidToReply) ->

            initOnClickListener(object : OnActionClick {
                //region Main actions
                override fun onReply() {
                    trackBottomSheetThreadActionsEvent(ACTION_REPLY_NAME)
                    safeNavigateToNewMessageActivity(DraftMode.REPLY, messageUidToReply, currentClassName)
                }

                override fun onReplyAll() {
                    trackBottomSheetThreadActionsEvent(ACTION_REPLY_ALL_NAME)
                    safeNavigateToNewMessageActivity(DraftMode.REPLY_ALL, messageUidToReply, currentClassName)
                }

                override fun onForward() {
                    trackBottomSheetThreadActionsEvent(ACTION_FORWARD_NAME)
                    safeNavigateToNewMessageActivity(DraftMode.FORWARD, messageUidToReply, currentClassName)
                }

                override fun onDelete() {
                    trackBottomSheetThreadActionsEvent(ACTION_TRASH_NAME)
                    mainViewModel.deleteThread(threadUid)
                }
                //endregion

                //region Actions
                override fun onArchive() = with(mainViewModel) {
                    trackBottomSheetThreadActionsEvent(ACTION_ARCHIVE_NAME, isCurrentFolderRole(FolderRole.ARCHIVE))
                    archiveThread(threadUid)
                }

                override fun onReadUnread() {
                    trackBottomSheetThreadActionsEvent(ACTION_MARK_AS_SEEN_NAME, thread.unseenMessagesCount == 0)
                    mainViewModel.toggleThreadSeenStatus(threadUid)
                    findNavController().popBackStack(R.id.threadFragment, inclusive = true)
                }

                override fun onMove() {
                    trackBottomSheetThreadActionsEvent(ACTION_MOVE_NAME)
                    animatedNavigation(
                        resId = R.id.moveFragment,
                        args = MoveFragmentArgs(arrayOf(threadUid)).toBundle(),
                        currentClassName = currentClassName,
                    )
                }

                override fun onPostpone() {
                    trackBottomSheetThreadActionsEvent(ACTION_POSTPONE_NAME)
                    TODO("Not yet implemented")
                }

                override fun onFavorite() {
                    trackBottomSheetThreadActionsEvent(ACTION_FAVORITE_NAME, thread.isFavorite)
                    mainViewModel.toggleThreadFavoriteStatus(threadUid)
                }

                override fun onSpam() = with(mainViewModel) {
                    trackBottomSheetThreadActionsEvent(ACTION_SPAM_NAME, isCurrentFolderRole(FolderRole.SPAM))
                    toggleThreadSpamStatus(threadUid)
                }

                override fun onReportJunk() = Unit

                override fun onPrint() {
                    trackBottomSheetThreadActionsEvent(ACTION_PRINT_NAME)
                    notYetImplemented()
                }

                override fun onReportDisplayProblem() {
                    notYetImplemented()
                }
                //endregion
            })
        }
    }

    private fun setSpamUi() = with(binding) {
        reportJunk.isGone = true
        spam.apply {
            isVisible = true
            setText(if (mainViewModel.isCurrentFolderRole(FolderRole.SPAM)) R.string.actionNonSpam else R.string.actionSpam)
        }
    }
}
