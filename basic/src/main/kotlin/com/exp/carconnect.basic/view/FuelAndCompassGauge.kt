package com.exp.carconnect.basic.view

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.*
import android.support.graphics.drawable.VectorDrawableCompat
import android.text.TextPaint
import com.exp.carconnect.basic.R
import java.util.*


internal class FuelAndCompassGauge(dashboard: Dashboard,
                                   private val startAngle: Float,
                                   private val sweep: Float,
                                   currentFuelPercentage: Float,
                                   currentAzimuth: Float,
                                   onlineColor: Int,
                                   offlineColor: Int) : RightGauge(dashboard, onlineColor, offlineColor) {
    companion object {
        private val COMPASS_STRIKE_WIDTH = 10f
        private val FUEL_GAUGE_START_ANGLE = 70
        private val FUEL_GAUGE_SWEEP_ANGLE = -140
        private val FUEL_GAUGE_BOUNDARY_STROKE_WIDTH = 10
        private val FUEL_GAUGE_DEFAULT_WIDTH_PERCENTAGE = .15f
        private val COMPASS_TEXT_SIZE_PERCENTAGE = .40f
        private val COMPASS_TEXT_MARGIN_FROM_GAUGE = 10f
        private val COMPASS_GAUGE_WIDTH_PERCENTAGE = .30f
    }

    private val fuelGaugePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fuelGaugeBoundary = Paint(Paint.ANTI_ALIAS_FLAG)
    private val compassPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val compassTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)


    private val fuelIcon = VectorDrawableCompat.create(dashboard.context.resources, R.drawable.ic_local_gas_station_black_24dp, null)!!
    private val compassIcon = VectorDrawableCompat.create(dashboard.context.resources, R.drawable.ic_compass, null)!!

    internal var fuelPercentageChangedListener: ((Float) -> Unit)? = null
    private var currentFuelPercentage = currentFuelPercentage
        set(value) {
            field = if (value == 0.0f) {
                .01f
            } else {
                value
            }
            dashboard.invalidate()
            fuelPercentageChangedListener?.invoke(field)
        }

    internal var currentAzimuth = currentAzimuth
        set(value) {
            field = value
            dashboard.invalidate()
        }

    private var currentFuelAnimator: ObjectAnimator? = null
    private var currentAzimuthAnimator: ObjectAnimator? = null

    init {
        compassPaint.style = Paint.Style.STROKE
        compassPaint.strokeWidth = COMPASS_STRIKE_WIDTH

        compassTextPaint.textLocale = Locale.US
        compassTextPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        compassTextPaint.textAlign = Paint.Align.CENTER


        fuelGaugePaint.style = Paint.Style.STROKE
        fuelGaugePaint.pathEffect = DashPathEffect(floatArrayOf(40f, 15f), 0f)


        fuelGaugeBoundary.style = Paint.Style.STROKE
        fuelGaugeBoundary.strokeWidth = FUEL_GAUGE_BOUNDARY_STROKE_WIDTH.toFloat()

        compassIcon.setTint(onlineColor)

    }

    override fun onConnected() {
        super.onConnected()

        fuelGaugeBoundary.color = onlineColor
        fuelGaugePaint.color = onlineColor
        compassTextPaint.color = onlineColor
        compassPaint.color = onlineColor
        fuelIcon.setTint(onlineColor)

    }

    override fun onDisconnected() {
        super.onDisconnected()

        fuelGaugeBoundary.color = offlineColor
        fuelGaugePaint.color = offlineColor
        compassTextPaint.color = offlineColor
        compassPaint.color = offlineColor
        fuelIcon.setTint(offlineColor)
    }

    override fun drawGauge(canvas: Canvas, bounds: RectF) {
        canvas.drawArc(bounds, startAngle, sweep, false, gaugePaint)
        val boundsAfterStrokeWidth = RectF(bounds)
        boundsAfterStrokeWidth.inset(GAUGE_STROKE_WIDTH / 2, GAUGE_STROKE_WIDTH / 2)
        drawFuelGauge(canvas, boundsAfterStrokeWidth)
//        drawCompass(canvas, boundsAfterStrokeWidth);
        drawCompassDrawable(canvas, boundsAfterStrokeWidth)
    }


    private fun drawFuelGauge(canvas: Canvas, bounds: RectF) {
        val fuelGaugeWidth = bounds.width() * FUEL_GAUGE_DEFAULT_WIDTH_PERCENTAGE


        val fuelGaugeInnerBound = RectF(bounds)
        fuelGaugeInnerBound.inset(fuelGaugeWidth + FUEL_GAUGE_BOUNDARY_STROKE_WIDTH / 2, fuelGaugeWidth + FUEL_GAUGE_BOUNDARY_STROKE_WIDTH / 2)
        canvas.drawArc(fuelGaugeInnerBound, FUEL_GAUGE_START_ANGLE.toFloat(), FUEL_GAUGE_SWEEP_ANGLE.toFloat(), false, fuelGaugeBoundary)

        val fuelGaugeOuterBound = RectF(bounds)
        fuelGaugePaint.strokeWidth = fuelGaugeWidth
        fuelGaugeOuterBound.inset(fuelGaugeWidth / 2, fuelGaugeWidth / 2)

        val sweepAngle = Math.abs(FUEL_GAUGE_SWEEP_ANGLE) * currentFuelPercentage
        canvas.drawArc(fuelGaugeOuterBound, FUEL_GAUGE_START_ANGLE.toFloat(), -sweepAngle, false, fuelGaugePaint)


        val fuelIndicatorDimen = fuelGaugeWidth * .8f
        val fuelIndicatorMarginFromGauge = fuelGaugeWidth * .2f
        val rect = Rect((bounds.centerX() - fuelIndicatorDimen / 2).toInt(),
                (bounds.bottom - (fuelIndicatorDimen + fuelIndicatorMarginFromGauge)).toInt(),
                (bounds.centerX() + fuelIndicatorDimen / 2).toInt(),
                (bounds.bottom - fuelIndicatorMarginFromGauge).toInt())
        fuelIcon.bounds = rect
        fuelIcon.draw(canvas)

    }

    private fun drawCompass(canvas: Canvas, bounds: RectF) {
        val compassRadius = bounds.width() * COMPASS_GAUGE_WIDTH_PERCENTAGE / 2
        val compassBounds = RectF(bounds.centerX() - compassRadius, bounds.centerY() - compassRadius,
                bounds.centerX() + compassRadius, bounds.centerY() + compassRadius)
        canvas.drawCircle(bounds.centerX(), bounds.centerY(), compassRadius, compassPaint)

        val compassTextSize = compassRadius * COMPASS_TEXT_SIZE_PERCENTAGE
        compassTextPaint.textSize = compassTextSize
        val strokeAndMarginFactor = COMPASS_STRIKE_WIDTH / 2 + COMPASS_TEXT_MARGIN_FROM_GAUGE / 2
        val compassBoundAfterStrokeAndMargin = RectF(compassBounds)
        compassBoundAfterStrokeAndMargin.inset(strokeAndMarginFactor, strokeAndMarginFactor)

        val eastDirectionFromOrigin = 35f
        canvas.save()
        canvas.rotate(eastDirectionFromOrigin, compassBoundAfterStrokeAndMargin.centerX(), compassBoundAfterStrokeAndMargin.centerY())
        compassTextPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("E", compassBoundAfterStrokeAndMargin.right, compassBoundAfterStrokeAndMargin.centerY() + compassTextSize / 2, compassTextPaint)
        compassTextPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("W", compassBoundAfterStrokeAndMargin.left, compassBoundAfterStrokeAndMargin.centerY() + compassTextSize / 2, compassTextPaint)

        compassTextPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("S", compassBoundAfterStrokeAndMargin.centerX(), compassBoundAfterStrokeAndMargin.bottom, compassTextPaint)
        canvas.drawText("N", compassBoundAfterStrokeAndMargin.centerX(), compassBoundAfterStrokeAndMargin.top + compassTextSize, compassTextPaint)
        canvas.restore()
    }

    private fun drawCompassDrawable(canvas: Canvas, bounds: RectF) {
        val compassRadius = bounds.width() * COMPASS_GAUGE_WIDTH_PERCENTAGE / 2
        val compassBounds = Rect((bounds.centerX() - compassRadius).toInt(), (bounds.centerY() - compassRadius).toInt(),
                (bounds.centerX() + compassRadius).toInt(), (bounds.centerY() + compassRadius).toInt())
        compassIcon.bounds = compassBounds
        canvas.save()
        canvas.rotate(-currentAzimuth, bounds.centerX(), bounds.centerY())
        compassIcon.draw(canvas)
        canvas.restore()

    }

    @SuppressLint("ObjectAnimatorBinding")
    internal fun updateFuel(fuel: Float) {
        currentFuelAnimator?.cancel()
        var fuelVar = fuel
        if (fuelVar <= 0f) {
            fuelVar = .01f
        }
        currentFuelAnimator = ObjectAnimator.ofFloat(this, "currentFuelPercentage", currentFuelPercentage, fuelVar)
        currentFuelAnimator?.start()
    }

}