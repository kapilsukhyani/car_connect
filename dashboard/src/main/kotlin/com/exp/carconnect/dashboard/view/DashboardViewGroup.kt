package com.exp.carconnect.dashboard.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v4.view.GestureDetectorCompat
import android.text.TextPaint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.exp.carconnect.base.R
import com.exp.carconnect.dashboard.view.RPMGauge.Companion.MAX_RPM
import com.exp.carconnect.dashboard.view.RPMGauge.Companion.MIN_RPM
import com.exp.carconnect.dashboard.view.SpeedometerGauge.Companion.MAX_SPEED
import com.exp.carconnect.dashboard.view.SpeedometerGauge.Companion.MIN_SPEED
import java.util.*


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class DashboardViewGroup @JvmOverloads constructor(context: Context,
                                                   attrs: AttributeSet? = null,
                                                   defStyleAttr: Int = 0,
                                                   defStyleRes: Int = -1) :
        ViewGroup(context, attrs, defStyleAttr, defStyleRes) {


    companion object {
        private const val LABEL_TEXT = "Car Connect"
        private const val LABEL_REFERENCE_TEXT_SIZE = 50f

        private const val MIDDLE_GAUGE_START_ANGLE = 135
        private const val MIDDLE_GAUGE_SWEEP_ANGLE = 270
        private const val LEFT_GAUGE_START_ANGLE = 55
        private const val LEFT_GAUGE_SWEEP_ANGLE = 250
        private const val RIGHT_GAUGE_START_ANGLE = 235
        private const val RIGHT_GAUGE_SWEEP_ANGLE = 250

        private const val MIDDLE_GAUGE_WIDTH_PERCENTAGE = 0.4f
        private const val LEFT_GAUGE_WIDTH_PERCENTAGE = 0.3f
        private const val DASHBOARD_LABEL_MARGIN_PERCENTAGE_OF_AVAILABLE_HEIGHT = 0.05f
        private const val RIGHT_GAUGE_WIDTH_PERCENTAGE = LEFT_GAUGE_WIDTH_PERCENTAGE
        private const val MINIMUM_WIDTH = 800
        private const val MINIMUM_HEIGHT = 400 // fifty percent of height
    }

    enum class Theme {
        Light,
        Dark
    }

    private val viewCenter: PointF = PointF()
    private val middleGaugeBounds: RectF = RectF()
    private val leftGaugeBounds: RectF = RectF()
    private val rightGaugeBounds: RectF = RectF()
    private val leftGaugeBoundsToBeShownCompletely: RectF = RectF()
    private val rightGaugeBoundsToBeShownCompletely: RectF = RectF()
    private val labelBoundsOnCanvas: RectF = RectF()
    private var animatingSideGauges = false
    private var middleGaugeRadius: Float = 0.0f
    private var sideGaugeRadius: Float = 0.0f

    private var layedOut = false
    var onOnlineChangedListener: ((Boolean) -> Unit)? = null
    var onVINChangedListener: ((String) -> Unit)? = null


    var theme: Theme = Theme.Dark
        set(value) {
            if (value != field) {
                onlineColor = if (value == Theme.Dark) {
                    darkOnlineColor
                } else {
                    lightOnlineColor
                }
                speedometerGauge.setOnlineColor(onlineColor)
                rpmGauge.setOnlineColor(onlineColor)
                fuelAndTemperatureGauge.setOnlineColor(onlineColor)
                field = value
                adoptOnlineStatus()
                invalidate()
            }
        }

    var fuelPercentage = 0.0f
        set(value) {
            if (value in 0.0..1.0 &&
                    value != field &&
                    !animatingSideGauges) {
                fuelAndTemperatureGauge.updateFuel(value)
                field = value
            }
        }

    var currentSpeed = 0f
        set(value) {
            if (value in MIN_SPEED..MAX_SPEED &&
                    value != field &&
                    !animatingSideGauges) {
                speedIndicatorView.updateSpeed(value)
                field = value
            }
        }

    var showCheckEngineLight = false
        set(value) {
            if (value != field) {
                speedIndicatorView.showCheckEngineLight = value
                field = value
            }
        }

    var showIgnitionIcon = false
        set(value) {
            if (value != field) {
                speedIndicatorView.showIgnitionIcon = value
                field = value
            }
        }

    var currentRPM = 0.0f
        set(value) {
            if (value in MIN_RPM..MAX_RPM &&
                    value != field &&
                    !animatingSideGauges) {
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
                vinView.vin = value
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
                speedIndicatorView.speedDribbleEnabled = value
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
                if (layedOut) {
                    if (field) {
                        showSideGauges()
                    } else {
                        hideSideGauges()
                    }
                }
            }
        }

    private val lightOnlineColor = Color.rgb(179, 157, 219)
    private val darkOnlineColor = Color.rgb(104, 82, 168)

    private var onlineColor = darkOnlineColor
    private val offlineColor = Color.argb(100, 104, 82, 168)


    private val labelColor = context.getColor(R.color.accent)
    private val gaugeBGPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val labelBound: Rect = Rect()
    private val gaugeBackgroundDrawable = RoundedBitmapDrawableFactory.create(resources,
            (getContext().getDrawable(R.drawable.gauge_bg) as BitmapDrawable).bitmap)


    private val speedometerGauge = SpeedometerGaugeView(getContext(), null, 0, -1,
            MIDDLE_GAUGE_START_ANGLE.toFloat(),
            MIDDLE_GAUGE_SWEEP_ANGLE.toFloat(),
            onlineColor,
            offlineColor,
            gaugeBackgroundDrawable)
    private val speedIndicatorView = SpeedIndicatorView(getContext(), null, 0, -1,
            MIDDLE_GAUGE_START_ANGLE.toFloat(),
            MIDDLE_GAUGE_SWEEP_ANGLE.toFloat(),
            currentSpeed,
            showIgnitionIcon,
            showCheckEngineLight,
            speedDribbleEnabled,
            onlineColor,
            offlineColor)
    private val rpmGauge = RPMGaugeView(getContext(), null, 0, -1,
            LEFT_GAUGE_START_ANGLE.toFloat(),
            LEFT_GAUGE_SWEEP_ANGLE.toFloat(),
            rpmDribbleEnabled,
            currentRPM,
            onlineColor,
            offlineColor,
            gaugeBackgroundDrawable)
    private val fuelAndTemperatureGauge = FuelAndTemperatureGaugeView(getContext(), null, 0, -1,
            RIGHT_GAUGE_START_ANGLE.toFloat(),
            RIGHT_GAUGE_SWEEP_ANGLE.toFloat(),
            .01f,
            currentAirIntakeTemp,
            currentAmbientTemp,
            onlineColor,
            offlineColor,
            gaugeBackgroundDrawable)

    private val vinView = VINView(getContext(), null, 0, -1)

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            return when {
                middleGaugeBounds.contains(e.x, e.y) -> {
                    speedIndicatorView.onTap(e)
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

        gaugeBGPaint.shader = BitmapShader((context.getDrawable(R.drawable.gauge_bg) as BitmapDrawable).bitmap,
                Shader.TileMode.REPEAT,
                Shader.TileMode.REPEAT)
        gaugeBGPaint.style = Paint.Style.FILL

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

        addOnLayoutChangeListener(object : OnLayoutChangeListener {
            override fun onLayoutChange(v: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
                layedOut = true
                removeOnLayoutChangeListener(this)
            }
        })

        //order in which the views are added here is important
        addView(rpmGauge)
        addView(fuelAndTemperatureGauge)
        addView(speedometerGauge)
        addView(speedIndicatorView)
        addView(vinView)
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        this.gestureDetector.onTouchEvent(event)
        return true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = (right - left).toFloat()
        val height = (bottom - top).toFloat()
        val remainingWidthAfterPadding = width - paddingLeft.toFloat() - paddingRight.toFloat()
        val remainingHeightAfterPadding = height - paddingTop.toFloat() - paddingBottom.toFloat()
        //todo view's height should be at least 45% of width, which is middle gauge diameter requirement or I should consider height into consideration before finalizing gauges diameter
        viewCenter.x = width / 2
        viewCenter.y = height / 2

        middleGaugeRadius = remainingWidthAfterPadding * MIDDLE_GAUGE_WIDTH_PERCENTAGE / 2
        sideGaugeRadius = remainingWidthAfterPadding * LEFT_GAUGE_WIDTH_PERCENTAGE / 2

        middleGaugeBounds.left = viewCenter.x - middleGaugeRadius
        middleGaugeBounds.top = viewCenter.y - middleGaugeRadius
        middleGaugeBounds.right = viewCenter.x + middleGaugeRadius
        middleGaugeBounds.bottom = viewCenter.y + middleGaugeRadius

        val leftGaugeStartPoint = Math.ceil(middleGaugeBounds.left - sideGaugeRadius * Math.sqrt(2.0)).toInt()
        leftGaugeBounds.left = leftGaugeStartPoint.toFloat()
        leftGaugeBounds.top = viewCenter.y - sideGaugeRadius
        leftGaugeBounds.right = leftGaugeStartPoint + 2 * sideGaugeRadius
        leftGaugeBounds.bottom = viewCenter.y + sideGaugeRadius

        leftGaugeBoundsToBeShownCompletely.set(leftGaugeBounds)

        val rightGaugeEndpoint = Math.ceil(middleGaugeBounds.right + sideGaugeRadius * Math.sqrt(2.0)).toInt()
        rightGaugeBounds.left = rightGaugeEndpoint - 2 * sideGaugeRadius
        rightGaugeBounds.top = viewCenter.y - sideGaugeRadius
        rightGaugeBounds.right = rightGaugeEndpoint.toFloat()
        rightGaugeBounds.bottom = viewCenter.y + sideGaugeRadius

        rightGaugeBoundsToBeShownCompletely.set(rightGaugeBounds)

        if (!showSideGauges) {
            rightGaugeBounds.offset(-rightGaugeBounds.width(), 0f)
            leftGaugeBounds.offset(leftGaugeBounds.width(), 0f)
        }


        val availableHeight = (remainingHeightAfterPadding - (2 * sideGaugeRadius)) / 2
        val margin = availableHeight * DASHBOARD_LABEL_MARGIN_PERCENTAGE_OF_AVAILABLE_HEIGHT
        labelBoundsOnCanvas.left = margin
        labelBoundsOnCanvas.top = viewCenter.y + sideGaugeRadius + margin
        labelBoundsOnCanvas.right = labelBound.width().toFloat()
        labelBoundsOnCanvas.bottom = viewCenter.y + sideGaugeRadius + availableHeight - margin


        for (index in 0..childCount) {
            when (val child = getChildAt(index)) {
                is SpeedometerGaugeView -> {
                    child.layout(middleGaugeBounds.left.toInt(),
                            middleGaugeBounds.top.toInt(),
                            middleGaugeBounds.right.toInt(),
                            middleGaugeBounds.bottom.toInt())
                }

                is SpeedIndicatorView -> {
                    child.layout(middleGaugeBounds.left.toInt(),
                            middleGaugeBounds.top.toInt(),
                            middleGaugeBounds.right.toInt(),
                            middleGaugeBounds.bottom.toInt())
                }

                is RPMGaugeView -> {
                    child.layout(leftGaugeBounds.left.toInt(),
                            leftGaugeBounds.top.toInt(),
                            leftGaugeBounds.right.toInt(),
                            leftGaugeBounds.bottom.toInt())
                }
                is FuelAndTemperatureGaugeView -> {
                    val sideAndMiddleGaugeIntersectionLength = Math.abs(rightGaugeBoundsToBeShownCompletely.left - middleGaugeBounds.right)
                    child.onBoundChanged(rightGaugeBounds,
                            middleGaugeRadius,
                            sideAndMiddleGaugeIntersectionLength)

                    child.layout(rightGaugeBounds.left.toInt(),
                            rightGaugeBounds.top.toInt(),
                            rightGaugeBounds.right.toInt(),
                            rightGaugeBounds.bottom.toInt())
                }
                is VINView -> {
                    child.layout(middleGaugeBounds.left.toInt(),
                            middleGaugeBounds.top.toInt(),
                            middleGaugeBounds.right.toInt(),
                            middleGaugeBounds.bottom.toInt())
                }
            }

        }
    }


    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
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


    private fun adoptOnlineStatus() {
        if (online) {
            speedometerGauge.onConnected()
            speedIndicatorView.onConnected()
            rpmGauge.onConnected()
            fuelAndTemperatureGauge.onConnected()
            vinView.onConnected()
        } else {
            speedometerGauge.onDisconnected()
            speedIndicatorView.onDisconnected()
            rpmGauge.onDisconnected()
            fuelAndTemperatureGauge.onDisconnected()
            vinView.onDisconnected()
        }
    }

    private fun showSideGauges() {
        startSideGaugeAnimation(true)
    }

    private fun hideSideGauges() {
        startSideGaugeAnimation(false)
    }


    private fun startSideGaugeAnimation(show: Boolean) {
        val animatorSet = AnimatorSet()
        val rpmGaugeAnimator = ObjectAnimator.ofFloat(rpmGauge,
                "translationX",
                rpmGauge.width.toFloat() * (if (show) -1 else 1))
        val fuelGaugeAnimator = ObjectAnimator.ofFloat(fuelAndTemperatureGauge,
                "translationX",
                fuelAndTemperatureGauge.width.toFloat() * (if (show) 1 else -1))

        animatorSet.playTogether(rpmGaugeAnimator,
                fuelGaugeAnimator)

        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                animatingSideGauges = true
            }

            override fun onAnimationEnd(animation: Animator?) {
                animatingSideGauges = false
            }
        })
        animatorSet.start()
    }

    fun setOnRPMChangedListener(listener: (Float) -> Unit) {
        rpmGauge.rpmChangedListener = listener
    }

    fun setOnSpeedChangedListener(listener: (Float) -> Unit) {
        speedIndicatorView.speedChangedListener = listener
    }

    fun setOnFuelPercentageChangedListener(listener: (Float) -> Unit) {
        fuelAndTemperatureGauge.fuelPercentageChangedListener = listener
    }

    fun setOnCheckEngineLightChangedListener(listener: (Boolean) -> Unit) {
        speedIndicatorView.checkEngineLightChangedListener = listener
    }

    fun setOnIgnitionChangedListener(listener: (Boolean) -> Unit) {
        speedIndicatorView.ignitionIconChangedListener = listener
    }


    fun setOnRPMGaugeCLickListener(listener: (Float) -> Unit) {
        rpmGauge.onRPMClickListener = listener

    }

    fun setOnIgnitionIconCLickListener(listener: (Boolean) -> Unit) {
        speedIndicatorView.onIgnitionIconClickListener = listener
    }

    fun setOnCheckEngineLightIconCLickListener(listener: (Boolean) -> Unit) {
        speedIndicatorView.onCheckEngineLightIconClickListener = listener
    }

    fun setOnSpeedStripClickListener(listener: (Float) -> Unit) {
        speedIndicatorView.onSpeedStripClickListener = listener
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
