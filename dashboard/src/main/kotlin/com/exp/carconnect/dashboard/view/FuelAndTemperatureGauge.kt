package com.exp.carconnect.dashboard.view

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.*
import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v4.graphics.ColorUtils
import android.text.TextPaint
import android.view.MotionEvent
import com.exp.carconnect.base.R
import java.util.*


internal class FuelAndTemperatureGauge(dashboard: Dashboard,
                                       private val startAngle: Float,
                                       private val sweep: Float,
                                       currentFuelPercentage: Float,
                                       currentAirIntakeTemp: Float,
                                       currentAmbientTemp: Float,
                                       onlineColor: Int,
                                       offlineColor: Int) : RightGauge(dashboard, onlineColor, offlineColor) {
    companion object {
        private const val FUEL_GAUGE_START_ANGLE = 70
        private const val FUEL_GAUGE_SWEEP_ANGLE = -140
        private const val FUEL_GAUGE_BOUNDARY_STROKE_WIDTH = 10f
        private const val FUEL_GAUGE_DEFAULT_WIDTH_PERCENTAGE = .15f

        private const val MAX_NO_OF_TEMPERATURE_DOTTS = 4
        private const val TEMPERATURE_PERCENTAGE_COVERED_PER_DOTT = 1f / MAX_NO_OF_TEMPERATURE_DOTTS.toFloat()
        private const val TEMPERATURE_DOTT_DISTANCE_FROM_MIDDLE_GAUGE_PERCENTAGE = 0.05f
        private const val TEMPERATURE_TEXT_SIZE_PERCENTAGE = .08f
        private const val AIR_INTAKE_TEXT = "Air-Intake"
        private const val AMBIENT_TEXT = "Ambient"
        private const val CELSIUS = "C"

        private const val MAXIMUM_AIR_INTAKE_TEMPERATURE = 65f // 60 degrees celsius
        private const val MAXIMUM_AMBIENT_TEMPERATURE = 55f // 50 degrees celsius
        private const val TEMPERATURE_ROTATION_DEGREES_PERCENTAGE_OVER_TEMPERATURE_CIRCLE_RADIUS = .015f
        private const val SMALLEST_TEMPERATURE_DOTT_RADIUS_PERCENTAGE_OVER_WIDTH = .008f
    }

    private val fuelGaugePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val temperatureDottActivePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val temperatureDottInActivePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fuelGaugeBoundary = Paint(Paint.ANTI_ALIAS_FLAG)
    private val temperatureTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)


    private val fuelIcon = VectorDrawableCompat.create(dashboard.context.resources, R.drawable.ic_local_gas_station_black_24dp, null)!!


    internal var fuelPercentageChangedListener: ((Float) -> Unit)? = null
    //todo implement following listeners
    internal var airIntakeTempChangedListener: ((Float) -> Unit)? = null
    internal var ambientTempChangedListener: ((Float) -> Unit)? = null

    internal var onFuelIconCLickListener: ((Float) -> Unit)? = null
    internal var onAirIntakeTempClickListener: ((Float) -> Unit)? = null
    internal var onAmbientTempClickListener: ((Float) -> Unit)? = null


    private var currentFuelPercentage = currentFuelPercentage
        set(value) {
            field = if (value == 0.0f) {
                .01f
            } else {
                value
            }
            dashboard.invalidateFuelGauge()
            fuelPercentageChangedListener?.invoke(field)
        }

    internal var currentAirIntakeTemperature = currentAirIntakeTemp
        set(value) {
            field = value
            dashboard.invalidateFuelGauge()
        }

    internal var currentAmbientTemperature = currentAmbientTemp
        set(value) {
            field = value
            dashboard.invalidateFuelGauge()
        }

    private var termperatureDottsRotationDegrees = 0.0f
    private var smallestTemperatureDottRadius = 0.0f
    private lateinit var airIntakeLabelBounds: RectF
    private lateinit var currentAirIntakeTextStartingPoint: PointF
    private lateinit var ambientLabelBounds: RectF
    private lateinit var currentAmbientTextStartingPoint: PointF
    private lateinit var temperatureDottCenter: PointF

    private lateinit var ambientTouchBounds: RectF
    private lateinit var airIntakeTouchBounds: RectF


    private lateinit var fuelGaugeInnerBound: RectF
    private lateinit var fuelGaugeOuterBound: RectF
    private lateinit var fuelIconBounds: Rect
    private lateinit var boundsAfterStrokeWidth: RectF
    private var temperatureGaugeCircleRadius: Float = 0.0f
    private var temperatureCircleDistanceFromLeftEdge: Float = 0f
    private lateinit var temperatureGaugeCircleCenter: PointF

    private var currentFuelAnimator: ObjectAnimator? = null

    init {

        temperatureTextPaint.textLocale = Locale.US
        temperatureTextPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        fuelGaugePaint.style = Paint.Style.STROKE
        fuelGaugePaint.pathEffect = DashPathEffect(floatArrayOf(40f, 15f), 0f)


        fuelGaugeBoundary.style = Paint.Style.STROKE
        fuelGaugeBoundary.strokeWidth = FUEL_GAUGE_BOUNDARY_STROKE_WIDTH

        temperatureDottActivePaint.style = Paint.Style.FILL
        temperatureDottInActivePaint.style = Paint.Style.FILL

    }

    override fun onConnected() {
        super.onConnected()

        fuelGaugeBoundary.color = onlineColor
        fuelGaugePaint.color = onlineColor
        fuelIcon.setTint(onlineColor)

        temperatureTextPaint.color = onlineColor
        temperatureDottInActivePaint.color = onlineColor

    }

    override fun onDisconnected() {
        super.onDisconnected()

        fuelGaugeBoundary.color = offlineColor
        fuelGaugePaint.color = offlineColor
        fuelIcon.setTint(offlineColor)

        temperatureTextPaint.color = offlineColor
        temperatureDottInActivePaint.color = offlineColor

    }


    override fun onBoundChanged(bounds: RectF) {
        boundsAfterStrokeWidth = RectF(bounds)
        boundsAfterStrokeWidth.inset(GAUGE_STROKE_WIDTH / 2, GAUGE_STROKE_WIDTH / 2)

        val fuelGaugeWidth = bounds.width() * FUEL_GAUGE_DEFAULT_WIDTH_PERCENTAGE

        fuelGaugeInnerBound = RectF(bounds)
        fuelGaugeInnerBound.inset(fuelGaugeWidth + FUEL_GAUGE_BOUNDARY_STROKE_WIDTH / 2, fuelGaugeWidth + FUEL_GAUGE_BOUNDARY_STROKE_WIDTH / 2)

        fuelGaugeOuterBound = RectF(bounds)
        fuelGaugePaint.strokeWidth = fuelGaugeWidth
        fuelGaugeOuterBound.inset(fuelGaugeWidth / 2, fuelGaugeWidth / 2)

        val fuelIndicatorDimen = fuelGaugeWidth * .8f
        val fuelIndicatorMarginFromGauge = fuelGaugeWidth * .2f
        fuelIconBounds = Rect((bounds.centerX() - fuelIndicatorDimen / 2).toInt(),
                (bounds.bottom - (fuelIndicatorDimen + fuelIndicatorMarginFromGauge)).toInt(),
                (bounds.centerX() + fuelIndicatorDimen / 2).toInt(),
                (bounds.bottom - fuelIndicatorMarginFromGauge).toInt())

        // temperature gauge measurements

        val textBound = Rect()
        val currentTempTextBound = Rect()

        temperatureGaugeCircleCenter = PointF(boundsAfterStrokeWidth.left - temperatureCircleDistanceFromLeftEdge, boundsAfterStrokeWidth.centerY())
        temperatureTextPaint.textSize = boundsAfterStrokeWidth.width() * TEMPERATURE_TEXT_SIZE_PERCENTAGE
        smallestTemperatureDottRadius = boundsAfterStrokeWidth.width() * SMALLEST_TEMPERATURE_DOTT_RADIUS_PERCENTAGE_OVER_WIDTH

        val temperatureTextBoundLeftEdge = (temperatureGaugeCircleRadius + temperatureGaugeCircleCenter.x + 2 * smallestTemperatureDottRadius * MAX_NO_OF_TEMPERATURE_DOTTS)

        //todo improve text bound calculation, touch is not getting registered properly as of now
        temperatureTextPaint.getTextBounds(AIR_INTAKE_TEXT, 0, AIR_INTAKE_TEXT.length, textBound)
        val currentAirIntakeTempString = String.format("%.1f", currentAirIntakeTemperature) + " " + CELSIUS
        temperatureTextPaint.getTextBounds(currentAirIntakeTempString, 0, currentAirIntakeTempString.length, currentTempTextBound)
        textBound.offsetTo(temperatureTextBoundLeftEdge.toInt(), temperatureGaugeCircleCenter.y.toInt())

        airIntakeLabelBounds = RectF(textBound)
        currentAirIntakeTextStartingPoint = PointF(textBound.left.toFloat(), textBound.top - currentTempTextBound.height().toFloat())
        airIntakeTouchBounds = RectF(currentAirIntakeTextStartingPoint.x, currentAirIntakeTextStartingPoint.y - currentTempTextBound.height().toFloat(),
                airIntakeLabelBounds.right, airIntakeLabelBounds.bottom)

        temperatureTextPaint.getTextBounds(AMBIENT_TEXT, 0, AMBIENT_TEXT.length, textBound)
        val currentAmbientTempString = String.format("%.1f", currentAmbientTemperature) + " " + CELSIUS
        temperatureTextPaint.getTextBounds(currentAmbientTempString, 0, currentAmbientTempString.length, currentTempTextBound)
        textBound.offsetTo(temperatureTextBoundLeftEdge.toInt(), temperatureGaugeCircleCenter.y.toInt())

        ambientLabelBounds = RectF(textBound)
        currentAmbientTextStartingPoint = PointF(textBound.left.toFloat(),
                textBound.bottom + currentTempTextBound.height().toFloat())
        ambientTouchBounds = RectF(ambientLabelBounds.left, ambientLabelBounds.top,
                ambientLabelBounds.right, ambientLabelBounds.bottom + currentAmbientTextStartingPoint.y)

        temperatureDottCenter = PointF(temperatureGaugeCircleRadius + temperatureGaugeCircleCenter.x, temperatureGaugeCircleCenter.y)

    }


    internal fun onBoundChanged(bounds: RectF, middleGaugeRadius: Float, sideAndMiddleGaugeIntersectionLength: Float) {
        temperatureGaugeCircleRadius = middleGaugeRadius + bounds.width() * TEMPERATURE_DOTT_DISTANCE_FROM_MIDDLE_GAUGE_PERCENTAGE
        temperatureCircleDistanceFromLeftEdge = (middleGaugeRadius - sideAndMiddleGaugeIntersectionLength)
        termperatureDottsRotationDegrees = temperatureGaugeCircleRadius * TEMPERATURE_ROTATION_DEGREES_PERCENTAGE_OVER_TEMPERATURE_CIRCLE_RADIUS
        onBoundChanged(bounds)

    }


    override fun drawGauge(canvas: Canvas, bounds: RectF) {
        canvas.drawArc(bounds, startAngle, sweep, false, gaugePaint)
        drawFuelGauge(canvas, boundsAfterStrokeWidth)
        drawTemperatureGauges(canvas)

    }


    private fun drawFuelGauge(canvas: Canvas, bounds: RectF) {

        canvas.drawArc(fuelGaugeInnerBound, FUEL_GAUGE_START_ANGLE.toFloat(), FUEL_GAUGE_SWEEP_ANGLE.toFloat(), false, fuelGaugeBoundary)

        val sweepAngle = Math.abs(FUEL_GAUGE_SWEEP_ANGLE) * currentFuelPercentage
        canvas.drawArc(fuelGaugeOuterBound, FUEL_GAUGE_START_ANGLE.toFloat(), -sweepAngle, false, fuelGaugePaint)

        fuelIcon.setTint(ColorUtils.blendARGB(gaugePaint.color, Color.RED, 1 - currentFuelPercentage))
        fuelIcon.bounds = fuelIconBounds
        fuelIcon.draw(canvas)

    }

    private fun drawTemperatureGauges(canvas: Canvas) {

        val currentAirIntakeTempOverMax = currentAirIntakeTemperature / MAXIMUM_AIR_INTAKE_TEMPERATURE
        val currentAmbientTempOverMax = currentAmbientTemperature / MAXIMUM_AMBIENT_TEMPERATURE

        val ambientActiveDottColor = ColorUtils.blendARGB(gaugePaint.color, Color.RED, currentAmbientTempOverMax)
        val airIntakeActiveDottColor = ColorUtils.blendARGB(gaugePaint.color, Color.RED, currentAirIntakeTempOverMax)

        var totalPercentageSoFar: Float
        var dottRadius: Float

        canvas.save()
        //draw air-intake temperature dotts
        for (dottNumber in MAX_NO_OF_TEMPERATURE_DOTTS downTo 1) {
            canvas.rotate(-termperatureDottsRotationDegrees, temperatureGaugeCircleCenter.x, temperatureGaugeCircleCenter.y)
            totalPercentageSoFar = (MAX_NO_OF_TEMPERATURE_DOTTS - dottNumber + 1) * TEMPERATURE_PERCENTAGE_COVERED_PER_DOTT
            dottRadius = smallestTemperatureDottRadius * dottNumber

            when {
                currentAirIntakeTempOverMax == 0.0f || (currentAirIntakeTempOverMax < totalPercentageSoFar - TEMPERATURE_PERCENTAGE_COVERED_PER_DOTT) -> {
                    canvas.drawCircle(temperatureDottCenter.x, temperatureDottCenter.y,
                            dottRadius, temperatureDottInActivePaint)
                }
                currentAirIntakeTempOverMax >= totalPercentageSoFar -> {
                    temperatureDottActivePaint.color = airIntakeActiveDottColor
                    canvas.drawCircle(temperatureDottCenter.x, temperatureDottCenter.y,
                            dottRadius, temperatureDottActivePaint)
                }
                else -> {
                    val curveAngleToDraw = 1440f * ((currentAirIntakeTempOverMax - (totalPercentageSoFar - TEMPERATURE_PERCENTAGE_COVERED_PER_DOTT)))
                    temperatureDottActivePaint.color = airIntakeActiveDottColor
                    canvas.drawCircle(temperatureDottCenter.x, temperatureDottCenter.y,
                            dottRadius, temperatureDottInActivePaint)
                    canvas.drawArc(temperatureDottCenter.x - dottRadius, temperatureDottCenter.y - dottRadius,
                            temperatureDottCenter.x + dottRadius, temperatureDottCenter.y + dottRadius,
                            90f - (curveAngleToDraw / 2f), curveAngleToDraw, true, temperatureDottActivePaint)
                }
            }



            if (dottNumber == MAX_NO_OF_TEMPERATURE_DOTTS) {
                val currentAirIntakeTempString = String.format("%.1f", currentAirIntakeTemperature) + " " + CELSIUS
                canvas.save()
                canvas.rotate(termperatureDottsRotationDegrees, airIntakeLabelBounds.centerX(), airIntakeLabelBounds.centerY())
                canvas.drawText(AIR_INTAKE_TEXT, airIntakeLabelBounds.left, airIntakeLabelBounds.bottom, temperatureTextPaint)
                canvas.drawText(currentAirIntakeTempString, currentAirIntakeTextStartingPoint.x,
                        currentAirIntakeTextStartingPoint.y, temperatureTextPaint)
                canvas.restore()
            }
        }
        canvas.restore()

        canvas.save()
        //draw ambient temperature dotts
        for (dottNumber in MAX_NO_OF_TEMPERATURE_DOTTS downTo 1) {
            canvas.rotate(termperatureDottsRotationDegrees, temperatureGaugeCircleCenter.x, temperatureGaugeCircleCenter.y)

            totalPercentageSoFar = (MAX_NO_OF_TEMPERATURE_DOTTS - dottNumber + 1) * TEMPERATURE_PERCENTAGE_COVERED_PER_DOTT
            dottRadius = smallestTemperatureDottRadius * dottNumber

            when {
                currentAmbientTempOverMax == 0.0f || currentAmbientTempOverMax < (totalPercentageSoFar - TEMPERATURE_PERCENTAGE_COVERED_PER_DOTT) -> {
                    canvas.drawCircle(temperatureDottCenter.x, temperatureDottCenter.y,
                            dottRadius, temperatureDottInActivePaint)
                }
                currentAmbientTempOverMax >= totalPercentageSoFar -> {
                    temperatureDottActivePaint.color = ambientActiveDottColor
                    canvas.drawCircle(temperatureDottCenter.x, temperatureDottCenter.y,
                            dottRadius, temperatureDottActivePaint)
                }
                else -> {
                    val curveAngleToDraw = 1440f * ((currentAmbientTempOverMax - (totalPercentageSoFar - TEMPERATURE_PERCENTAGE_COVERED_PER_DOTT)))
                    temperatureDottActivePaint.color = ambientActiveDottColor
                    canvas.drawCircle(temperatureDottCenter.x, temperatureDottCenter.y,
                            dottRadius, temperatureDottInActivePaint)
                    canvas.drawArc(temperatureDottCenter.x - dottRadius, temperatureDottCenter.y - dottRadius,
                            temperatureDottCenter.x + dottRadius, temperatureDottCenter.y + dottRadius,
                            -(90f + (curveAngleToDraw / 2f)), curveAngleToDraw, true, temperatureDottActivePaint)
                }
            }


            if (dottNumber == MAX_NO_OF_TEMPERATURE_DOTTS) {
                val currentAmbientTempString = String.format("%.1f", currentAmbientTemperature) + " " + CELSIUS
                canvas.save()
                canvas.rotate(-termperatureDottsRotationDegrees, ambientLabelBounds.centerX(), ambientLabelBounds.centerY())
                canvas.drawText(AMBIENT_TEXT, ambientLabelBounds.left, ambientLabelBounds.top, temperatureTextPaint)
                canvas.drawText(currentAmbientTempString, currentAmbientTextStartingPoint.x,
                        currentAmbientTextStartingPoint.y, temperatureTextPaint)
                canvas.restore()
            }
        }
        canvas.restore()

    }


    @SuppressLint("ObjectAnimatorBinding")
    internal fun updateFuel(fuel: Float) {
        currentFuelAnimator?.cancel()
        var fuelVar = fuel
        if (fuelVar <= 0f) {
            fuelVar = .01f
        }
        currentFuelAnimator = ObjectAnimator.ofFloat(this, "currentFuelPercentage", currentFuelPercentage, fuelVar)
        currentFuelAnimator?.start()
    }

    override fun onTap(event: MotionEvent): Boolean {
        return when {
            fuelIconBounds.contains(event.x.toInt(), event.y.toInt()) -> {
                onFuelIconCLickListener?.invoke(dashboard.fuelPercentage)
                true
            }
            ambientTouchBounds.contains(event.x, event.y) -> {
                onAmbientTempClickListener?.invoke(dashboard.currentAmbientTemp)
                true
            }
            airIntakeTouchBounds.contains(event.x, event.y) -> {
                onAirIntakeTempClickListener?.invoke(dashboard.currentAirIntakeTemp)

                true
            }
            else -> super.onTap(event)
        }
    }

}