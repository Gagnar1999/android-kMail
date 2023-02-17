/*
 * Infomaniak kMail - Android
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
package com.infomaniak.mail.ui.main.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.infomaniak.lib.core.utils.safeNavigate
import com.infomaniak.mail.data.models.Folder.FolderRole
import com.infomaniak.mail.databinding.FragmentMoveBinding
import com.infomaniak.mail.views.MenuDrawerItemView

class MoveFragment : MenuFoldersFragment() {

    private lateinit var binding: FragmentMoveBinding
    private val navigationArgs: MoveFragmentArgs by navArgs()
    private val moveViewModel: MoveViewModel by viewModels()

    override val inboxFolder: MenuDrawerItemView by lazy { binding.inboxFolder }
    override val defaultFoldersList: RecyclerView by lazy { binding.defaultFoldersList }
    override val customFoldersList: RecyclerView by lazy { binding.customFoldersList }

    override val isInMenuDrawer = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentMoveBinding.inflate(inflater, container, false).also { binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeFolders()
    }

    override fun setupListeners() = with(binding) {
        super.setupListeners()

        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        iconAddFolder.setOnClickListener { safeNavigate(MoveFragmentDirections.actionMoveFragmentToNewFolderDialog()) }
    }

    private fun observeFolders() = with(navigationArgs) {
        moveViewModel.currentFolders.observe(viewLifecycleOwner) { (inbox, defaultFolders, customFolders) ->

            inboxFolderId = inbox?.id

            binding.inboxFolder.setSelectedState(folderId == inboxFolderId)
            defaultFoldersAdapter.setFolders(defaultFolders.filterNot { it.role == FolderRole.DRAFT }, folderId)
            customFoldersAdapter.setFolders(customFolders, folderId)
        }
    }

    override fun onFolderSelected(folderId: String): Unit = with(navigationArgs) {
        mainViewModel.moveTo(folderId, threadUid, messageUid)
        findNavController().popBackStack()
    }
}
