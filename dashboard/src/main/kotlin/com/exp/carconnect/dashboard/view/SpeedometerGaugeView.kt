package com.exp.carconnect.dashboard.view

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v4.graphics.ColorUtils
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import com.exp.carconnect.base.R
import java.util.*

@SuppressLint("ViewConstructor")
internal class SpeedometerGaugeView(context: Context,
                                    attrs: AttributeSet? = null,
                                    defStyleAttr: Int = 0,
                                    defStyleRes: Int = -1,
                                    private val startAngle: Float,
                                    private val sweep: Float,
                                    onlineColor: Int,
                                    offlineColor: Int,
                                    gaugeBackgroundDrawable: Drawable) : MiddleGaugeView(context,
        attrs,
        defStyleAttr,
        defStyleRes,
        onlineColor,
        offlineColor,
        gaugeBackgroundDrawable) {

    companion object {
        private const val BIG_TICK_LENGTH_PERCENTAGE = .08f
        private const val BIG_TICK_WIDTH_PERCENTAGE = .009f
        private const val SMALL_TICK_LENGTH_PERCENTAGE = .06f
        private const val SMALL_TICK_WIDTH_PERCENTAGE = .007f
        private const val TICK_MARKER_TEXT_SIZE_PERCENTAGE = .05f
        private const val TICK_MARKER_MARGIN_PERCENTAGE = .025f

        private const val TOTAL_NO_OF_TICKS = 33
        private const val BIG_TICK_MULTIPLE = 2
        private const val TICK_MARKER_START = 0
        private const val TICK_MARKER_DIFF = 20
    }

    private val tickPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tickMarkerPaint: Paint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    private var bigTickLength: Float = 0.0f
    private var bigTickWidth: Float = 0.0f

    private var smallTickLength: Float = 0.0f
    private var smallTickWidth: Float = 0.0f

    private var textSize: Float = 0.0f
    private var testSizeMargin: Float = 0.0f

    init {
        tickMarkerPaint.textLocale = Locale.US
        tickMarkerPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        tickMarkerPaint.textAlign = Paint.Align.CENTER
        tickPaint.maskFilter = EmbossMaskFilter(floatArrayOf(1f, 5f, 1f), 0.8f, 6.0f, 20.0f)
    }

    override fun onDisconnected() {
        super.onDisconnected()
        tickPaint.color = offlineColor
        tickMarkerPaint.color = offlineColor
    }

    override fun onConnected() {
        super.onConnected()
        tickPaint.color = onlineColor
        tickMarkerPaint.color = onlineColor
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawGauge(canvas, gaugeBounds)
    }

    override fun onBoundChanged(bounds: RectF) {
        val gaugeCircumference = (2.0 * Math.PI * (bounds.width() / 2).toDouble()).toFloat()

        //draw ticks
        bigTickLength = bounds.width() * BIG_TICK_LENGTH_PERCENTAGE
        bigTickWidth = gaugeCircumference * BIG_TICK_WIDTH_PERCENTAGE

        smallTickLength = bounds.width() * SMALL_TICK_LENGTH_PERCENTAGE
        smallTickWidth = gaugeCircumference * SMALL_TICK_WIDTH_PERCENTAGE

        textSize = bounds.width() * TICK_MARKER_TEXT_SIZE_PERCENTAGE
        testSizeMargin = bounds.width() * TICK_MARKER_MARGIN_PERCENTAGE

    }

    override fun drawGauge(canvas: Canvas, bounds: RectF) {
        canvas.drawArc(bounds, startAngle, sweep, false, gaugePaint)

        drawTicks(canvas, bounds, startAngle,
                sweep / (TOTAL_NO_OF_TICKS - 1),
                TOTAL_NO_OF_TICKS, BIG_TICK_MULTIPLE,
                bigTickLength, bigTickWidth, smallTickLength, smallTickWidth,
                TICK_MARKER_START, TICK_MARKER_DIFF,
                textSize, testSizeMargin, tickMarkerPaint, tickPaint)
    }


}

