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
package com.infomaniak.mail.ui.main.settings.mailbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.infomaniak.lib.core.utils.SnackbarUtils.showSnackbar
import com.infomaniak.mail.data.models.mailbox.Mailbox
import com.infomaniak.mail.data.models.signature.Signature
import com.infomaniak.mail.databinding.FragmentSignatureSettingBinding
import com.infomaniak.mail.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.realm.kotlin.ext.copyFromRealm

@AndroidEntryPoint
class SignatureSettingFragment : Fragment() {

    private lateinit var binding: FragmentSignatureSettingBinding

    private val mainViewModel: MainViewModel by activityViewModels()
    private val signatureSettingViewModel: SignatureSettingViewModel by viewModels()

    private lateinit var signatureAdapter: SignatureSettingAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentSignatureSettingBinding.inflate(inflater, container, false).also { binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(signatureSettingViewModel) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter(mailbox)

        runCatching {
            updateSignatures()
        }.onFailure {
            showSnackbar(title = mainViewModel.errorOrNoConnectionStringRes)
        }

        observeSignatures()
        observeApiError()
    }

    private fun setupAdapter(mailbox: Mailbox) {
        binding.signatureList.adapter = SignatureSettingAdapter(
            canManageSignature = mailbox.permissions?.canManageSignatures ?: false,
            onSignatureSelected = ::onSignatureClicked,
        ).also { signatureAdapter = it }
    }

    private fun observeApiError() {
        signatureSettingViewModel.showError.observe(viewLifecycleOwner) { title ->
            showSnackbar(title)
        }
    }

    private fun observeSignatures() {
        signatureSettingViewModel.signaturesLive.observe(viewLifecycleOwner, signatureAdapter::setSignatures)
    }

    private fun onSignatureClicked(signature: Signature) = with(signatureSettingViewModel) {
        val newDefaultSignature = signature.copyFromRealm(UInt.MIN_VALUE).apply { isDefault = true }

        setDefaultSignature(newDefaultSignature, mainViewModel.errorOrNoConnectionStringRes)
    }
}
