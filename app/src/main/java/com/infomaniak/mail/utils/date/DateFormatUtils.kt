/*
 * Infomaniak Mail - Android
 * Copyright (C) 2025 Infomaniak Network SA
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
package com.infomaniak.mail.utils.date

import android.content.Context
import android.text.format.DateFormat
import com.infomaniak.lib.core.utils.FORMAT_DATE_DAY_MONTH
import com.infomaniak.lib.core.utils.format
import com.infomaniak.mail.R
import java.util.Date

object DateFormatUtils {

    // Do not use the 12/24 hours format directly. Call localHourFormat() instead
    private const val FORMAT_DATE_24HOUR = "HH:mm"
    private const val FORMAT_DATE_12HOUR = "hh:mm a"
    private const val FORMAT_DATE_WITH_YEAR = "d MMM yyyy"
    private const val FORMAT_DATE_WITHOUT_YEAR = "d MMM"

    fun Context.formatTime(date: Date): String {
        return date.format(localHourFormat())
    }

    fun Context.fullDateWithYear(date: Date): String {
        return date.formatDateTime(this, FORMAT_DATE_WITHOUT_YEAR, localHourFormat())
    }

    fun Context.fullDateWithoutYear(date: Date): String {
        return date.formatDateTime(this, FORMAT_DATE_WITH_YEAR, localHourFormat())
    }

    fun Context.dayOfWeekDate(date: Date): String {
        return date.formatDateTime(this, FORMAT_DATE_DAY_MONTH, localHourFormat())
    }

    private fun Date.formatDateTime(context: Context, dateFormat: String, timeFormat: String) = context.getString(
        R.string.messageDetailsDateAt,
        format(dateFormat),
        format(timeFormat),
    )

    private fun Context.localHourFormat(): String {
        return if (DateFormat.is24HourFormat(this)) FORMAT_DATE_24HOUR else FORMAT_DATE_12HOUR
    }
}
