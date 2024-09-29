/**
 * This file is part of Breezy Weather.
 *
 * Breezy Weather is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3 of the License.
 *
 * Breezy Weather is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Breezy Weather. If not, see <https://www.gnu.org/licenses/>.
 */

package org.breezyweather.common.ui.images

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import kotlin.math.min


class SunDrawable : Drawable() {
    private val mPaint = Paint().apply {
        isAntiAlias = true
    }

    @ColorInt
    private val mColor: Int = Color.rgb(255, 184, 62)

    private var mAlpha: Float = 1f
    private var mBounds: Rect
    private var mCX = 0f
    private var mCY = 0f
    private var mCoreRadius = 0f
    private var haloHeight = 0f
    private var haloMargins = 0f
    private var haloRadius = 0f
    private var haloWidth = 0f
    private var haloRectF: RectF = RectF()


    init {
        mBounds = bounds
        ensurePosition(mBounds)
    }

    private fun ensurePosition(bounds: Rect) {
        val min = min(bounds.width(), bounds.height()).toFloat()
        mCoreRadius = (0.4843f * min) / 2.0f
        val width = bounds.width().toDouble()
        val d = bounds.left.toDouble()
        java.lang.Double.isNaN(width)
        java.lang.Double.isNaN(d)
        mCX = (((width * 1.0) / 2.0) + d).toFloat()
        val height = bounds.height().toDouble()
        val d2 = bounds.top.toDouble()
        java.lang.Double.isNaN(height)
        java.lang.Double.isNaN(d2)
        mCY = (((height * 1.0) / 2.0) + d2).toFloat()
        val f = 0.0703f * min
        haloWidth = f
        haloHeight = 0.1367f * min
        haloRadius = f / 2.0f
        haloMargins = min * 0.0898f

    }

    override fun onBoundsChange(bounds: Rect) {
        mBounds = bounds
        ensurePosition(bounds)
    }

    override fun draw(canvas: Canvas) {
        mPaint.alpha = (mAlpha * 255.0f).toInt()
        mPaint.color = mColor
        for (i in 0..3) {
            val save = canvas.save()
            canvas.rotate((i * 45).toFloat(), mCX, mCY)
            val rectF: RectF = haloRectF
            val f = mCX
            val f2 = haloWidth
            val f3 = mCoreRadius
            val f4 = haloHeight
            val f5 = haloMargins
            rectF[f - (f2 / 2.0f), (f - f3) - f4 - f5, f2 / 2.0f + f] = ((f - f3) - f4) - f5 + f4
            val rectF2: RectF = haloRectF
            val f6 = haloRadius
            canvas.drawRoundRect(rectF2, f6, f6, mPaint)
            val rectF3: RectF = haloRectF
            val f7 = mCX
            val f8 = haloWidth
            val f9 = mCoreRadius
            val f10 = haloMargins
            rectF3[f7 - (f8 / 2.0f), f7 + f9 + f10, f8 / 2.0f + f7] = f7 + f9 + f10 + haloHeight
            val rectF4: RectF = haloRectF
            val f11 = haloRadius
            canvas.drawRoundRect(rectF4, f11, f11, mPaint)
            canvas.restoreToCount(save)
        }
        canvas.drawCircle(mCX, mCY, mCoreRadius, mPaint)
    }

    override fun setAlpha(alpha: Int) {
        mAlpha = alpha.toFloat()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        mPaint.setColorFilter(colorFilter)
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }

    override fun getIntrinsicWidth(): Int {
        return mBounds.width()
    }

    override fun getIntrinsicHeight(): Int {
        return mBounds.height()
    }
}
