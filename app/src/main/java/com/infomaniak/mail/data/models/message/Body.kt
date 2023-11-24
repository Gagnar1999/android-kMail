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
package com.infomaniak.mail.data.models.message

import com.infomaniak.mail.data.api.FlatteningSubBodiesSerializer
import com.infomaniak.mail.utils.Utils
import com.infomaniak.mail.utils.htmlToText
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.RealmList
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class Body : EmbeddedRealmObject {

    //region Remote data
    var value: String = ""
    var type: String = ""

    @Serializable(FlatteningSubBodiesSerializer::class)
    @SerialName("subBody")
    var subBodies: RealmList<SubBody> = realmListOf()
    //endregion

    fun asText(): String {
        return when (type) {
            Utils.TEXT_HTML -> value.htmlToText()
            else -> value
        }
    }
}
