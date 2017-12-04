package com.exp.carconnect.basic.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import com.exp.carconnect.basic.R
import java.util.*


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class Dashboard @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = -1) : View(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
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
    private var fuelPercentage = .5f
    private var currentSpeed = 0f
    private var currentRPM = 0f
    private var showCheckEngineLight = false
    private var showIgnitionIcon = false
    private var online = true
    private var vin = "ABCD1234EFGHLOMN2"


    private val onlineColor = getContext().getColor(android.R.color.holo_blue_bright)
    private val offlineColor = getContext().getColor(android.R.color.darker_gray)

    private val vinOnlineColor = context.getColor(android.R.color.holo_orange_dark)
    private val vinOfflineColor = context.getColor(android.R.color.holo_orange_light)
    private val vinPaint: Paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundDrawable = RoundedBitmapDrawableFactory.create(resources,
            (getContext().getDrawable(R.drawable.background2) as BitmapDrawable).bitmap)


    private val speedometerGauge = SpeedometerGauge(getContext(), MIDDLE_GAUGE_START_ANGLE.toFloat(), MIDDLE_GAUGE_SWEEP_ANGLE.toFloat(), onlineColor, offlineColor)
    private val rpmGauge = RPMGauge(getContext(), LEFT_GAUGE_START_ANGLE.toFloat(), LEFT_GAUGE_SWEEP_ANGLE.toFloat(), onlineColor, offlineColor)
    private val fuelAndCompassGauge = FuelAndCompassGauge(getContext(), RIGHT_GAUGE_START_ANGLE.toFloat(), RIGHT_GAUGE_SWEEP_ANGLE.toFloat(), onlineColor, offlineColor)

    init {
        init()
    }

    private fun init() {
        backgroundDrawable.isCircular = true

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

//        background = backgroundDrawable
        setBackgroundColor(context.getColor(android.R.color.black))

    }

    //todo improve this and avoid allocation in onDraw
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()
        val remainingWidthAfterPadding = width - paddingLeft.toFloat() - paddingRight.toFloat()
        val remainingHeihtAfterPadding = height - paddingTop.toFloat() - paddingBottom.toFloat()
        viewCenter = PointF(width / 2, height / 2)

        middleGaugeRadius = remainingWidthAfterPadding * MIDDLE_GAUGE_WIDTH_PERCENTAGE / 2
        sideGaugeRadius = remainingWidthAfterPadding * LEFT_GAUGE_WIDTH_PERCENTAGE / 2

        val middleGaugeBounds = RectF(viewCenter!!.x - middleGaugeRadius, viewCenter!!.y - middleGaugeRadius,
                viewCenter!!.x + middleGaugeRadius, viewCenter!!.y + middleGaugeRadius)

        val leftSideGaugeStartPoint = Math.ceil(middleGaugeBounds.left - sideGaugeRadius * Math.sqrt(2.0)).toInt()
        val leftSideGaugeBounds = RectF(leftSideGaugeStartPoint.toFloat(), viewCenter!!.y - sideGaugeRadius,
                leftSideGaugeStartPoint + 2 * sideGaugeRadius, viewCenter!!.y + sideGaugeRadius)

        val rightSideGaudeEndpoint = Math.ceil(middleGaugeBounds.right + sideGaugeRadius * Math.sqrt(2.0)).toInt()
        val rightSideGaugeBounds = RectF(rightSideGaudeEndpoint - 2 * sideGaugeRadius, viewCenter!!.y - sideGaugeRadius,
                rightSideGaudeEndpoint.toFloat(), viewCenter!!.y + sideGaugeRadius)

        drawGaugeBackgrounds(canvas, middleGaugeBounds, leftSideGaugeBounds, rightSideGaugeBounds)
        drawGauges(canvas, middleGaugeBounds, leftSideGaugeBounds, rightSideGaugeBounds)
        drawVin(canvas, middleGaugeBounds)

    }

    private fun drawGauges(canvas: Canvas, middleGaugeBounds: RectF, leftSideGaugeBounds: RectF, rightSideGaugeBounds: RectF) {
        speedometerGauge.drawGauge(canvas, middleGaugeBounds)
        rpmGauge.drawGauge(canvas, leftSideGaugeBounds)
        fuelAndCompassGauge.drawGauge(canvas, rightSideGaugeBounds)
    }

    private fun drawGaugeBackgrounds(canvas: Canvas, middleGaugeBounds: RectF, leftSideGaugeBounds: RectF, rightSideGaugeBounds: RectF) {
        backgroundDrawable.setBounds(middleGaugeBounds.left.toInt(), middleGaugeBounds.top.toInt(), middleGaugeBounds.right.toInt(), middleGaugeBounds.bottom.toInt())
        backgroundDrawable.draw(canvas)

        backgroundDrawable.setBounds(leftSideGaugeBounds.left.toInt(), leftSideGaugeBounds.top.toInt(), leftSideGaugeBounds.right.toInt(), leftSideGaugeBounds.bottom.toInt())
        backgroundDrawable.draw(canvas)

        backgroundDrawable.setBounds(rightSideGaugeBounds.left.toInt(), rightSideGaugeBounds.top.toInt(), rightSideGaugeBounds.right.toInt(), rightSideGaugeBounds.bottom.toInt())
        backgroundDrawable.draw(canvas)
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


}
