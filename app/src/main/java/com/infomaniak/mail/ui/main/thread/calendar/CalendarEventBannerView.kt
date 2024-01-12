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
package com.infomaniak.mail.ui.main.thread.calendar

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.infomaniak.mail.data.models.calendar.Attendee
import com.infomaniak.mail.data.models.calendar.Attendee.AttendanceState
import com.infomaniak.mail.databinding.ViewCalendarEventBannerBinding
import com.infomaniak.mail.utils.UiUtils.getPrettyNameAndEmail

class CalendarEventBannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewCalendarEventBannerBinding.inflate(LayoutInflater.from(context), this, true) }

    //region Debug
    private val attendees = listOf<Attendee>(
        createAttendee("alice@info.com", "Alice in Borderlands", false, AttendanceState.ACCEPTED),
        createAttendee("bob@info.com", "Bob Dylan", false, AttendanceState.DECLINED),
        createAttendee("charles.windsor@infomaniak.com", "Charles Windsor", true, AttendanceState.TENTATIVE),
        createAttendee("delta@info.com", "Delta Rune", false, AttendanceState.NEEDS_ACTION),
        createAttendee("echo@info.com", "Echo Location", false, AttendanceState.NEEDS_ACTION),
    )

    private fun createAttendee(email: String, name: String?, isOrganizer: Boolean, state: AttendanceState): Attendee =
        Attendee().apply {
            this.email = email
            name?.let { this.name = it }
            this.isOrganizer = isOrganizer
            this.state = state
        }
    //endregion

    init {
        with(binding) {
            attendeesButton.apply {
                isGone = attendees.isEmpty()
                addOnCheckedChangeListener { _, isChecked ->
                    attendeesSubMenu.isVisible = isChecked
                }
            }

            displayOrganizer()
            allAttendeesButton.setOnClickListener {/* TODO */ }
            manyAvatarsView.setAttendees(attendees)
        }
        // attrs?.getAttributes(context, R.styleable.CalendarEventBannerView) {
        //     with(binding) {
        //
        //     }
        // }
    }

    private fun displayOrganizer() = with(binding) {
        val organizer = attendees.singleOrNull(Attendee::isOrganizer)
        organizerLayout.isGone = organizer == null

        organizer?.let {
            organizerAvatar.loadAvatar(organizer)

            val (name, _) = context.getPrettyNameAndEmail(organizer, true) // TODO : do we want to ignoreIsMe?
            organizerName.text = "$name (Organisateur)" // TODO : Use a string resource
        }
    }
}
