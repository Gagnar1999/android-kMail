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
package com.infomaniak.mail.ui.main.folder

import android.transition.AutoTransition
import android.transition.TransitionManager
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeRecyclerView.ListOrientation.DirectionFlag
import com.infomaniak.lib.core.utils.context
import com.infomaniak.lib.core.utils.safeNavigate
import com.infomaniak.mail.MatomoMail.ACTION_ARCHIVE_NAME
import com.infomaniak.mail.MatomoMail.ACTION_DELETE_NAME
import com.infomaniak.mail.MatomoMail.ACTION_FAVORITE_NAME
import com.infomaniak.mail.MatomoMail.ACTION_MARK_AS_SEEN_NAME
import com.infomaniak.mail.MatomoMail.OPEN_ACTION_BOTTOM_SHEET
import com.infomaniak.mail.MatomoMail.trackMultiSelectActionEvent
import com.infomaniak.mail.R
import com.infomaniak.mail.data.LocalSettings
import com.infomaniak.mail.data.LocalSettings.ThreadDensity
import com.infomaniak.mail.data.models.Folder.FolderRole
import com.infomaniak.mail.data.models.thread.Thread
import com.infomaniak.mail.databinding.FragmentThreadListBinding
import com.infomaniak.mail.ui.MainActivity
import com.infomaniak.mail.ui.MainViewModel
import com.infomaniak.mail.utils.Utils.runCatchingRealm
import com.infomaniak.mail.utils.createDescriptionDialog
import com.infomaniak.mail.utils.updateNavigationBarColor

class ThreadListMultiSelection {

    lateinit var binding: FragmentThreadListBinding
    lateinit var mainViewModel: MainViewModel
    lateinit var threadListFragment: ThreadListFragment
    lateinit var threadListAdapter: ThreadListAdapter
    lateinit var unlockSwipeActionsIfSet: () -> Unit
    lateinit var localSettings: LocalSettings

    private var shouldMultiselectRead: Boolean = false
    private var shouldMultiselectFavorite: Boolean = true

    fun initMultiSelection(
        binding: FragmentThreadListBinding,
        mainViewModel: MainViewModel,
        threadListFragment: ThreadListFragment,
        threadListAdapter: ThreadListAdapter,
        unlockSwipeActionsIfSet: () -> Unit,
        localSettings: LocalSettings,
    ) {
        this.binding = binding
        this.mainViewModel = mainViewModel
        this.threadListFragment = threadListFragment
        this.threadListAdapter = threadListAdapter
        this.unlockSwipeActionsIfSet = unlockSwipeActionsIfSet
        this.localSettings = localSettings

        setupMultiSelectionActions()

        observerMultiSelection()
    }

    private fun setupMultiSelectionActions() = with(mainViewModel) {
        binding.quickActionBar.setOnItemClickListener { menuId ->
            val selectedThreadsUids = selectedThreads.map { it.uid }
            val selectedThreadsCount = selectedThreadsUids.count()

            when (menuId) {
                R.id.quickActionUnread -> {
                    threadListFragment.trackMultiSelectActionEvent(ACTION_MARK_AS_SEEN_NAME, selectedThreadsCount)
                    toggleThreadsSeenStatus(selectedThreadsUids, shouldMultiselectRead)
                    isMultiSelectOn = false
                }
                R.id.quickActionArchive -> {
                    threadListFragment.trackMultiSelectActionEvent(ACTION_ARCHIVE_NAME, selectedThreadsCount)
                    archiveThreads(selectedThreadsUids)
                    isMultiSelectOn = false
                }
                R.id.quickActionFavorite -> {
                    threadListFragment.trackMultiSelectActionEvent(ACTION_FAVORITE_NAME, selectedThreadsCount)
                    toggleThreadsFavoriteStatus(selectedThreadsUids, shouldMultiselectFavorite)
                    isMultiSelectOn = false
                }
                R.id.quickActionDelete -> {

                    fun multiSelectDelete() {
                        threadListFragment.trackMultiSelectActionEvent(ACTION_DELETE_NAME, selectedThreadsCount)
                        deleteThreads(selectedThreadsUids)
                        isMultiSelectOn = false
                    }

                    fun confirmMultiSelectDelete() {
                        val resources = threadListFragment.resources
                        val titleId = R.plurals.threadListDeletionConfirmationAlertTitle
                        threadListFragment.createDescriptionDialog(
                            title = resources.getQuantityString(titleId, selectedThreadsCount, selectedThreadsCount),
                            description = resources.getQuantityString(
                                R.plurals.threadListDeletionConfirmationAlertDescription,
                                selectedThreadsCount,
                            ),
                            onPositiveButtonClicked = { multiSelectDelete() },
                        ).show()
                    }

                    when (currentFolder.value?.role) {
                        FolderRole.DRAFT, FolderRole.SPAM, FolderRole.TRASH -> confirmMultiSelectDelete()
                        else -> multiSelectDelete()
                    }
                }
                R.id.quickActionMenu -> {
                    threadListFragment.trackMultiSelectActionEvent(OPEN_ACTION_BOTTOM_SHEET, selectedThreadsCount)
                    val direction = if (selectedThreadsCount == 1) {
                        isMultiSelectOn = false
                        ThreadListFragmentDirections.actionThreadListFragmentToThreadActionsBottomSheetDialog(
                            threadUid = selectedThreadsUids.single(),
                            shouldLoadDistantResources = false,
                        )
                    } else {
                        ThreadListFragmentDirections.actionThreadListFragmentToMultiSelectBottomSheetDialog()
                    }
                    threadListFragment.safeNavigate(direction)
                }
            }
        }
    }

