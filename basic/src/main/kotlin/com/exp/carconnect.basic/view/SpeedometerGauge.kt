package com.exp.carconnect.basic.view

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.*
import android.support.graphics.drawable.VectorDrawableCompat
import android.text.TextPaint
import com.exp.carconnect.basic.R
import java.util.*


internal class SpeedometerGauge(dashboard: Dashboard,
                                private val startAngle: Float,
                                private val sweep: Float,
                                currentSpeed: Float,
                                showIgnitionIcon: Boolean,
                                showCheckEngineLight: Boolean,
                                speedDribbleEnabled: Boolean,
                                onlineColor: Int,
                                offlineColor: Int) : MiddleGauge(dashboard, onlineColor, offlineColor) {

    companion object {
        internal val MAX_SPEED = 320
        internal val MIN_SPEED = 0

        private val BIG_TICK_LENGTH_PERCENTAGE = .08f
        private val BIG_TICK_WIDTH_PERCENTAGE = .009f
        private val SMALL_TICK_LENGTH_PERCENTAGE = .06f
        private val SMALL_TICK_WIDTH_PERCENTAGE = .007f
        private val TICK_MARKER_TEXT_SIZE_PERCENTAGE = .05f
        private val TICK_MARKER_MARGIN_PERCENTAGE = .025f
        private val INNER_CIRCLE_WIDTH_PERCENTAGE = .45f
        private val INDICATOR_DIMEN_PERCENTAGE = .1f
        private val CHECK_ENGINE_LIGHT_DIMEN_PERCENTAGE = .08f
        private val IGNITION_DIMEN_PERCENTAGE = .08f

        private val INNER_CIRCLE_STROKE_WIDTH = 10
        private val TOTAL_NO_OF_TICKS = 33
        private val BIG_TICK_MULTIPLE = 2
        private val TICK_MARKER_START = 0
        private val TICK_MARKER_DIFF = 20
        private val CURRENT_SPEED_TEXT_SIZE_PERCENTAGE = .05f
        private val DEFAULT_SPEED_UNIT = "km/h"

        private val DRIBBLE_RANGE = 5.0f
        private val DRIBBLE_RANDOM = Random()
    }


    internal var speedChangedListener: ((Float) -> Unit)? = null
    internal var ignitionIconChangedListener: ((Boolean) -> Unit)? = null
    internal var checkEngineLightChangedListener: ((Boolean) -> Unit)? = null
    private var currentSpeed: Float = currentSpeed
        set(value) {
            field = value
            dashboard.invalidate()
            speedChangedListener?.invoke(field)
        }
    internal var showIgnitionIcon: Boolean = showIgnitionIcon
        set(value) {
            field = value
            dashboard.invalidate()
            ignitionIconChangedListener?.invoke(field)
        }
    internal var showCheckEngineLight: Boolean = showCheckEngineLight
        set(value) {
            field = value
            dashboard.invalidate()
            checkEngineLightChangedListener?.invoke(field)
        }

    internal var speedDribbleEnabled: Boolean = speedDribbleEnabled
        set(value) {
            field = value
            if (value) {
                dribble()
            } else {
                dribbleSpeedAnimator?.cancel()
                dribbleSpeedAnimator = null
            }
        }

    private var degreesPerDataPoint = sweep / MAX_SPEED
    private var currentSpeedAnimator: ObjectAnimator? = null
    private var dribbleSpeedAnimator: ObjectAnimator? = null


    private val innerCirclePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val speedTextPaint: Paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val speedStripPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tickPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tickMarkerPaint: Paint = TextPaint(Paint.ANTI_ALIAS_FLAG)


    private val indicatorIcon = VectorDrawableCompat.create(dashboard.context.resources, R.drawable.ic_arrow_right_24dp, null)!!
    private val checkEngineLightIcon = VectorDrawableCompat.create(dashboard.context.resources, R.drawable.ic_check_engine_light, null)!!
    private val ignitionIcon = VectorDrawableCompat.create(dashboard.context.resources, R.drawable.ic_ignition, null)!!

    private val activeIconColor = dashboard.context.getColor(android.R.color.holo_orange_dark)
    private val inActiveIconColor = dashboard.context.getColor(android.R.color.darker_gray)
    private val speedTextColor = dashboard.context.getColor(android.R.color.black)

    init {
        innerCirclePaint.style = Paint.Style.STROKE
        innerCirclePaint.strokeWidth = INNER_CIRCLE_STROKE_WIDTH.toFloat()
        innerCirclePaint.pathEffect = DashPathEffect(floatArrayOf(100f, 15f), 0f)


        speedTextPaint.color = speedTextColor
        speedTextPaint.textLocale = Locale.US
        speedTextPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        speedTextPaint.textAlign = Paint.Align.CENTER

        speedStripPaint.style = Paint.Style.FILL_AND_STROKE
        speedStripPaint.strokeWidth = 10f

        tickMarkerPaint.textLocale = Locale.US
        tickMarkerPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        tickMarkerPaint.textAlign = Paint.Align.CENTER


        checkEngineLightIcon.setTint(inActiveIconColor)
        ignitionIcon.setTint(inActiveIconColor)
    }

    override fun onDisconnected() {
        super.onDisconnected()
        speedStripPaint.color = offlineColor
        innerCirclePaint.color = offlineColor
        innerCirclePaint.setShadowLayer(20f, 0f, 0f, offlineColor)
        tickPaint.color = offlineColor
        tickMarkerPaint.color = offlineColor

        indicatorIcon.setTint(offlineColor)
    }

    override fun onConnected() {
        super.onConnected()
        speedStripPaint.color = onlineColor
        innerCirclePaint.color = onlineColor
        innerCirclePaint.setShadowLayer(20f, 0f, 0f, onlineColor)
        tickPaint.color = onlineColor
        tickMarkerPaint.color = onlineColor

        indicatorIcon.setTint(onlineColor)
    }


    override fun drawGauge(canvas: Canvas, bounds: RectF) {
        canvas.drawArc(bounds, startAngle, sweep, false, gaugePaint)
        val gaugeCircumference = (2.0 * Math.PI * (bounds.width() / 2).toDouble()).toFloat()

        //draw ticks
        val bigTickLength = bounds.width() * BIG_TICK_LENGTH_PERCENTAGE
        val bigTickWidth = gaugeCircumference * BIG_TICK_WIDTH_PERCENTAGE

        val smallTickLength = bounds.width() * SMALL_TICK_LENGTH_PERCENTAGE
        val smallTickWidth = gaugeCircumference * SMALL_TICK_WIDTH_PERCENTAGE

        val textSize = bounds.width() * TICK_MARKER_TEXT_SIZE_PERCENTAGE
        val testSizeMargin = bounds.width() * TICK_MARKER_MARGIN_PERCENTAGE

        drawTicks(canvas, bounds, startAngle,
                sweep / (TOTAL_NO_OF_TICKS - 1),
                TOTAL_NO_OF_TICKS, BIG_TICK_MULTIPLE,
                bigTickLength, bigTickWidth, smallTickLength, smallTickWidth,
                TICK_MARKER_START, TICK_MARKER_DIFF,
                textSize, testSizeMargin, tickMarkerPaint, tickPaint)


        drawMiddleGaugeInnerCircle(canvas, bounds)
    }


    private fun drawMiddleGaugeInnerCircle(canvas: Canvas, bounds: RectF) {

        //draw inner circle
        val reduceWidthAndHeightBy = bounds.width() * (1 - INNER_CIRCLE_WIDTH_PERCENTAGE) / 2
        val innerCircleBound = RectF(bounds)
        innerCircleBound.inset(reduceWidthAndHeightBy, reduceWidthAndHeightBy)
        canvas.drawCircle(innerCircleBound.centerX(), innerCircleBound.centerY(),
                innerCircleBound.width() / 2, innerCirclePaint)


        //draw strip and current speed
        val innerCircleRadius = innerCircleBound.width() / 2

        //calculate Text Bounds
        val currentSpeedText = String.format("%.2f", currentSpeed) + " " + DEFAULT_SPEED_UNIT
        val textBound = Rect()
        speedTextPaint.textSize = bounds.width() * CURRENT_SPEED_TEXT_SIZE_PERCENTAGE
        speedTextPaint.getTextBounds(currentSpeedText, 0, currentSpeedText.length, textBound)


        val textHeightToRadiusRatio = textBound.height() / innerCircleRadius

        //2r = 180 degrees, i.e. r/2 = 45 degrees
        //todo startdegree is suppose to be 45 degrees, not sure why 33 degrees making it work properly
        val startDegree = 33f
        val sweep = 90 * textHeightToRadiusRatio
        //draw strip
        val path = Path()
        path.arcTo(innerCircleBound, startDegree, -sweep)
        path.arcTo(innerCircleBound, 180 - startDegree + sweep, -sweep)
        path.close()

        canvas.drawPath(path, speedStripPaint)
        canvas.drawText(currentSpeedText, innerCircleBound.centerX(),
                innerCircleBound.centerY() + innerCircleRadius / 2, speedTextPaint)


        //draw check engine light
        val ignitionIconDimen = bounds.width() * IGNITION_DIMEN_PERCENTAGE
        val ignitionIconBounds = Rect((innerCircleBound.centerX() - innerCircleRadius / 2 - ignitionIconDimen / 2).toInt(),
                (innerCircleBound.centerY() - innerCircleRadius / 2 - ignitionIconDimen / 2).toInt(),
                (innerCircleBound.centerX() - innerCircleRadius / 2 + ignitionIconDimen / 2).toInt(),
                (innerCircleBound.centerY() - innerCircleRadius / 2 + ignitionIconDimen / 2).toInt())
        drawIgnitionIcon(canvas, ignitionIconBounds)


        //draw check engine light
        val checkEngineLightDimen = bounds.width() * CHECK_ENGINE_LIGHT_DIMEN_PERCENTAGE
        val checkEngineLightBounds = Rect((innerCircleBound.centerX() + innerCircleRadius / 2 - checkEngineLightDimen / 2).toInt(),
                (innerCircleBound.centerY() - innerCircleRadius / 2 - checkEngineLightDimen / 2).toInt(),
                (innerCircleBound.centerX() + innerCircleRadius / 2 + checkEngineLightDimen / 2).toInt(),
                (innerCircleBound.centerY() - innerCircleRadius / 2 + checkEngineLightDimen / 2).toInt())
        drawCheckEngineLightIcon(canvas, checkEngineLightBounds)


        //draw indicator
        canvas.save()
        canvas.rotate(getDegreeForCurrentSpeed(), innerCircleBound.centerX(), innerCircleBound.centerY())
        val indicatorDimen = bounds.width() * INDICATOR_DIMEN_PERCENTAGE
        val indicatorBound = Rect((innerCircleBound.centerX() + innerCircleBound.width() / 2).toInt(),
                (innerCircleBound.centerY() - indicatorDimen / 2).toInt(),
                (innerCircleBound.centerX() + innerCircleBound.width() / 2 + indicatorDimen).toInt(),
                (innerCircleBound.centerY() + indicatorDimen / 2).toInt())
        drawIndicator(canvas, indicatorBound)
        canvas.restore()
    }

    private fun drawIndicator(canvas: Canvas, indicatorBound: Rect) {
        indicatorIcon.bounds = indicatorBound
        indicatorIcon.draw(canvas)
    }

    private fun drawCheckEngineLightIcon(canvas: Canvas, checkEngineLightBounds: Rect) {
        checkEngineLightIcon.bounds = checkEngineLightBounds
        if (showCheckEngineLight) {
            checkEngineLightIcon.setTint(activeIconColor)
        } else {
            checkEngineLightIcon.setTint(inActiveIconColor)
        }
        checkEngineLightIcon.draw(canvas)
    }

    private fun drawIgnitionIcon(canvas: Canvas, ignitionIconBounds: Rect) {
        ignitionIcon.bounds = ignitionIconBounds
        if (showIgnitionIcon) {
            ignitionIcon.setTint(activeIconColor)
        } else {
            ignitionIcon.setTint(inActiveIconColor)
        }
        ignitionIcon.draw(canvas)
    }


    private fun getDegreeForCurrentSpeed(): Float {
        return startAngle + degreesPerDataPoint * currentSpeed
    }

    internal fun dribble() {
        if (speedDribbleEnabled && dashboard.currentSpeed > MIN_SPEED && dashboard.online) {
            val dribbleBy = DRIBBLE_RANDOM.nextFloat() * (DRIBBLE_RANGE - 0.0f) + 0.0f
            dribbleSpeedAnimator = ObjectAnimator.ofFloat(this, "currentSpeed", currentSpeed, dashboard.currentSpeed + dribbleBy,
                    dashboard.currentSpeed, dashboard.currentSpeed - dribbleBy, currentSpeed)
            dribbleSpeedAnimator?.addListener(object : Animator.AnimatorListener {
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
            dribbleSpeedAnimator?.start()
        }
    }

    @SuppressLint("ObjectAnimatorBinding")
    internal fun updateSpeed(speed: Float) {
        speedDribbleEnabled = false
        currentSpeedAnimator?.cancel()
        currentSpeedAnimator = ObjectAnimator.ofFloat(this, "currentSpeed", currentSpeed, speed)
        currentSpeedAnimator?.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
                if (dashboard.speedDribbleEnabled) {
                    speedDribbleEnabled = true
                }
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationStart(animation: Animator?) {
            }
        })
        currentSpeedAnimator?.start()
    }
}