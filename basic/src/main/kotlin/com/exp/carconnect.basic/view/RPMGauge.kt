package com.exp.carconnect.basic.view

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import java.util.*


internal class RPMGauge(private val context: Context,
                        private val startAngle: Float,
                        private val sweep: Float,
                        onlineColor: Int,
                        offlineColor: Int,
                        private val onlineGradientColor: Int,
                        private val offlineGradientColor:Int) : LeftGauge(onlineColor, offlineColor) {

    companion object {
        private val LEFT_GAUGE_TOTAL_NO_OF_TICKS = 41
        private val LEFT_GAUGE_BIG_TICK_MULTIPLE = 5
        private val LEFT_GAUGE_TICK_MARKER_START = 0
        private val LEFT_GAUGE_TICK_MARKER_DIFF = 1
        private val LEFT_GAUGE_BIG_TICK_LENGTH_PERCENTAGE = .08f
        private val LEFT_GAUGE_BIG_TICK_WIDTH_PERCENTAGE = .009f
        private val LEFT_GAUGE_SMALL_TICK_LENGTH_PERCENTAGE = .06f
        private val LEFT_GAUGE_SMALL_TICK_WIDTH_PERCENTAGE = .007f
        private val LEFT_GAUGE_TICK_MARKER_TEXT_SIZE_PERCENTAGE = .05f
        private val LEFT_GAUGE_TICK_MARKER_MARGIN_PERCENTAGE = .025f
        private val LEFT_GAUGE_INDICATOR_CENTER_PERCENTAGE = .15f
        private val LEFT_GAUGE_INDICATOR_LEGTH_PERCENTAGE = .5f

        private val CRITICAL_ANGLE_SWEEP = 45

        private val LEFT_GAUGE_INDICATOR_CIRCLE_STROKE_WIDTH = 10
        private val LEFT_GAUGE_INDICATOR_STROKE_WIDTH = 25
    }


    private val indicatorCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tickPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tickMarkerPaint: Paint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    private var criticalZoneColor = context.getColor(android.R.color.holo_red_dark)


    init {
        tickMarkerPaint.textLocale = Locale.US
        tickMarkerPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        tickMarkerPaint.textAlign = Paint.Align.CENTER

        indicatorCenterPaint.style = Paint.Style.STROKE
        indicatorCenterPaint.strokeWidth = LEFT_GAUGE_INDICATOR_CIRCLE_STROKE_WIDTH.toFloat()
        indicatorCenterPaint.pathEffect = DashPathEffect(floatArrayOf(100f, 15f), 0f)

        indicatorPaint.style = Paint.Style.STROKE
        indicatorPaint.strokeWidth = LEFT_GAUGE_INDICATOR_STROKE_WIDTH.toFloat()
    }

    override fun onConnected() {
        super.onConnected()
        tickPaint.color = onlineColor
        tickMarkerPaint.color = onlineColor

        indicatorCenterPaint.color = onlineColor
        indicatorCenterPaint.setShadowLayer(20f, 0f, 0f, onlineColor)
        indicatorPaint.color = onlineColor
        indicatorPaint.shader = LinearGradient(0f, 0f, 100f, 100f, onlineColor, onlineGradientColor, Shader.TileMode.MIRROR)
        indicatorPaint.setShadowLayer(20f, 0f, 0f, onlineColor)
    }

    override fun onDisconnected() {
        super.onDisconnected()

        tickPaint.color = offlineColor
        tickMarkerPaint.color = offlineColor


        indicatorCenterPaint.color = offlineColor
        indicatorCenterPaint.setShadowLayer(20f, 0f, 0f, offlineColor)
        indicatorPaint.color = offlineColor
        indicatorPaint.shader = LinearGradient(0f, 0f, 100f, 100f, offlineColor, offlineGradientColor, Shader.TileMode.MIRROR)
        indicatorPaint.setShadowLayer(20f, 0f, 0f, offlineColor)
    }


    override fun drawGauge(canvas: Canvas, bounds: RectF) {
        canvas.drawArc(bounds, startAngle, sweep, false, gaugePaint)


        val normalZoneStartAngle = startAngle
        val normalZoneSweep = sweep - CRITICAL_ANGLE_SWEEP
        val criticalZoneStartAngle = startAngle + normalZoneSweep
        canvas.drawArc(bounds, normalZoneStartAngle.toFloat(), normalZoneSweep.toFloat(), false, gaugePaint)
        val color = gaugePaint.color
        gaugePaint.color = criticalZoneColor
        canvas.drawArc(bounds, criticalZoneStartAngle.toFloat(), CRITICAL_ANGLE_SWEEP.toFloat(), false, gaugePaint)
        gaugePaint.color = color


        val gaugeCircumference = (2.0 * Math.PI * (bounds.width() / 2).toDouble()).toFloat()

        //draw ticks
        val bigTickLength = bounds.width() * LEFT_GAUGE_BIG_TICK_LENGTH_PERCENTAGE
        val bigTickWidth = gaugeCircumference * LEFT_GAUGE_BIG_TICK_WIDTH_PERCENTAGE

        val smallTickLength = bounds.width() * LEFT_GAUGE_SMALL_TICK_LENGTH_PERCENTAGE
        val smallTickWidth = gaugeCircumference * LEFT_GAUGE_SMALL_TICK_WIDTH_PERCENTAGE

        val textSize = bounds.width() * LEFT_GAUGE_TICK_MARKER_TEXT_SIZE_PERCENTAGE
        val testSizeMargin = bounds.width() * LEFT_GAUGE_TICK_MARKER_MARGIN_PERCENTAGE

        drawTicks(canvas, bounds, startAngle,
                sweep / (LEFT_GAUGE_TOTAL_NO_OF_TICKS - 1),
                LEFT_GAUGE_TOTAL_NO_OF_TICKS, LEFT_GAUGE_BIG_TICK_MULTIPLE,
                bigTickLength, bigTickWidth, smallTickLength, smallTickWidth,
                LEFT_GAUGE_TICK_MARKER_START, LEFT_GAUGE_TICK_MARKER_DIFF,
                textSize, testSizeMargin, tickMarkerPaint, tickPaint)


        val indicatorCenterRadius = bounds.width() * LEFT_GAUGE_INDICATOR_CENTER_PERCENTAGE / 2
        canvas.drawCircle(bounds.centerX(), bounds.centerY(), indicatorCenterRadius, indicatorCenterPaint)

        canvas.save()
        canvas.rotate(getDegreesForCurrentRPM(), bounds.centerX(), bounds.centerY())

        val indicatorLength = bounds.width() * LEFT_GAUGE_INDICATOR_LEGTH_PERCENTAGE
        val x1 = (bounds.centerX() - indicatorLength * .4).toFloat()
        val y1 = bounds.centerY()
        val x2 = (bounds.centerX() + indicatorLength * .6).toFloat()
        val y2 = bounds.centerY()

        canvas.drawLine(x1, y1, x2, y2, indicatorPaint)

        canvas.restore()
    }


    private fun getDegreesForCurrentRPM(): Float {
        return startAngle
    }


}