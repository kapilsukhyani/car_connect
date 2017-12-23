package com.exp.carconnect.basic.view

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.*
import android.text.TextPaint
import java.util.*


internal class RPMGauge(dashboard: Dashboard,
                        private val startAngle: Float,
                        private val sweep: Float,
                        rpmDribbleEnabled: Boolean,
                        currentRPM: Float,
                        onlineColor: Int,
                        offlineColor: Int) : LeftGauge(dashboard, onlineColor, offlineColor) {

    companion object {

        internal val MIN_RPM = 0
        internal val MAX_RPM = 8

        internal val TOTAL_NO_OF_DATA_POINTS = MAX_RPM

        private val RPM_TEXT = "RPM x 1000"
        private val TOTAL_NO_OF_TICKS = 25
        private val BIG_TICK_MULTIPLE = 3
        private val TICK_MARKER_START = 0
        private val TICK_MARKER_DIFF = 1
        private val BIG_TICK_LENGTH_PERCENTAGE = .08f
        private val BIG_TICK_WIDTH_PERCENTAGE = .009f
        private val SMALL_TICK_LENGTH_PERCENTAGE = .06f
        private val SMALL_TICK_WIDTH_PERCENTAGE = .007f
        private val TICK_MARKER_TEXT_SIZE_PERCENTAGE = .06f
        private val TICK_MARKER_MARGIN_PERCENTAGE = .03f
        private val INDICATOR_CENTER_PERCENTAGE = .15f
        private val INDICATOR_LENGTH_PERCENTAGE = .5f
        private val RPM_TEXT_SIZE_PERCENTAGE = .06f
        private val RPM_TEXT_MARGIN = .04f

        private val CRITICAL_ANGLE_SWEEP = 45f

        private val INDICATOR_CIRCLE_STROKE_WIDTH = 10
        private val INDICATOR_STROKE_WIDTH = 25

        private val DRIBBLE_RANGE = .1f
        private val DRIBBLE_RANDOM = Random()
    }


    private val indicatorCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorThinLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tickPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tickMarkerPaint: Paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val rpmTextPaint: Paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val onlineGradientColor: Int = getDarkerColor(onlineColor)
    private val offlineGradientColor: Int = getDarkerColor(offlineColor)
    private var criticalZoneColor = dashboard.context.getColor(android.R.color.holo_red_dark)

    internal var rpmDribbleEnabled: Boolean = rpmDribbleEnabled
        set(value) {
            field = value
            if (value) {
                dribble()
            } else {
                dribbleRPMAnimator?.cancel()
                dribbleRPMAnimator = null
            }
        }


    private var currentRPM = currentRPM
        set(value) {
            field = value
            dashboard.invalidate()
            rpmChangedListener?.invoke(field)
        }


    private var currentRPMAnimator: ObjectAnimator? = null
    private var dribbleRPMAnimator: ObjectAnimator? = null
    internal var rpmChangedListener: ((Float) -> Unit)? = null


    private val degreesPerDataPoint = sweep / TOTAL_NO_OF_DATA_POINTS


    init {

        tickMarkerPaint.textLocale = Locale.US
        tickMarkerPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        tickMarkerPaint.textAlign = Paint.Align.CENTER

        rpmTextPaint.textLocale = Locale.US
        rpmTextPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        rpmTextPaint.textAlign = Paint.Align.CENTER

        indicatorCenterPaint.style = Paint.Style.STROKE
        indicatorCenterPaint.strokeWidth = INDICATOR_CIRCLE_STROKE_WIDTH.toFloat()
        indicatorCenterPaint.pathEffect = DashPathEffect(floatArrayOf(30f, 15f), 0f)

        indicatorPaint.style = Paint.Style.STROKE
        indicatorPaint.strokeWidth = INDICATOR_STROKE_WIDTH.toFloat()

        indicatorThinLinePaint.style = Paint.Style.STROKE
        indicatorThinLinePaint.strokeWidth = INDICATOR_STROKE_WIDTH.toFloat() * .2f
        indicatorThinLinePaint.color = criticalZoneColor
        indicatorThinLinePaint.shader = LinearGradient(0f, 0f, 100f, 100f, criticalZoneColor, getDarkerColor(criticalZoneColor), Shader.TileMode.MIRROR)
    }

    override fun onConnected() {
        super.onConnected()
        tickPaint.color = onlineColor
        tickMarkerPaint.color = onlineColor

        rpmTextPaint.color = onlineColor

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

        rpmTextPaint.color = offlineColor

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
        canvas.drawArc(bounds, normalZoneStartAngle, normalZoneSweep, false, gaugePaint)
        val color = gaugePaint.color
        gaugePaint.color = criticalZoneColor
        canvas.drawArc(bounds, criticalZoneStartAngle, CRITICAL_ANGLE_SWEEP, false, gaugePaint)
        gaugePaint.color = color


        val gaugeCircumference = (2.0 * Math.PI * (bounds.width() / 2).toDouble()).toFloat()

        //draw ticks
        val bigTickLength = bounds.width() * BIG_TICK_LENGTH_PERCENTAGE
        val bigTickWidth = gaugeCircumference * BIG_TICK_WIDTH_PERCENTAGE

        val smallTickLength = bounds.width() * SMALL_TICK_LENGTH_PERCENTAGE
        val smallTickWidth = gaugeCircumference * SMALL_TICK_WIDTH_PERCENTAGE

        val textSize = bounds.width() * TICK_MARKER_TEXT_SIZE_PERCENTAGE
        val textMargin = bounds.width() * TICK_MARKER_MARGIN_PERCENTAGE

        drawTicks(canvas, bounds, startAngle,
                sweep / (TOTAL_NO_OF_TICKS - 1),
                TOTAL_NO_OF_TICKS, BIG_TICK_MULTIPLE,
                bigTickLength, bigTickWidth, smallTickLength, smallTickWidth,
                TICK_MARKER_START, TICK_MARKER_DIFF,
                textSize, textMargin, tickMarkerPaint, tickPaint,
                true, CRITICAL_ANGLE_SWEEP, criticalZoneColor)


        drawIndicator(canvas, bounds)


    }

    private fun drawIndicator(canvas: Canvas, parentBounds: RectF) {
        val indicatorCenterRadius = parentBounds.width() * INDICATOR_CENTER_PERCENTAGE / 2
        canvas.drawCircle(parentBounds.centerX(), parentBounds.centerY(), indicatorCenterRadius, indicatorCenterPaint)

        val rpmTextSize = parentBounds.width() * RPM_TEXT_SIZE_PERCENTAGE
        val rpmTextTopMargin = parentBounds.width() * RPM_TEXT_MARGIN
        rpmTextPaint.textSize = rpmTextSize
        val textSize = Rect()
        rpmTextPaint.getTextBounds(RPM_TEXT, 0, RPM_TEXT.length, textSize)
        canvas.drawText(RPM_TEXT, parentBounds.centerX(), parentBounds.centerY() + indicatorCenterRadius + textSize.height() + rpmTextTopMargin, rpmTextPaint)

        canvas.save()
        canvas.rotate(getDegreesForCurrentRPM(), parentBounds.centerX(), parentBounds.centerY())

        val indicatorLength = parentBounds.width() * INDICATOR_LENGTH_PERCENTAGE
        val x1 = (parentBounds.centerX() - indicatorLength * .35).toFloat()
        val y1 = parentBounds.centerY()
        val x2 = (parentBounds.centerX() + indicatorLength * .65).toFloat()
        val y2 = parentBounds.centerY()

        canvas.drawLine(x1, y1, x2, y2, indicatorPaint)
        canvas.drawLine(x1, y1, x2, y2, indicatorThinLinePaint)

        canvas.restore()
    }


    internal fun dribble() {
        if (rpmDribbleEnabled && dashboard.currentRPM > MIN_RPM && dashboard.online) {
            val dribbleBy = DRIBBLE_RANDOM.nextFloat() * (DRIBBLE_RANGE - 0.0f) + 0.0f
            dribbleRPMAnimator = ObjectAnimator.ofFloat(this, "currentRPM", currentRPM, dashboard.currentRPM + dribbleBy,
                    dashboard.currentRPM, dashboard.currentRPM - dribbleBy, currentRPM)
            dribbleRPMAnimator?.addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {
                }

                override fun onAnimationEnd(animation: Animator?) {
                    dribble()
                }

                override fun onAnimationCancel(animation: Animator?) {
                }

                override fun onAnimationStart(animation: Animator?) {
                }
            })
            dribbleRPMAnimator?.start()
        }
    }

    @SuppressLint("ObjectAnimatorBinding")
    internal fun updateRPM(rpm: Float) {
        rpmDribbleEnabled = false
        currentRPMAnimator?.cancel()
        currentRPMAnimator = ObjectAnimator.ofFloat(this, "currentRPM", currentRPM, rpm)
        currentRPMAnimator?.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
                if (dashboard.rpmDribbleEnabled) {
                    rpmDribbleEnabled = true
                }
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationStart(animation: Animator?) {
            }
        })
        currentRPMAnimator?.start()
    }

    private fun getDegreesForCurrentRPM(): Float {
        return startAngle + degreesPerDataPoint * currentRPM
    }


}