package com.exp.carconnect.dashboard.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Parcelable
import android.support.annotation.RequiresApi
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v4.view.GestureDetectorCompat
import android.text.TextPaint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.exp.carconnect.base.R
import com.exp.carconnect.dashboard.view.RPMGauge.Companion.MAX_RPM
import com.exp.carconnect.dashboard.view.RPMGauge.Companion.MIN_RPM
import com.exp.carconnect.dashboard.view.SpeedometerGauge.Companion.MAX_SPEED
import com.exp.carconnect.dashboard.view.SpeedometerGauge.Companion.MIN_SPEED
import java.util.*


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class Dashboard @JvmOverloads constructor(context: Context,
                                          attrs: AttributeSet? = null,
                                          defStyleAttr: Int = 0, defStyleRes: Int = -1) :
        View(context, attrs, defStyleAttr, defStyleRes) {


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

    private val vinCharGapInDegrees = VIN_SWEEP / VIN_LENGTH
    private lateinit var viewCenter: PointF
    private lateinit var middleGaugeBounds: RectF
    private lateinit var leftGaugeBounds: RectF
    private lateinit var rightGaugeBounds: RectF
    private lateinit var leftGaugeBoundsToBeShownCompletely: RectF
    private lateinit var rightGaugeBoundsToBeShownCompletely: RectF
    private lateinit var labelBoundsOnCanvas: RectF
    private var animatingSideGauges = false
    private var middleGaugeRadius: Float = 0.0f
    private var sideGaugeRadius: Float = 0.0f
    private var vinCharSize: Float = 0.0f
    private val transientRect = Rect()


    var onOnlineChangedListener: ((Boolean) -> Unit)? = null
    var onVINChangedListener: ((String) -> Unit)? = null

    var fuelPercentage = 0.0f
        set(value) {
            if (value in 0.0..1.0 && value != field) {
                fuelAndTemperatureGauge.updateFuel(value)
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
                onOnlineChangedListener?.invoke(field)
            }
        }

    var vin = "-------VIN-------"
        set(value) {
            if (value.length == 17 && value != field) {
                field = value
                invalidateSpeedometerGauge()
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


    var currentAirIntakeTemp = 0.0f
        set(value) {
            if (value != field) {
                fuelAndTemperatureGauge.currentAirIntakeTemperature = value
                field = value
            }
        }

    var currentAmbientTemp = 0.0f
        set(value) {
            if (value != field) {
                fuelAndTemperatureGauge.currentAmbientTemperature = value
                field = value
            }
        }


    var showSideGauges = true
        set(value) {
            if (value != field && !animatingSideGauges) {
                field = value
                if (field) {
                    showSideGauges()
                } else {
                    hideSideGauges()
                }
            }
        }

//    private val onlineColor = Color.rgb(255, 116, 0)
//    private val onlineColor = Color.rgb(105, 8, 233)
    //    private val onlineColor = Color.rgb(105, 71, 255)
    //    private val onlineColor = Color.rgb(198, 132, 0)
    //private val onlineColor = Color.rgb(89, 134, 0)
    //    private val onlineColor = Color.rgb(101, 57, 244)


    private val onlineColor = Color.rgb(104, 82, 168)
    private val offlineColor = Color.argb(100, 104, 82, 168)

    private val vinOnlineColor = context.getColor(android.R.color.holo_orange_dark)
    private val vinOfflineColor = context.getColor(android.R.color.white)
    private val labelColor = context.getColor(android.R.color.holo_blue_dark)
    private val gaugeBGPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val vinPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val labelBound: Rect = Rect()
    private val gaugeBackgroundDrawable = RoundedBitmapDrawableFactory.create(resources,
            (getContext().getDrawable(R.drawable.gauge_bg) as BitmapDrawable).bitmap)


    private val speedometerGauge = SpeedometerGauge(this, MIDDLE_GAUGE_START_ANGLE.toFloat(), MIDDLE_GAUGE_SWEEP_ANGLE.toFloat(),
            currentSpeed, showIgnitionIcon, showCheckEngineLight, speedDribbleEnabled, onlineColor, offlineColor)
    private val rpmGauge = RPMGauge(this, LEFT_GAUGE_START_ANGLE.toFloat(), LEFT_GAUGE_SWEEP_ANGLE.toFloat(),
            rpmDribbleEnabled, currentRPM, onlineColor, offlineColor)
    private val fuelAndTemperatureGauge = FuelAndTemperatureGauge(this, RIGHT_GAUGE_START_ANGLE.toFloat(), RIGHT_GAUGE_SWEEP_ANGLE.toFloat(),
            .01f, currentAirIntakeTemp, currentAmbientTemp, onlineColor, offlineColor)

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            return when {
                middleGaugeBounds.contains(e.x, e.y) -> {
                    speedometerGauge.onTap(e)
                    true
                }
                leftGaugeBounds.contains(e.x, e.y) -> {
                    rpmGauge.onTap(e)
                    true
                }
                rightGaugeBounds.contains(e.x, e.y) -> {
                    fuelAndTemperatureGauge.onTap(e)
                    true
                }
                else -> super.onSingleTapUp(e)
            }
        }
    }
    private val gestureDetector = GestureDetectorCompat(this.context, gestureListener)


    init {
        init()
    }

    private fun init() {
//        setLayerType(LAYER_TYPE_SOFTWARE, null)

        gaugeBackgroundDrawable.isCircular = true
        background = context.getDrawable(R.drawable.dashboard_bg)

        gaugeBGPaint.shader = BitmapShader((context.getDrawable(R.drawable.gauge_bg) as BitmapDrawable).bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        gaugeBGPaint.style = Paint.Style.FILL

        vinPaint.textLocale = Locale.US
        vinPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        vinPaint.textAlign = Paint.Align.CENTER

        labelPaint.textLocale = Locale.US
        labelPaint.typeface = Typeface.create(ResourcesCompat.getFont(context, R.font.atomic_age), Typeface.BOLD)
        labelPaint.color = labelColor

        labelPaint.textSize = LABEL_REFERENCE_TEXT_SIZE
        labelPaint.getTextBounds(LABEL_TEXT, 0, LABEL_TEXT.length, labelBound)

        adoptOnlineStatus()

        addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
            override fun onViewDetachedFromWindow(v: View?) {
                //todo this logic needs to be updated to stop dribble
                currentRPM = 0.0f
                currentSpeed = 0.0f
            }

            override fun onViewAttachedToWindow(v: View?) {
            }
        })

    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        this.gestureDetector.onTouchEvent(event)
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val width = w.toFloat()
        val height = h.toFloat()
        val remainingWidthAfterPadding = width - paddingLeft.toFloat() - paddingRight.toFloat()
        val remainingHeihtAfterPadding = height - paddingTop.toFloat() - paddingBottom.toFloat()
        //todo view's height should be at least 45% of width, which is middle gauge diameter requirement or I should consider height into consideration before finalizing gauges diameter
        viewCenter = PointF(width / 2, height / 2)

        middleGaugeRadius = remainingWidthAfterPadding * MIDDLE_GAUGE_WIDTH_PERCENTAGE / 2
        sideGaugeRadius = remainingWidthAfterPadding * LEFT_GAUGE_WIDTH_PERCENTAGE / 2

        middleGaugeBounds = RectF(viewCenter.x - middleGaugeRadius, viewCenter.y - middleGaugeRadius,
                viewCenter.x + middleGaugeRadius, viewCenter.y + middleGaugeRadius)

        val leftGaugeStartPoint = Math.ceil(middleGaugeBounds.left - sideGaugeRadius * Math.sqrt(2.0)).toInt()
        leftGaugeBounds = RectF(leftGaugeStartPoint.toFloat(), viewCenter.y - sideGaugeRadius,
                leftGaugeStartPoint + 2 * sideGaugeRadius, viewCenter.y + sideGaugeRadius)
        leftGaugeBoundsToBeShownCompletely = RectF(leftGaugeBounds)

        val rightGaugeEndpoint = Math.ceil(middleGaugeBounds.right + sideGaugeRadius * Math.sqrt(2.0)).toInt()
        rightGaugeBounds = RectF(rightGaugeEndpoint - 2 * sideGaugeRadius, viewCenter.y - sideGaugeRadius,
                rightGaugeEndpoint.toFloat(), viewCenter.y + sideGaugeRadius)
        rightGaugeBoundsToBeShownCompletely = RectF(rightGaugeBounds)

        if (!showSideGauges) {
            rightGaugeBounds.offset(-rightGaugeBounds.width(), 0f)
            leftGaugeBounds.offset(leftGaugeBounds.width(), 0f)
        }


        val availableHeight = (remainingHeihtAfterPadding - (2 * sideGaugeRadius)) / 2
        val margin = availableHeight * DASHBOARD_LABEL_MARGIN_PERCENTAGE_OF_AVAILABLE_HEIGHT
        labelBoundsOnCanvas = RectF(margin, viewCenter.y + sideGaugeRadius + margin,
                labelBound.width().toFloat(), viewCenter.y + sideGaugeRadius + availableHeight - margin)


        val middleGaugeCircumference: Float = (Math.PI * middleGaugeBounds.width()).toFloat()
        val areaToDrawVinText = middleGaugeCircumference * (VIN_SWEEP / 360f)
        vinCharSize = areaToDrawVinText / VIN_LENGTH


        speedometerGauge.onBoundChanged(middleGaugeBounds)
        rpmGauge.onBoundChanged(leftGaugeBounds)

        val sideAndMiddleGaugeIntersectionLength = Math.abs(rightGaugeBoundsToBeShownCompletely.left - middleGaugeBounds.right)
        fuelAndTemperatureGauge.onBoundChanged(rightGaugeBounds, middleGaugeRadius, sideAndMiddleGaugeIntersectionLength)

    }


    //todo improve this and avoid allocation in onDraw
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        drawGauges(canvas, middleGaugeBounds, leftGaugeBounds, rightGaugeBounds)
        drawVin(canvas, middleGaugeBounds)
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
        drawRPMGauge(canvas, leftSideGaugeBounds)
        drawFuelGauge(canvas, rightSideGaugeBounds)
        drawSpeedometerGauge(canvas, middleGaugeBounds)
    }

    private fun drawSpeedometerGauge(canvas: Canvas, middleGaugeBounds: RectF) {
        //drawing middle gauge bg at end, otherwise left and right gauge bgs overwrite the middle gauge bg a little bit
        gaugeBackgroundDrawable.setBounds(middleGaugeBounds.left.toInt(), middleGaugeBounds.top.toInt(), middleGaugeBounds.right.toInt(), middleGaugeBounds.bottom.toInt())
        gaugeBackgroundDrawable.draw(canvas)
        speedometerGauge.drawGauge(canvas, middleGaugeBounds)
    }

    private fun drawRPMGauge(canvas: Canvas, leftSideGaugeBounds: RectF) {
        gaugeBackgroundDrawable.setBounds(leftSideGaugeBounds.left.toInt(), leftSideGaugeBounds.top.toInt(), leftSideGaugeBounds.right.toInt(), leftSideGaugeBounds.bottom.toInt())
        gaugeBackgroundDrawable.draw(canvas)
        rpmGauge.drawGauge(canvas, leftSideGaugeBounds)
    }

    private fun drawFuelGauge(canvas: Canvas, rightSideGaugeBounds: RectF) {
        gaugeBackgroundDrawable.setBounds(rightSideGaugeBounds.left.toInt(), rightSideGaugeBounds.top.toInt(), rightSideGaugeBounds.right.toInt(), rightSideGaugeBounds.bottom.toInt())
        gaugeBackgroundDrawable.draw(canvas)
        fuelAndTemperatureGauge.drawGauge(canvas, rightSideGaugeBounds)
    }


    private fun drawVin(canvas: Canvas, middleGaugeBounds: RectF) {
        if (!vin.isEmpty()) {
            vinPaint.textSize = vinCharSize
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

                canvas.rotate(-vinCharGapInDegrees, middleGaugeBounds.centerX(), middleGaugeBounds.centerY())
                totalRotationSoFar -= vinCharGapInDegrees
            }

            canvas.restore()
        }

    }

    private fun adoptOnlineStatus() {
        if (online) {
            vinPaint.color = vinOnlineColor
            speedometerGauge.onConnected()
            rpmGauge.onConnected()
            fuelAndTemperatureGauge.onConnected()

        } else {
            vinPaint.color = vinOfflineColor
            speedometerGauge.onDisconnected()
            rpmGauge.onDisconnected()
            fuelAndTemperatureGauge.onDisconnected()

        }
    }

    private fun showSideGauges() {
        startSideGaugeAnimation(rightGaugeBounds.left,
                rightGaugeBoundsToBeShownCompletely.left)
        {
            rightGaugeBounds.offset(it, 0f)
            leftGaugeBounds.offset(-it, 0f)
        }
    }

    private fun hideSideGauges() {
        startSideGaugeAnimation(rightGaugeBoundsToBeShownCompletely.left,
                rightGaugeBounds.left - rightGaugeBounds.width())
        {
            rightGaugeBounds.offset(-it, 0f)
            leftGaugeBounds.offset(it, 0f)
        }
    }


    private fun startSideGaugeAnimation(animateFrom: Float, animateTo: Float, offsetBoundsBy: (Float) -> Unit) {
        val animator = ValueAnimator
                .ofFloat(animateFrom, animateTo)
        animator.addUpdateListener({
            val animatedBy = Math.abs((it.animatedValue as Float) - rightGaugeBounds.left)
            offsetBoundsBy(animatedBy)
            rpmGauge.onBoundChanged(leftGaugeBounds)
            fuelAndTemperatureGauge.onBoundChanged(rightGaugeBounds)
            invalidate()
        })
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                animatingSideGauges = true
            }

            override fun onAnimationEnd(animation: Animator?) {
                animatingSideGauges = false
            }
        })
        animator.start()
    }

    internal fun invalidateSpeedometerGauge() {
//        middleGaugeBounds.round(transientRect)
//        invalidate(transientRect)
        invalidate()
    }

    internal fun invalidateRPMGauge() {
//        leftGaugeBounds.round(transientRect)
//        invalidate(transientRect)
        invalidate()
    }


    internal fun invalidateFuelGauge() {
//        rightGaugeBounds.round(transientRect)
//        invalidate(transientRect)
        invalidate()
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
        fuelAndTemperatureGauge.fuelPercentageChangedListener = listener
    }

    fun setOnCheckEngineLightChangedListener(listener: (Boolean) -> Unit) {
        speedometerGauge.checkEngineLightChangedListener = listener
    }

    fun setOnIgnitionChangedListener(listener: (Boolean) -> Unit) {
        speedometerGauge.ignitionIconChangedListener = listener
    }


    fun setOnRPMGaugeCLickListener(listener: (Float) -> Unit) {
        rpmGauge.onRPMClickListener = listener

    }

    fun setOnIgnitionIconCLickListener(listener: (Boolean) -> Unit) {
        speedometerGauge.onIgnitionIconClickListener = listener
    }

    fun setOnCheckEngineLightIconCLickListener(listener: (Boolean) -> Unit) {
        speedometerGauge.onCheckEngineLightIconClickListener = listener
    }

    fun setOnSpeedStripClickListener(listener: (Float) -> Unit) {
        speedometerGauge.onSpeedStripClickListener = listener
    }

    fun setOnAirIntakeTempClickListener(listener: (Float) -> Unit) {
        fuelAndTemperatureGauge.onAirIntakeTempClickListener = listener
    }

    fun setOnAmbientTempClickListener(listener: (Float) -> Unit) {
        fuelAndTemperatureGauge.onAmbientTempClickListener = listener
    }

    fun setOnFuelIconClickListener(listener: (Float) -> Unit) {
        fuelAndTemperatureGauge.onFuelIconCLickListener = listener
    }


}
