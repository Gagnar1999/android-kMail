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
package com.infomaniak.mail.ui.alertDialogs

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.infomaniak.lib.core.utils.context
import com.infomaniak.mail.R
import com.infomaniak.mail.databinding.DialogDescriptionBinding
import com.infomaniak.mail.utils.AlertDialogUtils.negativeButton
import com.infomaniak.mail.utils.AlertDialogUtils.positiveButton
import dagger.hilt.android.qualifiers.ActivityContext
import com.infomaniak.lib.core.R as RCore

abstract class BaseAlertDialog(@ActivityContext private val activityContext: Context) {

    protected val activity = activityContext as Activity
    protected val binding by lazy { DialogDescriptionBinding.inflate(activity.layoutInflater) }

    var description: CharSequence = ""
    var title: String = ""
    @StringRes
    var confirmButtonText = R.string.buttonConfirm

    protected val alertDialog = initDialog()

    private fun initDialog() = with(binding) {
        MaterialAlertDialogBuilder(context)
            .setView(root)
            .setPositiveButton(confirmButtonText, null)
            .setNegativeButton(RCore.string.buttonCancel, null)
            .create()
    }

    protected fun showDialog(
        title: String? = null,
        description: CharSequence? = null,
        @StringRes confirmButtonText: Int? = null,
        displayCancelButton: Boolean = true,
    ): Unit = with(alertDialog) {
        show()

        title?.let(binding.dialogTitle::setText)
        description?.let(binding.dialogDescription::setText)
        confirmButtonText?.let(positiveButton::setText)
        negativeButton.isVisible = displayCancelButton
    }
}
