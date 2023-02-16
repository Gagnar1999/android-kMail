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
package com.infomaniak.mail.ui.main.thread.actions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.infomaniak.mail.R
import com.infomaniak.mail.data.models.message.Message
import com.infomaniak.mail.databinding.BottomSheetJunkBinding
import com.infomaniak.mail.ui.MainViewModel
import com.infomaniak.mail.utils.notYetImplemented

class JunkBottomSheetDialog : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetJunkBinding
    private val navigationArgs: JunkBottomSheetDialogArgs by navArgs()
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return BottomSheetJunkBinding.inflate(inflater, container, false).also { binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(navigationArgs) {
        super.onViewCreated(view, savedInstanceState)

        if (messageUid == null) {
            handleButtons(threadUid)
        } else {
            mainViewModel.getMessage(messageUid).observe(viewLifecycleOwner) { message ->
                handleButtons(threadUid, message)
            }
        }
    }

    private fun handleButtons(threadUid: String, message: Message? = null) = with(binding) {
        setSpamUi(message)

        spam.setClosingOnClickListener { mainViewModel.toggleSpamOrHam(threadUid, message) }
        phishing.setClosingOnClickListener { notYetImplemented() }
        blockSender.setClosingOnClickListener { notYetImplemented() }
    }

    private fun setSpamUi(message: Message?) {
        binding.spam.setText(if (mainViewModel.isSpam(message)) R.string.actionNonSpam else R.string.actionSpam)
    }

    private fun ActionItemView.setClosingOnClickListener(callback: (() -> Unit)) {
        setOnClickListener {
            callback()
            findNavController().popBackStack()
        }
    }
}
