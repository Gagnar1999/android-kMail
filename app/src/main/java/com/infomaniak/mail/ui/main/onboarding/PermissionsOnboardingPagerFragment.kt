/*
 * Infomaniak Mail - Android
 * Copyright (C) 2024 Infomaniak Network SA
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
package com.infomaniak.mail.ui.main.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.infomaniak.lib.core.utils.safeBinding
import com.infomaniak.lib.core.utils.safeNavigate
import com.infomaniak.mail.R
import com.infomaniak.mail.data.LocalSettings
import com.infomaniak.mail.databinding.FragmentPermissionsOnboardingPagerBinding
import com.infomaniak.mail.ui.MainViewModel
import com.infomaniak.mail.utils.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PermissionsOnboardingPagerFragment : Fragment() {

    private var binding: FragmentPermissionsOnboardingPagerBinding by safeBinding()
    private val mainViewModel: MainViewModel by activityViewModels()

    @Inject
    lateinit var localSettings: LocalSettings

    @Inject
    lateinit var permissionUtils: PermissionUtils

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentPermissionsOnboardingPagerBinding.inflate(inflater, container, false).also { binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?): Unit = with(binding) {
        super.onViewCreated(view, savedInstanceState)

        permissionUtils.apply {
            registerReadContactsPermission(fragment = this@PermissionsOnboardingPagerFragment)
            registerNotificationsPermissionIfNeeded(fragment = this@PermissionsOnboardingPagerFragment)
        }

        permissionsViewpager.apply {
            adapter = PermissionsPagerAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
            isUserInputEnabled = false
        }

        continueButton.setOnClickListener {
            when (permissionsViewpager.currentItem) {
                0 -> {
                    permissionUtils.requestReadContactsPermission { hasPermission ->
                        if (hasPermission) mainViewModel.updateUserInfo()
                        permissionsViewpager.currentItem += 1
                    }
                }
                1 -> {
                    permissionUtils.requestNotificationsPermissionIfNeeded { safeNavigate(R.id.threadListFragment) }
                }
            }

        }
    }
}
