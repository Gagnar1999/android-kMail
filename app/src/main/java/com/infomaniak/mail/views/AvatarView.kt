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
package com.infomaniak.mail.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import coil.imageLoader
import coil.load
import coil.request.Disposable
import com.infomaniak.lib.core.models.user.User
import com.infomaniak.lib.core.utils.CoilUtils.simpleImageLoader
import com.infomaniak.lib.core.utils.getAttributes
import com.infomaniak.lib.core.utils.loadAvatar
import com.infomaniak.mail.R
import com.infomaniak.mail.data.models.correspondent.Correspondent
import com.infomaniak.mail.data.models.correspondent.MergedContact
import com.infomaniak.mail.data.models.correspondent.Recipient
import com.infomaniak.mail.databinding.ViewAvatarBinding

class AvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewAvatarBinding.inflate(LayoutInflater.from(context), this, true) }

    init {
        attrs?.getAttributes(context, R.styleable.AvatarView) {
            binding.avatarImage.setImageDrawable(getDrawable(R.styleable.AvatarView_android_src))
        }
    }

    override fun setOnClickListener(onClickListener: OnClickListener?) = binding.avatar.setOnClickListener(onClickListener)

    fun loadAvatar(user: User): Disposable {
        val color = context.getColor(R.color.onColorfulBackground)
        return binding.avatarImage.loadAvatar(user.id, user.avatar, user.getInitials(), context.simpleImageLoader, color)
    }

    fun loadAvatar(recipient: Recipient, contacts: Map<String, Map<String, MergedContact>>) {
        val recipientsForEmail = contacts[recipient.email]
        val mergedContact = recipientsForEmail?.getOrElse(recipient.name) { recipientsForEmail.entries.elementAt(0).value }
        binding.avatarImage.loadCorrespondentAvatar(mergedContact ?: recipient)
    }

    fun loadAvatar(mergedContact: MergedContact) {
        binding.avatarImage.loadCorrespondentAvatar(mergedContact)
    }

    fun loadUnknownUserAvatar() {
        binding.avatarImage.load(R.drawable.ic_unknown_user_avatar)
    }

    fun setImageDrawable(drawable: Drawable?) = binding.avatarImage.setImageDrawable(drawable)

    private fun ImageView.loadCorrespondentAvatar(correspondent: Correspondent): Disposable = with(correspondent) {
        val avatar = (correspondent as? MergedContact)?.avatar
        val color = context.getColor(R.color.onColorfulBackground)
        return loadAvatar(email.hashCode(), avatar, initials, context.imageLoader, color)
    }
}
