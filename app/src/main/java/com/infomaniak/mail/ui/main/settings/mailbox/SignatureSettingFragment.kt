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
package com.infomaniak.mail.ui.main.settings.mailbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.infomaniak.mail.databinding.FragmentSignatureSettingBinding
import com.infomaniak.mail.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignatureSettingFragment : Fragment() {

    private lateinit var binding: FragmentSignatureSettingBinding

    private val mainViewModel: MainViewModel by activityViewModels()
    private val signatureSettingViewModel: SignatureSettingViewModel by viewModels()

    private val signatureAdapter = SignatureSettingAdapter { }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentSignatureSettingBinding.inflate(inflater, container, false).also { binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.signatureList.adapter = signatureAdapter
        observeSignatures()
    }

    private fun observeSignatures() {
        signatureSettingViewModel.signaturesLive.observe(viewLifecycleOwner) { signatures ->
            signatureAdapter.setSignatures(signatures)
        }
    }
}
