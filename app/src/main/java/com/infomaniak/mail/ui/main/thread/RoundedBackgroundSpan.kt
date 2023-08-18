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
package com.infomaniak.mail.ui.main.thread

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.RectF
import android.graphics.Typeface
import android.text.style.LineHeightSpan
import android.text.style.ReplacementSpan

/**
 * A span to create a rounded background on a text.
 *
 * If radius is set, it generates a rounded background.
 * If radius is null, it generates a circle background.
 */
class RoundedBackgroundSpan(
    private val backgroundColor: Int,
    private val textColor: Int,
    private val textTypeface: Typeface,
    private val padding: Int = 16,
    private val radius: Float,
    private val verticalOffset: Float,
) : ReplacementSpan(), LineHeightSpan {
    override fun getSize(paint: Paint, text: CharSequence?, start: Int, end: Int, fm: FontMetricsInt?): Int {
        return (padding + paint.measureText(text, start, end) + padding).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        paint.color = backgroundColor

        val width = paint.measureText(text, start, end)

        val rect = RectF(
            /* left = */ x,
            /* top = */ top.toFloat() + verticalOffset,
            /* right = */ x + width + 2 * padding,
            /* bottom = */ bottom.toFloat() - verticalOffset
        )
        canvas.drawRoundRect(rect, radius, radius, paint)

        paint.setGivenTextStyle()
        canvas.drawText(
            /* text = */ text,
            /* start = */ start,
            /* end = */ end,
            /* x = */ x + padding,
            /* y = */ y.toFloat() - verticalOffset,
            /* paint = */ paint
        )
    }

    private fun Paint.setGivenTextStyle() {
        apply {
            color = textColor
            typeface = textTypeface
        }
    }

    override fun chooseHeight(text: CharSequence?, start: Int, end: Int, spanstartv: Int, lineHeight: Int, fm: FontMetricsInt?) {}
}