    private fun observerMultiSelection() = with(binding) {
        mainViewModel.isMultiSelectOnLiveData.observe(threadListFragment.viewLifecycleOwner) { isMultiSelectOn ->
            threadListAdapter.updateSelection()
            if (localSettings.threadDensity != ThreadDensity.LARGE) TransitionManager.beginDelayedTransition(threadsList)
            if (!isMultiSelectOn) mainViewModel.selectedThreads.clear()

            displaySelectionToolbar(isMultiSelectOn)
            lockDrawerAndSwipe(isMultiSelectOn)
            hideUnreadChip(isMultiSelectOn)
            displayMultiSelectActions(isMultiSelectOn)
        }

        mainViewModel.selectedThreadsLiveData.observe(threadListFragment.viewLifecycleOwner) { selectedThreads ->
            updateSelectedCount(selectedThreads)
            updateSelectAllLabel()
            updateMultiSelectActionsStatus(selectedThreads)
        }
    }

    private fun displaySelectionToolbar(isMultiSelectOn: Boolean) = with(binding) {
        val autoTransition = AutoTransition()
        autoTransition.duration = TOOLBAR_FADE_DURATION
        TransitionManager.beginDelayedTransition(toolbarLayout, autoTransition)

        toolbar.isGone = isMultiSelectOn
        toolbarSelection.isVisible = isMultiSelectOn
    }

    private fun lockDrawerAndSwipe(isMultiSelectOn: Boolean) = with(binding) {
        (threadListFragment.activity as MainActivity).setDrawerLockMode(!isMultiSelectOn)
        if (isMultiSelectOn) {
            threadsList.apply {
                disableSwipeDirection(DirectionFlag.LEFT)
                disableSwipeDirection(DirectionFlag.RIGHT)
            }
        } else {
            unlockSwipeActionsIfSet()
        }
    }

    private fun hideUnreadChip(isMultiSelectOn: Boolean) = runCatchingRealm {
        val thereAreUnread = mainViewModel.currentFolderLive.value?.let { it.unreadCountLocal > 0 } == true
        binding.unreadCountChip.isVisible = thereAreUnread && !isMultiSelectOn
    }.getOrDefault(Unit)

    private fun displayMultiSelectActions(isMultiSelectOn: Boolean) = with(binding) {
        newMessageFab.isGone = isMultiSelectOn
        quickActionBar.isVisible = isMultiSelectOn
        val navBarColor = context.getColor(if (isMultiSelectOn) R.color.elevatedBackground else R.color.backgroundColor)
        threadListFragment.requireActivity().window.updateNavigationBarColor(navBarColor)
    }

    private fun updateSelectedCount(selectedThreads: MutableSet<Thread>) {
        val threadCount = selectedThreads.count()
        binding.selectedCount.text = threadListFragment.resources.getQuantityString(
            R.plurals.multipleSelectionCount,
            threadCount,
            threadCount
        )
    }

    private fun updateSelectAllLabel() {
        val selectAllLabel = if (mainViewModel.isEverythingSelected) R.string.buttonUnselectAll else R.string.buttonSelectAll
        binding.selectAll.setText(selectAllLabel)
    }

    private fun updateMultiSelectActionsStatus(selectedThreads: MutableSet<Thread>) {
        computeReadFavoriteStatus(selectedThreads).let { (shouldRead, shouldFavorite) ->
            shouldMultiselectRead = shouldRead
            shouldMultiselectFavorite = shouldFavorite
        }

        binding.quickActionBar.apply {
            val (readIcon, readText) = getReadIconAndShortText(shouldMultiselectRead)
            changeIcon(READ_UNREAD_INDEX, readIcon)
            changeText(READ_UNREAD_INDEX, readText)

            val favoriteIcon = if (shouldMultiselectFavorite) R.drawable.ic_star else R.drawable.ic_unstar
            changeIcon(FAVORITE_INDEX, favoriteIcon)

            val isSelectionEmpty = selectedThreads.isEmpty()
            val isInsideArchive = mainViewModel.isCurrentFolderRole(FolderRole.ARCHIVE)
            for (index in 0 until getButtonCount()) {
                val shouldDisable = isSelectionEmpty || (isInsideArchive && index == ARCHIVE_INDEX)
                if (shouldDisable) disable(index) else enable(index)
            }
        }
    }

    companion object {
        private const val TOOLBAR_FADE_DURATION = 150L

        private const val READ_UNREAD_INDEX = 0
        private const val ARCHIVE_INDEX = 1
        private const val FAVORITE_INDEX = 2

        fun computeReadFavoriteStatus(selectedThreads: Set<Thread>): Pair<Boolean, Boolean> {
            var shouldUnread = true
            var shouldUnfavorite = selectedThreads.isNotEmpty()

            for (thread in selectedThreads) {
                shouldUnread = shouldUnread && thread.unseenMessagesCount == 0
                shouldUnfavorite = shouldUnfavorite && thread.isFavorite

                if (!shouldUnread && !shouldUnfavorite) break
            }
            return !shouldUnread to !shouldUnfavorite
        }

        fun getReadIconAndShortText(shouldRead: Boolean): Pair<Int, Int> {
            return if (shouldRead) {
                R.drawable.ic_envelope_open to R.string.actionShortMarkAsRead
            } else {
                R.drawable.ic_envelope to R.string.actionShortMarkAsUnread
            }
        }
    }
}
