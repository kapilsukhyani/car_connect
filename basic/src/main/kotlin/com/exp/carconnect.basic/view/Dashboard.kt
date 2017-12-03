package com.exp.carconnect.basic.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.RectF
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.AttributeSet
import android.view.View


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class Dashboard @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = -1) : View(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private val MIDDLE_GAUGE_START_ANGLE = 135
        private val MIDDLE_GAUGE_SWEEP_ANGLE = 270
        private val LEFT_GAUGE_START_ANGLE = 55
        private val LEFT_GAUGE_SWEEP_ANGLE = 250
        private val RIGHT_GAUGE_START_ANGLE = 235
        private val RIGHT_GAUGE_SWEEP_ANGLE = 250
        private val MIDDLE_GAUGE_WIDTH_PERCENTAGE = 0.4f
        private val LEFT_GAUGE_WIDTH_PERCENTAGE = 0.3f
        private val RIGHT_GAUGE_WIDTH_PERCENTAGE = LEFT_GAUGE_WIDTH_PERCENTAGE
        private val MINIMUM_WIDTH = 800
        private val MINIMUM_HEIGHT = 400 // fifty percent of height
    }


    private var viewCenter: PointF? = null
    private var middleGaugeRadius: Float = 0.toFloat()
    private var sideGaugeRadius: Float = 0.toFloat()
    private val fuelPercentage = .5f
    private val currentSpeed = 0f
    private val currentRPM = 0f
    private val showCheckEngineLight = false
    private val showIgnitionIcon = false
    private val online = true
    private val onlineColor = getContext().getColor(android.R.color.holo_blue_bright)
    private val onlineGradientColor = getContext().getColor(android.R.color.holo_blue_dark)

    private val offlineColor = getContext().getColor(android.R.color.darker_gray)
    private val offlineGradientColor = getContext().getColor(android.R.color.black)


    private val speedometerGauge = SpeedometerGauge(getContext(), MIDDLE_GAUGE_START_ANGLE.toFloat(), MIDDLE_GAUGE_SWEEP_ANGLE.toFloat(), onlineColor, offlineColor)
    private val rpmGauge = RPMGauge(getContext(), LEFT_GAUGE_START_ANGLE.toFloat(), LEFT_GAUGE_SWEEP_ANGLE.toFloat(), onlineColor, offlineColor, onlineGradientColor, offlineGradientColor)
    private val fuelAndCompassGauge = FuelAndCompassGauge(getContext(), RIGHT_GAUGE_START_ANGLE.toFloat(), RIGHT_GAUGE_SWEEP_ANGLE.toFloat(), onlineColor, offlineColor)

    init {
        init()
    }

    private fun init() {
        if (online) {
            speedometerGauge.onConnected()
            rpmGauge.onConnected()
            fuelAndCompassGauge.onConnected()

        } else {
            speedometerGauge.onDisconnected()
            rpmGauge.onDisconnected()
            fuelAndCompassGauge.onDisconnected()

        }

        setBackgroundColor(context.resources.getColor(android.R.color.black))

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

        speedometerGauge.drawGauge(canvas, middleGaugeBounds)
        rpmGauge.drawGauge(canvas, leftSideGaugeBounds)
        fuelAndCompassGauge.drawGauge(canvas, rightSideGaugeBounds)

    }


}
