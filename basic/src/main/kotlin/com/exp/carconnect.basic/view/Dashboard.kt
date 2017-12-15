package com.exp.carconnect.basic.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Parcelable
import android.support.annotation.RequiresApi
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import com.exp.carconnect.basic.R
import com.exp.carconnect.basic.view.RPMGauge.Companion.MAX_RPM
import com.exp.carconnect.basic.view.RPMGauge.Companion.MIN_RPM
import com.exp.carconnect.basic.view.SpeedometerGauge.Companion.MAX_SPEED
import com.exp.carconnect.basic.view.SpeedometerGauge.Companion.MIN_SPEED
import java.util.*


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class Dashboard @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = -1) : View(context, attrs, defStyleAttr, defStyleRes) {

    companion object {

        private val FUEL_PERCENTAGE_KEY = "fuel_percentage"
        private val CURRENT_SPEED_KEY = "current_speed"
        private val SHOW_CHECK_ENGINE_LIGHT_KEY = "check_engine_light"
        private val SHOW_IGNITION_ICON_KEY = "show_ignition_icon"
        private val CURRENT_RPM_KEY = "current_rpm"
        private val ONLINE_KEY = "online"
        private val VIN_KEY = "vin"

        private val MIDDLE_GAUGE_START_ANGLE = 135
        private val MIDDLE_GAUGE_SWEEP_ANGLE = 270
        private val LEFT_GAUGE_START_ANGLE = 55
        private val LEFT_GAUGE_SWEEP_ANGLE = 250
        private val RIGHT_GAUGE_START_ANGLE = 235
        private val RIGHT_GAUGE_SWEEP_ANGLE = 250
        private val VIN_START_ANGLE = 130f
        private val VIN_SWEEP = 85f

        private val VIN_LENGTH = 17

        private val MIDDLE_GAUGE_WIDTH_PERCENTAGE = 0.4f
        private val LEFT_GAUGE_WIDTH_PERCENTAGE = 0.3f
        private val RIGHT_GAUGE_WIDTH_PERCENTAGE = LEFT_GAUGE_WIDTH_PERCENTAGE
        private val MINIMUM_WIDTH = 800
        private val MINIMUM_HEIGHT = 400 // fifty percent of height
    }


    private var viewCenter: PointF? = null
    private var middleGaugeRadius: Float = 0.toFloat()
    private var sideGaugeRadius: Float = 0.toFloat()


    var fuelPercentage = .5f
        set(value) {
            if (value in 0.0..1.0 && value != field) {
                fuelAndCompassGauge.updateFuel(value)
                field = value
            }
        }

    var currentSpeed = 0f
        set(value) {
            if (value in MIN_SPEED..MAX_SPEED && value != field) {
                speedometerGauge.updateSpeed(value)
                field = value
            }
        }

    var showCheckEngineLight = false
        set(value) {
            if (value != field) {
                speedometerGauge.showCheckEngineLight = value
                field = value
            }
        }

    var showIgnitionIcon = false
        set(value) {
            if (value != field) {
                speedometerGauge.showIgnitionIcon = value
                field = value
            }
        }

    var currentRPM = 0.0f
        set(value) {
            if (value in MIN_RPM..MAX_RPM && value != field) {
                rpmGauge.updateRPM(value)
                field = value
            }
        }

    var online = true
        set(value) {
            if (value != field) {
                field = value
                invalidate()
            }
        }

    var vin = "-------VIN-------"
        set(value) {
            if (value.length == 17 && value != field) {
                field = value
                invalidate()
            }
        }


    private val onlineColor = getContext().getColor(android.R.color.holo_blue_bright)
    private val offlineColor = getContext().getColor(android.R.color.darker_gray)

    private val vinOnlineColor = context.getColor(android.R.color.holo_orange_dark)
    private val vinOfflineColor = context.getColor(android.R.color.holo_orange_light)
    private val vinPaint: Paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val gaugeBackgroundDrawable = RoundedBitmapDrawableFactory.create(resources,
            (getContext().getDrawable(R.drawable.gauge_bg) as BitmapDrawable).bitmap)


    private val speedometerGauge = SpeedometerGauge(getContext(), MIDDLE_GAUGE_START_ANGLE.toFloat(), MIDDLE_GAUGE_SWEEP_ANGLE.toFloat(), this, onlineColor, offlineColor)
    private val rpmGauge = RPMGauge(getContext(), LEFT_GAUGE_START_ANGLE.toFloat(), LEFT_GAUGE_SWEEP_ANGLE.toFloat(), this, onlineColor, offlineColor)
    private val fuelAndCompassGauge = FuelAndCompassGauge(getContext(), RIGHT_GAUGE_START_ANGLE.toFloat(), RIGHT_GAUGE_SWEEP_ANGLE.toFloat(), this, onlineColor, offlineColor)

    init {
        init()
    }

    private fun init() {
        gaugeBackgroundDrawable.isCircular = true
        background = context.getDrawable(R.drawable.dashboard_bg)

        vinPaint.textLocale = Locale.US
        vinPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        vinPaint.textAlign = Paint.Align.CENTER

        if (online) {
            vinPaint.color = vinOnlineColor
            speedometerGauge.onConnected()
            rpmGauge.onConnected()
            fuelAndCompassGauge.onConnected()

        } else {
            vinPaint.color = vinOfflineColor
            speedometerGauge.onDisconnected()
            rpmGauge.onDisconnected()
            fuelAndCompassGauge.onDisconnected()

        }

    }

    //todo improve this and avoid allocation in onDraw
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()
        val remainingWidthAfterPadding = width - paddingLeft.toFloat() - paddingRight.toFloat()
        val remainingHeihtAfterPadding = height - paddingTop.toFloat() - paddingBottom.toFloat()
        //todo view's height should be at least 45% of width, which is middle gauge diameter requirement or I should consider height into consideration before finalizing gauges diameter
        viewCenter = PointF(width / 2, height / 2)

        middleGaugeRadius = remainingWidthAfterPadding * MIDDLE_GAUGE_WIDTH_PERCENTAGE / 2
        sideGaugeRadius = remainingWidthAfterPadding * LEFT_GAUGE_WIDTH_PERCENTAGE / 2

        val middleGaugeBounds = RectF(viewCenter!!.x - middleGaugeRadius, viewCenter!!.y - middleGaugeRadius,
                viewCenter!!.x + middleGaugeRadius, viewCenter!!.y + middleGaugeRadius)

        val leftGaugeStartPoint = Math.ceil(middleGaugeBounds.left - sideGaugeRadius * Math.sqrt(2.0)).toInt()
        val leftGaugeBounds = RectF(leftGaugeStartPoint.toFloat(), viewCenter!!.y - sideGaugeRadius,
                leftGaugeStartPoint + 2 * sideGaugeRadius, viewCenter!!.y + sideGaugeRadius)

        val rightGaugeEndpoint = Math.ceil(middleGaugeBounds.right + sideGaugeRadius * Math.sqrt(2.0)).toInt()
        val rightGaugeBounds = RectF(rightGaugeEndpoint - 2 * sideGaugeRadius, viewCenter!!.y - sideGaugeRadius,
                rightGaugeEndpoint.toFloat(), viewCenter!!.y + sideGaugeRadius)

        drawGaugeBackgrounds(canvas, middleGaugeBounds, leftGaugeBounds, rightGaugeBounds)
        drawGauges(canvas, middleGaugeBounds, leftGaugeBounds, rightGaugeBounds)
        drawVin(canvas, middleGaugeBounds)

    }

    private fun drawGauges(canvas: Canvas, middleGaugeBounds: RectF, leftSideGaugeBounds: RectF, rightSideGaugeBounds: RectF) {
        speedometerGauge.drawGauge(canvas, middleGaugeBounds)
        rpmGauge.drawGauge(canvas, leftSideGaugeBounds)
        fuelAndCompassGauge.drawGauge(canvas, rightSideGaugeBounds)
    }

    private fun drawGaugeBackgrounds(canvas: Canvas, middleGaugeBounds: RectF, leftSideGaugeBounds: RectF, rightSideGaugeBounds: RectF) {
        gaugeBackgroundDrawable.setBounds(leftSideGaugeBounds.left.toInt(), leftSideGaugeBounds.top.toInt(), leftSideGaugeBounds.right.toInt(), leftSideGaugeBounds.bottom.toInt())
        gaugeBackgroundDrawable.draw(canvas)

        gaugeBackgroundDrawable.setBounds(rightSideGaugeBounds.left.toInt(), rightSideGaugeBounds.top.toInt(), rightSideGaugeBounds.right.toInt(), rightSideGaugeBounds.bottom.toInt())
        gaugeBackgroundDrawable.draw(canvas)

        //drawing middle gauge bg at end, otherwise left and right gauge bgs overwrite the middle gauge bg a little bit
        gaugeBackgroundDrawable.setBounds(middleGaugeBounds.left.toInt(), middleGaugeBounds.top.toInt(), middleGaugeBounds.right.toInt(), middleGaugeBounds.bottom.toInt())
        gaugeBackgroundDrawable.draw(canvas)
    }

    private fun drawVin(canvas: Canvas, middleGaugeBounds: RectF) {
        if (!vin.isEmpty()) {
            val circumference: Float = (Math.PI * middleGaugeBounds.width()).toFloat()
            val areaToDrawText = circumference * (VIN_SWEEP / 360f)

            val vinCharSize = areaToDrawText / VIN_LENGTH
            vinPaint.textSize = vinCharSize
            val gapInDegrees = VIN_SWEEP / VIN_LENGTH
            var totalRotationSoFar = VIN_START_ANGLE
            canvas.save()
            canvas.rotate(VIN_START_ANGLE, middleGaugeBounds.centerX(), middleGaugeBounds.centerY())
            val x: Float = middleGaugeBounds.centerX() + (middleGaugeBounds.width() / 2)
            val y: Float = middleGaugeBounds.centerY()
            for (i in 0 until 17) {
                canvas.save()
                canvas.rotate(-totalRotationSoFar, x, y)
                canvas.drawText(vin.substring(i, i + 1), x, y, vinPaint)
                canvas.restore()

                canvas.rotate(-gapInDegrees, middleGaugeBounds.centerX(), middleGaugeBounds.centerY())
                totalRotationSoFar -= gapInDegrees
            }

            canvas.restore()
        }

    }


    override fun onSaveInstanceState(): Parcelable {
        return super.onSaveInstanceState()
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
    }

}
