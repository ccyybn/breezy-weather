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

package org.breezyweather.main.adapters.main.holder

import android.animation.AnimatorSet
import android.animation.FloatEvaluator
import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.Size
import androidx.core.graphics.ColorUtils
import breezyweather.domain.location.model.Location
import breezyweather.domain.weather.model.MoonPhase
import breezyweather.domain.weather.model.Weather
import org.breezyweather.R
import org.breezyweather.common.basic.GeoActivity
import org.breezyweather.common.extensions.getFormattedTime
import org.breezyweather.common.extensions.is12Hour
import org.breezyweather.common.ui.widgets.astro.MoonPhaseView
import org.breezyweather.common.ui.widgets.astro.SunMoonView
import org.breezyweather.domain.weather.model.getDescription
import org.breezyweather.main.utils.MainThemeColorProvider
import org.breezyweather.theme.ThemeManager
import org.breezyweather.theme.resource.ResourceHelper
import org.breezyweather.theme.resource.providers.ResourceProvider
import org.breezyweather.theme.weatherView.WeatherViewController
import org.shredzone.commons.suncalc.MoonIllumination
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.days

class AstroViewHolder(parent: ViewGroup) : AbstractMainCardViewHolder(
    LayoutInflater
        .from(parent.context)
        .inflate(R.layout.container_main_sun_moon, parent, false)
) {
    private val mTitle: TextView
    private val mPhaseText: TextView
    private val mPhaseView: MoonPhaseView
    private val mSunMoonView: SunMoonView
    private val mSunContainer: RelativeLayout
    private val mSunTxt: TextView
    private val mMoonContainer: RelativeLayout
    private val mMoonTxt: TextView
    private var mWeather: Weather? = null

    @Size(2)
    private var mStartTimes: LongArray = LongArray(2)

    @Size(2)
    private var mEndTimes: LongArray = LongArray(2)

    @Size(2)
    private var mCurrentTimes: LongArray = LongArray(2)

    @Size(2)
    private var mAnimCurrentTimes: LongArray = LongArray(2)
    private var mPhaseAngle = 0.0

    @Size(3)
    private val mAttachAnimatorSets: Array<AnimatorSet?>

    init {
        mTitle = itemView.findViewById(R.id.container_main_sun_moon_title)
        mPhaseText = itemView.findViewById(R.id.container_main_sun_moon_phaseText)
        mPhaseView = itemView.findViewById(R.id.container_main_sun_moon_phaseView)
        mSunMoonView = itemView.findViewById(R.id.container_main_sun_moon_controlView)
        mSunContainer = itemView.findViewById(R.id.container_main_sun_moon_sunContainer)
        mSunTxt = itemView.findViewById(R.id.container_main_sun_moon_sunrise_sunset)
        mMoonContainer = itemView.findViewById(R.id.container_main_sun_moon_moonContainer)
        mMoonTxt = itemView.findViewById(R.id.container_main_sun_moon_moonrise_moonset)
        mAttachAnimatorSets = arrayOf(null, null, null)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindView(
        activity: GeoActivity, location: Location,
        provider: ResourceProvider,
        listAnimationEnabled: Boolean, itemAnimationEnabled: Boolean, firstCard: Boolean
    ) {
        super.onBindView(
            activity, location, provider,
            listAnimationEnabled, itemAnimationEnabled, firstCard
        )
        mWeather = location.weather!!
        val themeColors = ThemeManager.getInstance(context)
            .weatherThemeDelegate
            .getThemeColors(
                context,
                WeatherViewController.getWeatherKind(location),
                WeatherViewController.isDaylight(location)
            )
        mTitle.setTextColor(themeColors[0])
        val talkBackBuilder = StringBuilder(mTitle.text)
        ensureTime(mWeather!!, location.javaTimeZone)
        ensurePhaseAngle()

        mPhaseText.visibility = View.VISIBLE
        mPhaseView.visibility = View.VISIBLE
        mPhaseText.setTextColor(MainThemeColorProvider.getColor(location, R.attr.colorBodyText))
        mPhaseView.setColor(
            MainThemeColorProvider.getColor(location, R.attr.colorMoonLight),
            MainThemeColorProvider.getColor(location, R.attr.colorMoonDark),
            MainThemeColorProvider.getColor(location, R.attr.colorMoonDark)
        )
        mPhaseText.text = MoonPhase(mPhaseAngle).getDescription(context)
        talkBackBuilder.append(context.getString(R.string.comma_separator))
            .append(mPhaseText.text)

        mSunMoonView.setSunDrawable(ResourceHelper.getSunDrawable(provider))
        mSunMoonView.setMoonDrawable(ResourceHelper.getMoonDrawable(provider))
        if (MainThemeColorProvider.isLightTheme(context, location)) {
            mSunMoonView.setColors(
                themeColors[0],
                ColorUtils.setAlphaComponent(themeColors[1], (0.66 * 255).toInt()),
                ColorUtils.setAlphaComponent(themeColors[1], (0.33 * 255).toInt()),
                MainThemeColorProvider.getColor(location, R.attr.colorMainCardBackground),
                true
            )
        } else {
            mSunMoonView.setColors(
                themeColors[2],
                ColorUtils.setAlphaComponent(themeColors[2], (0.5 * 255).toInt()),
                ColorUtils.setAlphaComponent(themeColors[2], (0.2 * 255).toInt()),
                MainThemeColorProvider.getColor(location, R.attr.colorMainCardBackground),
                false
            )
        }
        if (itemAnimationEnabled) {
            mSunMoonView.setTime(mStartTimes, mEndTimes, mStartTimes)
            mSunMoonView.setDayIndicatorRotation(0f)
            mSunMoonView.setNightIndicatorRotation(0f)
            mPhaseView.setSurfaceAngle(0.0)
        } else {
            mSunMoonView.post { mSunMoonView.setTime(mStartTimes, mEndTimes, mCurrentTimes) }
            mSunMoonView.setDayIndicatorRotation(0f)
            mSunMoonView.setNightIndicatorRotation(0f)
            mPhaseView.setSurfaceAngle(mPhaseAngle)
        }

        mWeather?.today?.sun?.let { sun ->
            if (sun.isValid || sun.isPreValid) {
                var sunriseTime: String
                var sunsetTime: String

                if (!sun.isValid || (sun.riseDatePre != null && sun.setDatePre != null && mCurrentTimes[0] < sun.setDatePre!!.time)) {
                    sunriseTime = sun.riseDatePre!!.getFormattedTime(location, context, context.is12Hour)
                    sunsetTime = sun.setDatePre!!.getFormattedTime(location, context, context.is12Hour)
                    if (sun.riseDatePre!! < mWeather?.today?.date) sunriseTime = context.getString(R.string.short_yesterday) + " " + sunriseTime
                    if (sun.setDatePre!!.time >= mWeather?.today?.date?.time!! + 1.days.inWholeMilliseconds) sunsetTime = context.getString(R.string.short_tomorrow) + " " + sunsetTime
                } else {
                    sunriseTime = sun.riseDate!!.getFormattedTime(location, context, context.is12Hour)
                    sunsetTime = sun.setDate!!.getFormattedTime(location, context, context.is12Hour)
                    if (sun.riseDate!! < mWeather?.today?.date) sunriseTime = context.getString(R.string.short_yesterday) + " " + sunriseTime
                    if (sun.setDate!!.time >= mWeather?.today?.date?.time!! + 1.days.inWholeMilliseconds) sunsetTime = context.getString(R.string.short_tomorrow) + " " + sunsetTime
                }
                mSunContainer.visibility = View.VISIBLE
                mSunTxt.text = sunriseTime + "↑" + "\n" + sunsetTime + "↓"
                talkBackBuilder
                    .append(context.getString(R.string.comma_separator))
                    .append(activity.getString(R.string.ephemeris_sunrise_at, sunriseTime))
                    .append(context.getString(R.string.comma_separator))
                    .append(activity.getString(R.string.ephemeris_sunset_at, sunsetTime))
            } else {
                mSunContainer.visibility = View.GONE
            }
        } ?: run {
            mSunContainer.visibility = View.GONE
        }

        mWeather?.today?.moon?.let { moon ->
            if (moon.isValid || moon.isPreValid) {
                var moonriseTime: String
                var moonsetTime: String
                if (!moon.isValid || (moon.riseDatePre != null && moon.setDatePre != null && mCurrentTimes[1] < moon.setDatePre!!.time)) {
                    moonriseTime = moon.riseDatePre!!.getFormattedTime(location, context, context.is12Hour)
                    moonsetTime = moon.setDatePre!!.getFormattedTime(location, context, context.is12Hour)
                    if (moon.riseDatePre!! < mWeather?.today?.date) moonriseTime = context.getString(R.string.short_yesterday) + " " + moonriseTime
                    if (moon.setDatePre!!.time >= mWeather?.today?.date?.time!! + 1.days.inWholeMilliseconds) moonsetTime = context.getString(R.string.short_tomorrow) + " " + moonsetTime
                } else {
                    moonriseTime = moon.riseDate!!.getFormattedTime(location, context, context.is12Hour)
                    moonsetTime = moon.setDate!!.getFormattedTime(location, context, context.is12Hour)
                    if (moon.riseDate!! < mWeather?.today?.date) moonriseTime = context.getString(R.string.short_yesterday) + " " + moonriseTime
                    if (moon.setDate!!.time >= mWeather?.today?.date?.time!! + 1.days.inWholeMilliseconds) moonsetTime = context.getString(R.string.short_tomorrow) + " " + moonsetTime
                }
                mMoonContainer.visibility = View.VISIBLE
                mMoonTxt.text = moonriseTime + "↑" + "\n" + moonsetTime + "↓"
                talkBackBuilder
                    .append(context.getString(R.string.comma_separator))
                    .append(activity.getString(R.string.ephemeris_moonrise_at, moonriseTime))
                    .append(context.getString(R.string.comma_separator))
                    .append(activity.getString(R.string.ephemeris_moonset_at, moonsetTime))
            } else {
                mMoonContainer.visibility = View.GONE
            }
        } ?: run {
            mMoonContainer.visibility = View.GONE
        }
        itemView.contentDescription = talkBackBuilder.toString()
    }

    private class LongEvaluator : TypeEvaluator<Long> {
        override fun evaluate(fraction: Float, startValue: Long, endValue: Long): Long {
            return startValue + ((endValue - startValue) * fraction).toLong()
        }
    }

    @SuppressLint("Recycle")
    override fun onEnterScreen() {
        if (itemAnimationEnabled && mWeather != null) {
            val timeDay = ValueAnimator.ofObject(LongEvaluator(), mStartTimes[0], mCurrentTimes[0])
            timeDay.addUpdateListener { animation: ValueAnimator ->
                mAnimCurrentTimes[0] = animation.animatedValue as Long
                mSunMoonView.setTime(mStartTimes, mEndTimes, mAnimCurrentTimes)
            }
            val totalRotationDay = 360.0 * 5 * (mCurrentTimes[0] - mStartTimes[0]) / (mEndTimes[0] - mStartTimes[0]) + 360.0
            val rotateDay = ValueAnimator.ofObject(
                FloatEvaluator(), 0, (totalRotationDay - totalRotationDay % 360).toInt()
            )
            rotateDay.addUpdateListener { animation: ValueAnimator -> mSunMoonView.setDayIndicatorRotation((animation.animatedValue as Float)) }
            mAttachAnimatorSets[0] = AnimatorSet().apply {
                playTogether(timeDay, rotateDay)
                interpolator = OvershootInterpolator(1f)
                duration = getPathAnimatorDuration(0)
            }.also { it.start() }
            val timeNight = ValueAnimator.ofObject(LongEvaluator(), mStartTimes[1], mCurrentTimes[1])
            timeNight.addUpdateListener { animation: ValueAnimator ->
                mAnimCurrentTimes[1] = animation.animatedValue as Long
                mSunMoonView.setTime(mStartTimes, mEndTimes, mAnimCurrentTimes)
            }
            val arcAngle = 135
            val offsetAngle = 0
            var proportion = (mCurrentTimes[1] - mStartTimes[1]).toFloat() / (mEndTimes[1] - mStartTimes[1]).toFloat()
            if (proportion > 1) proportion = 1.0F
            val round = (360.0 * 15 * proportion / 360.0).toInt() + 1
            val totalRotationNight = arcAngle * proportion + offsetAngle + 360.0 * round
            val rotateNight = ValueAnimator.ofObject(
                FloatEvaluator(), offsetAngle, totalRotationNight
            )
            rotateNight.addUpdateListener { animation: ValueAnimator -> mSunMoonView.setNightIndicatorRotation(animation.animatedValue as Float) }
            mAttachAnimatorSets[1] = AnimatorSet().apply {
                playTogether(timeNight, rotateNight)
                interpolator = OvershootInterpolator(1f)
                duration = getPathAnimatorDuration(1)
            }.also { it.start() }
            if (mPhaseAngle > 0) {
                val moonAngle = ValueAnimator.ofObject(FloatEvaluator(), 0, mPhaseAngle)
                moonAngle.addUpdateListener { animation: ValueAnimator -> mPhaseView.setSurfaceAngle(((animation.animatedValue as Float).toDouble())) }
                mAttachAnimatorSets[2] = AnimatorSet().apply {
                    playTogether(moonAngle)
                    interpolator = DecelerateInterpolator()
                    duration = phaseAnimatorDuration
                }.also { it.start() }
            }
        }
    }

    override fun onRecycleView() {
        super.onRecycleView()
        for (i in mAttachAnimatorSets.indices) {
            mAttachAnimatorSets[i]?.let {
                if (it.isRunning) {
                    it.cancel()
                }
            }
            mAttachAnimatorSets[i] = null
        }
    }

    private fun ensureTime(weather: Weather, timeZone: TimeZone) {
        val calendar = Calendar.getInstance(timeZone)
        val currentTime = calendar.time.time
        mStartTimes = LongArray(2)
        mEndTimes = LongArray(2)
        mCurrentTimes = longArrayOf(currentTime, currentTime)

        // sun.
        if (weather.today?.sun?.riseDate != null && weather.today!!.sun!!.setDate != null) {
            if (weather.today?.sun?.riseDatePre != null && weather.today?.sun?.setDatePre != null && currentTime < weather.today?.sun?.setDatePre!!.time) {
                mStartTimes[0] = weather.today!!.sun!!.riseDatePre!!.time
                mEndTimes[0] = weather.today!!.sun!!.setDatePre!!.time
            } else {
                mStartTimes[0] = weather.today!!.sun!!.riseDate!!.time
                mEndTimes[0] = weather.today!!.sun!!.setDate!!.time
            }
        } else {
            mStartTimes[0] = currentTime + 1
            mEndTimes[0] = currentTime + 1
        }

        // moon.
        if (weather.today?.moon?.riseDate != null && weather.today!!.moon!!.setDate != null) {
            if (weather.today?.moon?.riseDatePre != null && weather.today?.moon?.setDatePre != null && currentTime < weather.today?.moon?.setDatePre!!.time) {
                mStartTimes[1] = weather.today!!.moon!!.riseDatePre!!.time
                mEndTimes[1] = weather.today!!.moon!!.setDatePre!!.time
            } else {
                mStartTimes[1] = weather.today!!.moon!!.riseDate!!.time
                mEndTimes[1] = weather.today!!.moon!!.setDate!!.time
            }
        } else {
            mStartTimes[1] = currentTime + 1
            mEndTimes[1] = currentTime + 1
        }
        if (mCurrentTimes[0] > mEndTimes[0]) {
            mStartTimes[0] = currentTime + 1
            mEndTimes[0] = currentTime + 1
        }
        if (mCurrentTimes[1] > mEndTimes[1]) {
            mStartTimes[1] = currentTime + 1
            mEndTimes[1] = currentTime + 1
        }
        mAnimCurrentTimes = longArrayOf(mCurrentTimes[0], mCurrentTimes[1])
    }

    private fun ensurePhaseAngle() {
        val illumination = MoonIllumination.compute()
            .on(Date())
            .execute()
        mPhaseAngle = illumination.phase + 180
    }

    private fun getPathAnimatorDuration(index: Int): Long {
        val duration = max(
            1000 + 3000.0
                    * (mCurrentTimes[index] - mStartTimes[index])
                    / (mEndTimes[index] - mStartTimes[index]),
            0.0
        ).toLong()
        return min(duration, 4000)
    }

    private val phaseAnimatorDuration: Long
        get() {
            val duration = max(0.0, mPhaseAngle / 360.0 * 1000 + 1000).toLong()
            return min(duration, 2000)
        }
}