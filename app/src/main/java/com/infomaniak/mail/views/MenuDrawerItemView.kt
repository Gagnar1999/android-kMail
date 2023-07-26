/*
 * Infomaniak ikMail - Android
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
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.getDimensionPixelSizeOrThrow
import androidx.core.view.isVisible
import com.google.android.material.shape.ShapeAppearanceModel
import com.infomaniak.lib.core.utils.context
import com.infomaniak.lib.core.utils.getAttributes
import com.infomaniak.lib.core.utils.setMarginsRelative
import com.infomaniak.mail.R
import com.infomaniak.mail.databinding.ItemMenuDrawerBinding
import com.infomaniak.mail.utils.UiUtils.formatUnreadCount
import com.infomaniak.mail.utils.getAttributeColor
import com.google.android.material.R as RMaterial
import com.infomaniak.lib.core.R as RCore

class MenuDrawerItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    val binding by lazy { ItemMenuDrawerBinding.inflate(LayoutInflater.from(context), this, true) }

    private val regular by lazy { ResourcesCompat.getFont(context, RCore.font.suisseintl_regular) }
    private val medium by lazy { ResourcesCompat.getFont(context, RCore.font.suisseintl_medium) }

    var icon: Drawable? = null
        set(value) {
            field = value
            binding.itemName.setCompoundDrawablesWithIntrinsicBounds(value, null, null, null)
        }

    var indent: Int = 0
        set(value) {
            field = value
            binding.itemName.setMarginsRelative(start = value)
        }

    var itemStyle = SelectionStyle.MENU_DRAWER
        set(value) {
            field = value
            if (value == SelectionStyle.MENU_DRAWER) {
                binding.root.apply {
                    context.obtainStyledAttributes(R.style.MenuDrawerItem, intArrayOf(android.R.attr.layout_marginStart)).let {
                        setMarginsRelative(it.getDimensionPixelSizeOrThrow(0))
                        it.recycle()
                    }
                    ShapeAppearanceModel.builder(context, 0, R.style.MenuDrawerItemShapeAppearance).build()
                }
            } else {
                binding.root.apply {
                    setMarginsRelative(0)
                    shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(0.0f).build()
                    if (value == SelectionStyle.ACCOUNT) setContentPadding(0, 0, 0, 0)
                }
            }
        }

    var text: CharSequence? = null
        set(value) {
            field = value
            binding.itemName.text = value
        }

    var textWeight = TextWeight.MEDIUM
        set(fontFamily) {
            field = fontFamily
            binding.itemName.typeface = if (fontFamily == TextWeight.MEDIUM) medium else regular
        }

    var badge: Int = 0
        set(value) {
            field = value
            binding.itemBadge.apply {
                isVisible = shouldDisplayBadge()
                text = formatUnreadCount(value)
            }
        }

    var isPastilleDisplayed = false
        set(isDisplayed) {
            field = isDisplayed
            computeEndIconVisibility()
        }

    private var isInSelectedState = false

    init {
        attrs?.getAttributes(context, R.styleable.DecoratedTextItemView) {
            badge = getInteger(R.styleable.DecoratedTextItemView_badge, badge)
            icon = getDrawable(R.styleable.DecoratedTextItemView_icon)
            indent = getDimensionPixelSize(R.styleable.DecoratedTextItemView_indent, indent)
            itemStyle = SelectionStyle.values()[getInteger(R.styleable.DecoratedTextItemView_itemStyle, 0)]
            text = getString(R.styleable.DecoratedTextItemView_text)
            textWeight = TextWeight.values()[getInteger(R.styleable.DecoratedTextItemView_textWeight, 0)]
        }
    }

    fun setSelectedState(isSelected: Boolean) = with(binding) {
        isInSelectedState = isSelected
        val (color, textAppearance) = if (isSelected && itemStyle == SelectionStyle.MENU_DRAWER) {
            context.getAttributeColor(RMaterial.attr.colorPrimaryContainer) to R.style.BodyMedium_Accent
        } else {
            Color.TRANSPARENT to if (textWeight == TextWeight.MEDIUM) R.style.BodyMedium else R.style.Body
        }

        root.setCardBackgroundColor(color)
        itemName.setTextAppearance(textAppearance)

        checkmark.isVisible = shouldDisplayCheckmark()
    }

    private fun shouldDisplayBadge() = !shouldDisplayPastille() && badge > 0
    private fun shouldDisplayPastille() = isPastilleDisplayed
    private fun shouldDisplayCheckmark() = isInSelectedState && itemStyle != SelectionStyle.MENU_DRAWER

    private fun computeEndIconVisibility() = binding.apply {
        itemBadge.isVisible = shouldDisplayBadge()
        pastille.isVisible = shouldDisplayPastille()
        checkmark.isVisible = shouldDisplayCheckmark()
    }

    override fun setOnClickListener(onClickListener: OnClickListener?) {
        binding.root.setOnClickListener(onClickListener)
    }

    enum class SelectionStyle {
        MENU_DRAWER,
        ACCOUNT,
        OTHER,
    }

    enum class TextWeight {
        REGULAR,
        MEDIUM,
    }
}