@SuppressLint("ViewConstructor")
internal class SpeedIndicatorView(context: Context,
                                  attrs: AttributeSet? = null,
                                  defStyleAttr: Int = 0,
                                  defStyleRes: Int = -1,
                                  private val startAngle: Float,
                                  private val sweep: Float,
                                  currentSpeed: Float,
                                  showIgnitionIcon: Boolean,
                                  showCheckEngineLight: Boolean,
                                  speedDribbleEnabled: Boolean,
                                  onlineColor: Int,
                                  offlineColor: Int) : DashboardBasicView(context,
        attrs,
        defStyleAttr,
        defStyleRes,
        onlineColor,
        offlineColor) {


    companion object {
        internal const val MAX_SPEED = 320
        internal const val MIN_SPEED = 0

        private const val INNER_CIRCLE_WIDTH_PERCENTAGE = .45f
        private const val INDICATOR_DIMEN_PERCENTAGE = .1f
        private const val CHECK_ENGINE_LIGHT_DIMEN_PERCENTAGE = .08f
        private const val IGNITION_DIMEN_PERCENTAGE = .08f
        private const val INNER_CIRCLE_STROKE_WIDTH = 10
        private const val CURRENT_SPEED_TEXT_SIZE_PERCENTAGE = .05f
        private const val DEFAULT_SPEED_UNIT = "km/h"

        private const val DRIBBLE_RANGE = 5.0f
        private val DRIBBLE_RANDOM = Random()
    }

    internal var speedChangedListener: ((Float) -> Unit)? = null
    internal var ignitionIconChangedListener: ((Boolean) -> Unit)? = null
    internal var checkEngineLightChangedListener: ((Boolean) -> Unit)? = null
    internal var onCheckEngineLightIconClickListener: ((Boolean) -> Unit)? = null
    internal var onIgnitionIconClickListener: ((Boolean) -> Unit)? = null
    internal var onSpeedStripClickListener: ((Float) -> Unit)? = null

    private var currentSpeed: Float = currentSpeed
        set(value) {
            if (field == value) {
                return
            }
            field = value
            invalidate()
            speedChangedListener?.invoke(field)
        }
    internal var showIgnitionIcon: Boolean = showIgnitionIcon
        set(value) {
            field = value
            invalidate()
            ignitionIconChangedListener?.invoke(field)
        }
    internal var showCheckEngineLight: Boolean = showCheckEngineLight
        set(value) {
            field = value
            invalidate()
            checkEngineLightChangedListener?.invoke(field)
        }

    internal var speedDribbleEnabled: Boolean = speedDribbleEnabled
        set(value) {
            field = value
            if (value) {
                dribble()
            } else {
                cancelDribble()
            }
        }


    private var degreesPerDataPoint = sweep / MAX_SPEED
    private var currentSpeedAnimator: ObjectAnimator? = null
    private var dribbleSpeedAnimator: ObjectAnimator? = null

    private val innerCirclePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val speedTextPaint: Paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val speedStripPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val indicatorIcon = VectorDrawableCompat.create(context.resources, R.drawable.ic_arrow_right_24dp, null)!!
    private val checkEngineLightIcon = VectorDrawableCompat.create(context.resources, R.drawable.ic_check_engine_light, null)!!
    private val ignitionIcon = VectorDrawableCompat.create(context.resources, R.drawable.ic_ignition, null)!!

    private val activeIconColor = context.getColor(android.R.color.holo_orange_dark)
    private val inActiveIconColor = context.getColor(android.R.color.darker_gray)
    private val speedTextColor = context.getColor(android.R.color.black)

    private lateinit var innerCircleBound: RectF
    private lateinit var speedTextStripPath: Path
    private var innerCircleRadius: Float = 0.0f
    private lateinit var ignitionIconBounds: Rect
    private lateinit var checkEngineLightBounds: Rect
    private lateinit var speedTextStripBounds: RectF
    private lateinit var indicatorBound: Rect

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


        checkEngineLightIcon.setTint(inActiveIconColor)
        ignitionIcon.setTint(inActiveIconColor)
    }

    override fun onDisconnected() {
        super.onDisconnected()
        speedStripPaint.color = offlineColor
        innerCirclePaint.color = offlineColor
        innerCirclePaint.setShadowLayer(20f, 0f, 0f, offlineColor)

        indicatorIcon.setTint(offlineColor)

        cancelDribble()
    }

    override fun onConnected() {
        super.onConnected()
        speedStripPaint.color = onlineColor
        innerCirclePaint.color = onlineColor
        innerCirclePaint.setShadowLayer(20f, 0f, 0f, onlineColor)

        indicatorIcon.setTint(onlineColor)

        dribble()
    }

    override fun onBoundChanged(bounds: RectF) {
        val reduceWidthAndHeightBy = bounds.width() * (1 - INNER_CIRCLE_WIDTH_PERCENTAGE) / 2
        innerCircleBound = RectF(bounds)
        innerCircleBound.inset(reduceWidthAndHeightBy, reduceWidthAndHeightBy)

        //draw strip and current speed
        innerCircleRadius = innerCircleBound.width() / 2
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
        speedTextStripPath = Path()
        speedTextStripPath.arcTo(innerCircleBound, startDegree, -sweep)
        speedTextStripPath.arcTo(innerCircleBound, 180 - startDegree + sweep, -sweep)
        speedTextStripPath.close()

        speedTextStripBounds = RectF()
        speedTextStripPath.computeBounds(speedTextStripBounds, true)

        val ignitionIconDimen = bounds.width() * IGNITION_DIMEN_PERCENTAGE
        ignitionIconBounds = Rect((innerCircleBound.centerX() - innerCircleRadius / 2 - ignitionIconDimen / 2).toInt(),
                (innerCircleBound.centerY() - innerCircleRadius / 2 - ignitionIconDimen / 2).toInt(),
                (innerCircleBound.centerX() - innerCircleRadius / 2 + ignitionIconDimen / 2).toInt(),
                (innerCircleBound.centerY() - innerCircleRadius / 2 + ignitionIconDimen / 2).toInt())

        val checkEngineLightDimen = bounds.width() * CHECK_ENGINE_LIGHT_DIMEN_PERCENTAGE
        checkEngineLightBounds = Rect((innerCircleBound.centerX() + innerCircleRadius / 2 - checkEngineLightDimen / 2).toInt(),
                (innerCircleBound.centerY() - innerCircleRadius / 2 - checkEngineLightDimen / 2).toInt(),
                (innerCircleBound.centerX() + innerCircleRadius / 2 + checkEngineLightDimen / 2).toInt(),
                (innerCircleBound.centerY() - innerCircleRadius / 2 + checkEngineLightDimen / 2).toInt())

        val indicatorDimen = bounds.width() * INDICATOR_DIMEN_PERCENTAGE
        indicatorBound = Rect((innerCircleBound.centerX() + innerCircleBound.width() / 2).toInt(),
                (innerCircleBound.centerY() - indicatorDimen / 2).toInt(),
                (innerCircleBound.centerX() + innerCircleBound.width() / 2 + indicatorDimen).toInt(),
                (innerCircleBound.centerY() + indicatorDimen / 2).toInt())
    }


    override fun onDraw(canvas: Canvas) {
        drawMiddleGaugeInnerCircle(canvas, gaugeBounds)
    }


    private fun drawMiddleGaugeInnerCircle(canvas: Canvas, bounds: RectF) {

        val currentSpeedOverMaxSpeedRatio = currentSpeed / MAX_SPEED.toFloat()
        innerCirclePaint.color = ColorUtils.blendARGB(gaugePaint.color, Color.RED, currentSpeedOverMaxSpeedRatio)
        speedStripPaint.color = ColorUtils.blendARGB(gaugePaint.color, Color.RED, currentSpeedOverMaxSpeedRatio)


        //draw inner circle
        canvas.drawCircle(innerCircleBound.centerX(), innerCircleBound.centerY(),
                innerCircleBound.width() / 2, innerCirclePaint)

        //calculate Text Bounds
        val currentSpeedText = String.format("%.2f", currentSpeed) + " " + DEFAULT_SPEED_UNIT

        canvas.drawPath(speedTextStripPath, speedStripPaint)
        canvas.drawText(currentSpeedText, innerCircleBound.centerX(),
                innerCircleBound.centerY() + innerCircleRadius / 2, speedTextPaint)


        //draw check engine light
        drawIgnitionIcon(canvas, ignitionIconBounds)
        //draw check engine light
        drawCheckEngineLightIcon(canvas, checkEngineLightBounds)


        //draw indicator
        canvas.save()
        canvas.rotate(getDegreeForCurrentSpeed(), innerCircleBound.centerX(), innerCircleBound.centerY())
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

    private fun dribble() {
        if (speedDribbleEnabled && currentSpeed > MIN_SPEED /*&& dashboard.online*/) {
            val dribbleBy = DRIBBLE_RANDOM.nextFloat() * (DRIBBLE_RANGE - 0.0f) + 0.0f
            dribbleSpeedAnimator = ObjectAnimator.ofFloat(this, "currentSpeed", currentSpeed, currentSpeed + dribbleBy,
                    currentSpeed, currentSpeed - dribbleBy, currentSpeed)
            dribbleSpeedAnimator?.repeatMode = ObjectAnimator.RESTART
            dribbleSpeedAnimator?.repeatCount = ObjectAnimator.INFINITE
            //disabling dribble temporarily
//            dribbleSpeedAnimator?.start()
        } else {
            cancelDribble()
        }
    }

    private fun cancelDribble() {
        dribbleSpeedAnimator?.cancel()
        dribbleSpeedAnimator = null
    }

    @SuppressLint("ObjectAnimatorBinding")
    internal fun updateSpeed(speed: Float) {
        speedDribbleEnabled = false
        currentSpeedAnimator?.end()
        currentSpeedAnimator = ObjectAnimator.ofFloat(this, "currentSpeed", currentSpeed, speed)
        currentSpeedAnimator?.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
                if (speedDribbleEnabled) {
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

    override fun onTap(event: MotionEvent): Boolean {
        return when {
            ignitionIconBounds.contains(event.x.toInt(), event.y.toInt()) -> {
                onIgnitionIconClickListener?.invoke(showIgnitionIcon)
                true
            }
            checkEngineLightBounds.contains(event.x.toInt(), event.y.toInt()) -> {
                onCheckEngineLightIconClickListener?.invoke(showCheckEngineLight)
                true
            }
            speedTextStripBounds.contains(event.x, event.y) -> {
                onSpeedStripClickListener?.invoke(currentSpeed)
                true
            }
            else -> super.onTap(event)
        }
    }
}