/*
 * Infomaniak kMail - Android
 * Copyright (C) 2022 Infomaniak Network SA
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
package com.infomaniak.mail.ui.login

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class IntroPagerAdapter(manager: FragmentManager, lifecycle: Lifecycle, private val isFirstAccount: Boolean) :
    FragmentStateAdapter(manager, lifecycle) {

    override fun getItemCount() = if (isFirstAccount) 4 else 3

    override fun createFragment(position: Int): Fragment {
        return IntroFragment().apply {
            arguments = IntroFragmentArgs(if (isFirstAccount) position else position + 1).toBundle()
        }
    }
}
