package com.exp.carconnect.dashboard.view

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.*
import android.support.v4.graphics.ColorUtils
import android.text.TextPaint
import android.view.MotionEvent
import java.util.*


internal class RPMGauge(dashboard: Dashboard,
                        private val startAngle: Float,
                        private val sweep: Float,
                        rpmDribbleEnabled: Boolean,
                        currentRPM: Float,
                        onlineColor: Int,
                        offlineColor: Int) : LeftGauge(dashboard, onlineColor, offlineColor) {

    companion object {

        internal const val MIN_RPM = 0f
        internal const val MAX_RPM = 8f

        internal const val TOTAL_NO_OF_DATA_POINTS = MAX_RPM

        private const val RPM_TEXT = "RPM x 1000"
        private const val TOTAL_NO_OF_TICKS = 25
        private const val BIG_TICK_MULTIPLE = 3
        private const val TICK_MARKER_START = 0
        private const val TICK_MARKER_DIFF = 1
        private const val BIG_TICK_LENGTH_PERCENTAGE = .08f
        private const val BIG_TICK_WIDTH_PERCENTAGE = .009f
        private const val SMALL_TICK_LENGTH_PERCENTAGE = .06f
        private const val SMALL_TICK_WIDTH_PERCENTAGE = .007f
        private const val TICK_MARKER_TEXT_SIZE_PERCENTAGE = .06f
        private const val TICK_MARKER_MARGIN_PERCENTAGE = .03f
        private const val INDICATOR_CENTER_PERCENTAGE = .15f
        private const val INDICATOR_LENGTH_PERCENTAGE = .5f
        private const val RPM_TEXT_SIZE_PERCENTAGE = .06f
        private const val RPM_TEXT_MARGIN = .04f

        private const val CRITICAL_ANGLE_SWEEP = 45f

        private const val INDICATOR_CIRCLE_STROKE_WIDTH = 10
        private const val INDICATOR_STROKE_WIDTH = 25

        private const val DRIBBLE_RANGE = .1f
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
                cancelDribble()
            }
        }


    private var currentRPM = currentRPM
        set(value) {
            if (field == value) {
                return
            }
            field = value
            dashboard.invalidateRPMGauge()
            rpmChangedListener?.invoke(field)
        }


    private var currentRPMAnimator: ObjectAnimator? = null
    private var dribbleRPMAnimator: ObjectAnimator? = null
    internal var rpmChangedListener: ((Float) -> Unit)? = null
    internal var onRPMClickListener: ((Float) -> Unit)? = null


    private val degreesPerDataPoint = sweep / TOTAL_NO_OF_DATA_POINTS

    private val normalZoneStartAngle: Float = startAngle
    private val normalZoneSweep: Float = sweep - CRITICAL_ANGLE_SWEEP
    private val criticalZoneStartAngle: Float = startAngle + normalZoneSweep

    private var bigTickLength: Float = 0.0f
    private var bigTickWidth: Float = 0.0f

    private var smallTickLength: Float = 0.0f
    private var smallTickWidth: Float = 0.0f

    private var textSize: Float = 0.0f
    private var textMargin: Float = 0.0f

    init {

        tickMarkerPaint.textLocale = Locale.US
        tickMarkerPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        tickMarkerPaint.textAlign = Paint.Align.CENTER

        tickPaint.maskFilter = EmbossMaskFilter(floatArrayOf(1f, 5f, 1f),
                0.8f,
                6.0f,
                20.0f)

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
        indicatorThinLinePaint.shader = LinearGradient(0f,
                0f,
                100f,
                100f,
                criticalZoneColor,
                getDarkerColor(criticalZoneColor),
                Shader.TileMode.MIRROR)
    }

    override fun onConnected() {
        super.onConnected()
        tickPaint.color = onlineColor
        tickMarkerPaint.color = onlineColor

        rpmTextPaint.color = onlineColor

        indicatorCenterPaint.color = onlineColor
        indicatorCenterPaint.setShadowLayer(20f,
                0f,
                0f,
                onlineColor)
        indicatorPaint.color = onlineColor
        indicatorPaint.shader = LinearGradient(0f,
                0f,
                100f,
                100f,
                onlineColor,
                onlineGradientColor,
                Shader.TileMode.MIRROR)
        indicatorPaint.setShadowLayer(20f,
                0f,
                0f,
                onlineColor)
        dribble()
    }

    override fun onDisconnected() {
        super.onDisconnected()

        tickPaint.color = offlineColor
        tickMarkerPaint.color = offlineColor

        rpmTextPaint.color = offlineColor

        indicatorCenterPaint.color = offlineColor
        indicatorCenterPaint.setShadowLayer(20f,
                0f,
                0f,
                offlineColor)
        indicatorPaint.color = offlineColor
        indicatorPaint.shader = LinearGradient(0f,
                0f,
                100f,
                100f,
                offlineColor,
                offlineGradientColor,
                Shader.TileMode.MIRROR)
        indicatorPaint.setShadowLayer(20f,
                0f,
                0f,
                offlineColor)
        cancelDribble()
    }


    override fun onBoundChanged(bounds: RectF) {

        val gaugeCircumference = (2.0 * Math.PI * (bounds.width() / 2).toDouble()).toFloat()

        //draw ticks
        bigTickLength = bounds.width() * BIG_TICK_LENGTH_PERCENTAGE
        bigTickWidth = gaugeCircumference * BIG_TICK_WIDTH_PERCENTAGE

        smallTickLength = bounds.width() * SMALL_TICK_LENGTH_PERCENTAGE
        smallTickWidth = gaugeCircumference * SMALL_TICK_WIDTH_PERCENTAGE

        textSize = bounds.width() * TICK_MARKER_TEXT_SIZE_PERCENTAGE
        textMargin = bounds.width() * TICK_MARKER_MARGIN_PERCENTAGE
    }


    override fun drawGauge(canvas: Canvas, bounds: RectF) {
        canvas.drawArc(bounds,
                startAngle,
                sweep,
                false,
                gaugePaint)

        canvas.drawArc(bounds,
                normalZoneStartAngle,
                normalZoneSweep,
                false,
                gaugePaint)

        val color = gaugePaint.color
        gaugePaint.color = criticalZoneColor
        canvas.drawArc(bounds,
                criticalZoneStartAngle,
                CRITICAL_ANGLE_SWEEP,
                false,
                gaugePaint)

        gaugePaint.color = color

        drawTicks(canvas,
                bounds,
                startAngle,
                sweep / (TOTAL_NO_OF_TICKS - 1),
                TOTAL_NO_OF_TICKS,
                BIG_TICK_MULTIPLE,
                bigTickLength,
                bigTickWidth,
                smallTickLength,
                smallTickWidth,
                TICK_MARKER_START,
                TICK_MARKER_DIFF,
                textSize,
                textMargin,
                tickMarkerPaint,
                tickPaint,
                true,
                CRITICAL_ANGLE_SWEEP,
                criticalZoneColor)

        drawIndicator(canvas, bounds)
    }

    private val indicatorTextSize = Rect()
    private fun drawIndicator(canvas: Canvas, parentBounds: RectF) {
        val currentSpeedOverMaxSpeedRatio = currentRPM / MAX_RPM.toFloat()
        indicatorCenterPaint.color = ColorUtils.blendARGB(gaugePaint.color, Color.RED, currentSpeedOverMaxSpeedRatio)

        val indicatorCenterRadius = parentBounds.width() * INDICATOR_CENTER_PERCENTAGE / 2
        canvas.drawCircle(parentBounds.centerX(),
                parentBounds.centerY(),
                indicatorCenterRadius,
                indicatorCenterPaint)

        val rpmTextSize = parentBounds.width() * RPM_TEXT_SIZE_PERCENTAGE
        val rpmTextTopMargin = parentBounds.width() * RPM_TEXT_MARGIN
        rpmTextPaint.textSize = rpmTextSize
        rpmTextPaint.getTextBounds(RPM_TEXT,
                0,
                RPM_TEXT.length,
                indicatorTextSize)
        canvas.drawText(RPM_TEXT,
                parentBounds.centerX(),
                parentBounds.centerY() + indicatorCenterRadius + indicatorTextSize.height() + rpmTextTopMargin,
                rpmTextPaint)

        canvas.save()
        canvas.rotate(getDegreesForCurrentRPM(),
                parentBounds.centerX(),
                parentBounds.centerY())

        val indicatorLength = parentBounds.width() * INDICATOR_LENGTH_PERCENTAGE
        val x1 = (parentBounds.centerX() - indicatorLength * .35).toFloat()
        val y1 = parentBounds.centerY()
        val x2 = (parentBounds.centerX() + indicatorLength * .65).toFloat()
        val y2 = parentBounds.centerY()

        canvas.drawLine(x1, y1, x2, y2, indicatorPaint)
        canvas.drawLine(x1, y1, x2, y2, indicatorThinLinePaint)

        canvas.restore()
    }


    private fun dribble() {
        if (rpmDribbleEnabled && dashboard.currentRPM > MIN_RPM && dashboard.online) {
            val dribbleBy = DRIBBLE_RANDOM.nextFloat() * (DRIBBLE_RANGE - 0.0f) + 0.0f
            dribbleRPMAnimator = ObjectAnimator.ofFloat(this,
                    "currentRPM", currentRPM,
                    dashboard.currentRPM + dribbleBy,
                    dashboard.currentRPM,
                    dashboard.currentRPM - dribbleBy,
                    currentRPM)
            dribbleRPMAnimator?.repeatMode = ObjectAnimator.RESTART
            dribbleRPMAnimator?.repeatCount = ObjectAnimator.INFINITE
            //disabling dribble temporarily
//            dribbleRPMAnimator?.start()
        } else {
            cancelDribble()
        }
    }

    private fun cancelDribble() {
        dribbleRPMAnimator?.cancel()
        dribbleRPMAnimator = null
    }

    @SuppressLint("ObjectAnimatorBinding")
    internal fun updateRPM(rpm: Float) {
        rpmDribbleEnabled = false
        currentRPMAnimator?.end()
        currentRPMAnimator = ObjectAnimator.ofFloat(this,
                "currentRPM",
                currentRPM,
                rpm)
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

    override fun onTap(event: MotionEvent): Boolean {
        onRPMClickListener?.invoke(dashboard.currentRPM)
        return true
    }


}