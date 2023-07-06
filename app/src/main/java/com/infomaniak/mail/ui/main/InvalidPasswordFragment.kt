/*
 * Infomaniak ikMail - Android
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
package com.infomaniak.mail.ui.main

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.infomaniak.lib.core.utils.SnackbarUtils.showSnackbar
import com.infomaniak.lib.core.utils.hideProgress
import com.infomaniak.lib.core.utils.showKeyboard
import com.infomaniak.lib.core.utils.showProgress
import com.infomaniak.mail.MatomoMail.trackInvalidPasswordMailboxEvent
import com.infomaniak.mail.R
import com.infomaniak.mail.databinding.FragmentInvalidPasswordBinding
import com.infomaniak.mail.utils.createDescriptionDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InvalidPasswordFragment : Fragment() {

    private lateinit var binding: FragmentInvalidPasswordBinding
    private val navigationArgs: InvalidPasswordFragmentArgs by navArgs()
    private val invalidPasswordViewModel: InvalidPasswordViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentInvalidPasswordBinding.inflate(inflater, container, false).also { binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?): Unit = with(binding) {
        super.onViewCreated(view, savedInstanceState)

        enterPasswordDescription.text = getString(R.string.enterPasswordDescription, navigationArgs.mailboxEmail)

        passwordInput.showKeyboard()

        confirmButton.apply {
            isEnabled = false

            passwordInput.doAfterTextChanged {
                isEnabled = false
                passwordInput.text?.let(::manageButtonState)
            }

            setOnClickListener {
                trackInvalidPasswordMailboxEvent("updatePassword")
                showProgress()
                invalidPasswordViewModel.confirmPassword(
                    navigationArgs.mailboxId,
                    navigationArgs.mailboxObjectId,
                    passwordInput.text?.trim().toString(),
                ).observe(viewLifecycleOwner) {
                    passwordInputLayout.error = getString(it)
                    passwordInput.text = null
                    hideProgress(R.string.buttonConfirm)
                }
            }
        }

        detachMailbox.setOnClickListener {
            trackInvalidPasswordMailboxEvent("detachMailbox")
            createDescriptionDialog(
                title = getString(R.string.popupDetachMailboxTitle),
                description = navigationArgs.mailboxEmail,
                confirmButtonText = R.string.buttonDetach,
                onPositiveButtonClicked = {
                    trackInvalidPasswordMailboxEvent("detachMailboxConfirm")
                    invalidPasswordViewModel.detachMailbox(navigationArgs.mailboxId).observe(viewLifecycleOwner) { error ->
                        showSnackbar(error)
                    }
                },
            ).show()
        }
    }

    private fun manageButtonState(password: Editable) = with(binding) {
        if (password.count() in PASSWORD_LENGTH_RANGE) {
            passwordInputLayout.helperText = null
            confirmButton.isEnabled = true
        } else {
            passwordInputLayout.helperText = if (password.isEmpty()) null else getString(R.string.errorMailboxPasswordLength)
        }
    }

    private companion object {
        val PASSWORD_LENGTH_RANGE = 6..80
    }
}
