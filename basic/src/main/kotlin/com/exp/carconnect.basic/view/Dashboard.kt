package com.exp.carconnect.basic.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Parcelable
import android.support.annotation.RequiresApi
import android.support.v4.content.res.ResourcesCompat
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

        private val LABEL_TEXT = "Car Connect"
        private val LABEL_REFERENCE_TEXT_SIZE = 50f

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
        private val DASHBOARD_LABEL_MARGIN_PERCENTAGE_OF_AVAILABLE_HEIGHT = 0.05f
        private val RIGHT_GAUGE_WIDTH_PERCENTAGE = LEFT_GAUGE_WIDTH_PERCENTAGE
        private val MINIMUM_WIDTH = 800
        private val MINIMUM_HEIGHT = 400 // fifty percent of height
    }


    private var viewCenter: PointF? = null
    private var middleGaugeRadius: Float = 0.toFloat()
    private var sideGaugeRadius: Float = 0.toFloat()
    var onOnlineChangedListener: ((Boolean) -> Unit)? = null
    var onVINChangedListener: ((String) -> Unit)? = null

    var fuelPercentage = 0.0f
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

    var online = false
        set(value) {
            if (value != field) {
                field = value
                adoptOnlineStatus()
                invalidate()
                if (value) {
                    if (rpmDribbleEnabled) {
                        rpmGauge.dribble()
                    }
                    if (speedDribbleEnabled) {
                        speedometerGauge.dribble()
                    }
                }
                onOnlineChangedListener?.invoke(field)
            }
        }

    var vin = "-------VIN-------"
        set(value) {
            if (value.length == 17 && value != field) {
                field = value
                invalidate()
                onVINChangedListener?.invoke(field)
            }
        }

    var rpmDribbleEnabled = true
        set(value) {
            if (value != field) {
                rpmGauge.rpmDribbleEnabled = value
                field = value
            }
        }

    var speedDribbleEnabled = true
        set(value) {
            if (value != field) {
                speedometerGauge.speedDribbleEnabled = value
                field = value
            }
        }

    var currentAzimuth = 0.0f
        set(value) {
            if (value != field) {
                fuelAndCompassGauge.updateAzimuth(value)
                field = value
            }
        }


    private val onlineColor = getContext().getColor(android.R.color.holo_blue_bright)
    private val offlineColor = getContext().getColor(android.R.color.darker_gray)

    private val vinOnlineColor = context.getColor(android.R.color.holo_orange_dark)
    private val vinOfflineColor = context.getColor(android.R.color.white)
    private val labelColor = context.getColor(android.R.color.holo_blue_dark)
    private val vinPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val labelBound: Rect = Rect()
    private val gaugeBackgroundDrawable = RoundedBitmapDrawableFactory.create(resources,
            (getContext().getDrawable(R.drawable.gauge_bg) as BitmapDrawable).bitmap)


    private val speedometerGauge = SpeedometerGauge(this, MIDDLE_GAUGE_START_ANGLE.toFloat(), MIDDLE_GAUGE_SWEEP_ANGLE.toFloat(),
            currentSpeed, showIgnitionIcon, showCheckEngineLight, speedDribbleEnabled, onlineColor, offlineColor)
    private val rpmGauge = RPMGauge(this, LEFT_GAUGE_START_ANGLE.toFloat(), LEFT_GAUGE_SWEEP_ANGLE.toFloat(),
            rpmDribbleEnabled, currentRPM, onlineColor, offlineColor)
    private val fuelAndCompassGauge = FuelAndCompassGauge(this, RIGHT_GAUGE_START_ANGLE.toFloat(), RIGHT_GAUGE_SWEEP_ANGLE.toFloat(),
            .01f, currentAzimuth, onlineColor, offlineColor)


    init {
        init()
    }

    private fun init() {

        gaugeBackgroundDrawable.isCircular = true
        background = context.getDrawable(R.drawable.dashboard_bg)

        vinPaint.textLocale = Locale.US
        vinPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        vinPaint.textAlign = Paint.Align.CENTER

        labelPaint.textLocale = Locale.US
        labelPaint.typeface = Typeface.create(ResourcesCompat.getFont(context, R.font.atomic_age), Typeface.BOLD)
        labelPaint.color = labelColor
//        val direction = floatArrayOf(0.0f, -1.0f, 0.5f)
//        labelPaint.maskFilter = EmbossMaskFilter(direction, 0.8f, 15f, 1f)
        labelPaint.textSize = LABEL_REFERENCE_TEXT_SIZE
        labelPaint.getTextBounds(LABEL_TEXT, 0, LABEL_TEXT.length, labelBound)

        adoptOnlineStatus()
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

        val availableHeight = (remainingHeihtAfterPadding - (2 * sideGaugeRadius)) / 2
        val margin = availableHeight * DASHBOARD_LABEL_MARGIN_PERCENTAGE_OF_AVAILABLE_HEIGHT
        val labelBoundsOnCanvas = RectF(margin, viewCenter!!.y + sideGaugeRadius + margin,
                labelBound.width().toFloat(), viewCenter!!.y + sideGaugeRadius + availableHeight - margin)
        drawLabel(canvas, labelBoundsOnCanvas)
    }

    private fun drawLabel(canvas: Canvas, labelBoundsOnCanvas: RectF) {
        //change textsize only if we cannot achieve the height we need
        if (labelBoundsOnCanvas.height() < labelBound.height()) {
            labelPaint.textSize = (LABEL_REFERENCE_TEXT_SIZE * labelBoundsOnCanvas.height()) / labelBound.height()
            labelPaint.getTextBounds(LABEL_TEXT, 0, LABEL_TEXT.length, labelBound)
        }
        canvas.drawText(LABEL_TEXT, labelBoundsOnCanvas.left, labelBoundsOnCanvas.bottom, labelPaint)
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

    private fun adoptOnlineStatus() {
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

    override fun onSaveInstanceState(): Parcelable {
        return super.onSaveInstanceState()
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
    }

    fun setOnRPMChangedListener(listener: (Float) -> Unit) {
        rpmGauge.rpmChangedListener = listener
    }

    fun setOnSpeedChangedListener(listener: (Float) -> Unit) {
        speedometerGauge.speedChangedListener = listener
    }

    fun setOnFuelPercentageChangedListener(listener: (Float) -> Unit) {
        fuelAndCompassGauge.fuelPercentageChangedListener = listener
    }

    fun setOnCheckEngineLightChangedListener(listener: (Boolean) -> Unit) {
        speedometerGauge.checkEngineLightChangedListener = listener
    }

    fun setOnIgnitionChangedListener(listener: (Boolean) -> Unit) {
        speedometerGauge.ignitionIconChangedListener = listener
    }

}
