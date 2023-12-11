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
package com.infomaniak.mail.ui.main.thread.actions

import android.app.Dialog
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.infomaniak.lib.core.R
import com.infomaniak.lib.core.utils.SnackbarUtils.showSnackbar
import com.infomaniak.lib.core.utils.setBackNavigationResult
import com.infomaniak.mail.databinding.DialogDownloadProgressBinding
import com.infomaniak.mail.ui.MainViewModel
import com.infomaniak.mail.utils.AttachmentIntentUtils.DOWNLOAD_ATTACHMENT_RESULT
import com.infomaniak.mail.utils.AttachmentIntentUtils.getIntentOrGoToPlaystore
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DownloadAttachmentProgressDialog : DialogFragment() {

    private val binding by lazy { DialogDownloadProgressBinding.inflate(layoutInflater) }
    private val navigationArgs: DownloadAttachmentProgressDialogArgs by navArgs()
    private val mainViewModel: MainViewModel by activityViewModels()
    private val downloadAttachmentViewModel: DownloadAttachmentViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = false
        val iconDrawable = AppCompatResources.getDrawable(requireContext(), navigationArgs.attachmentType.icon)
        binding.icon.setImageDrawable(iconDrawable)

        downloadAttachment()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(navigationArgs.attachmentName)
            .setView(binding.root)
            .setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    findNavController().popBackStack()
                    true
                } else false
            }
            .create()
    }

    private fun downloadAttachment() {
        downloadAttachmentViewModel.downloadAttachment().observe(this) { cachedAttachment ->
            if (cachedAttachment == null) {
                popBackStackWithError()
            } else {
                cachedAttachment.getIntentOrGoToPlaystore(requireContext(), navigationArgs.intentType)?.let { openWithIntent ->
                    setBackNavigationResult(DOWNLOAD_ATTACHMENT_RESULT, openWithIntent)
                } ?: run { findNavController().popBackStack() }
            }
        }
    }

    private fun popBackStackWithError() {
        val title = if (mainViewModel.isInternetAvailable.value == true) R.string.anErrorHasOccurred else R.string.noConnection
        showSnackbar(title)
        findNavController().popBackStack()
    }
}
