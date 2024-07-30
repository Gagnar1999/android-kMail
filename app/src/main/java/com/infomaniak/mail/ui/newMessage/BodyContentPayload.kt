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
package com.infomaniak.mail.ui.newMessage


/**
 * @param content The string representation of the body in either html or plain text format.
 * @param type The type of representation of [content]. Each type will lead to different processing of the content.
 */
data class BodyContentPayload(val content: String, val type: BodyContentType) {
    companion object {
        fun emptyBody(): BodyContentPayload {
            return BodyContentPayload("", BodyContentType.TEXT_PLAIN_WITHOUT_HTML)
        }
    }
}

enum class BodyContentType { HTML_SANITIZED, HTML_UNSANITIZED, TEXT_PLAIN_WITH_HTML, TEXT_PLAIN_WITHOUT_HTML }
