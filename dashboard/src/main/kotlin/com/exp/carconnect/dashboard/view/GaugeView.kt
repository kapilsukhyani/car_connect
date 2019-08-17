package com.exp.carconnect.dashboard.view

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.support.annotation.CallSuper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

internal abstract class DashboardBasicView(context: Context,
                                           attrs: AttributeSet? = null,
                                           defStyleAttr: Int = 0,
                                           defStyleRes: Int = -1,
                                           var onlineColor: Int,
                                           val offlineColor: Int) : View(context,
        attrs,
        defStyleAttr,
        defStyleRes) {
    companion object {
        const val GAUGE_STROKE_WIDTH = 20f
    }

    protected val gaugePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    var connected: Boolean = false
        set(value) {
            if (value) {
                onConnected()
            } else {
                onDisconnected()
            }
            field = value
        }

    init {
        gaugePaint.style = Paint.Style.STROKE
        gaugePaint.strokeWidth = GAUGE_STROKE_WIDTH
//        gaugePaint.maskFilter = BlurMaskFilter(30f, BlurMaskFilter.Blur.OUTER)
        gaugePaint.maskFilter = EmbossMaskFilter(floatArrayOf(1f, 5f, 1f), 0.8f, 6.0f, 20.0f)
    }

    @CallSuper
    open fun onConnected() {
        gaugePaint.color = onlineColor
    }

    @CallSuper
    open fun onDisconnected() {
        gaugePaint.color = offlineColor
    }

    internal fun setOnlineColor(onlineColor: Int) {
        this.onlineColor = onlineColor
    }

    protected val gaugeBounds: RectF = RectF()
    private val viewCenter: PointF = PointF()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        gaugeBounds.left = 0.0f + paddingLeft.toFloat() + (GAUGE_STROKE_WIDTH / 2)
        gaugeBounds.top = 0.0f + paddingTop.toFloat() + (GAUGE_STROKE_WIDTH / 2)
        gaugeBounds.right = w.toFloat() - paddingRight.toFloat() - (GAUGE_STROKE_WIDTH / 2)
        gaugeBounds.bottom = h.toFloat() - paddingBottom.toFloat() - (GAUGE_STROKE_WIDTH / 2)
        onBoundChanged(gaugeBounds)
    }

    abstract fun onBoundChanged(bounds: RectF)

    open fun onTap(event: MotionEvent): Boolean {
        return false
    }
}


internal abstract class GaugeView(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = -1,
        onlineColor: Int,
        offlineColor: Int,
        private val gaugeBackgroundDrawable: Drawable) : DashboardBasicView(context,
        attrs,
        defStyleAttr,
        defStyleRes,
        onlineColor,
        offlineColor) {


    abstract fun drawGauge(canvas: Canvas, bounds: RectF)

    private val textBounds = Rect()
    private val textDrawingBounds = Rect()
    private val lineBounds = RectF()

    internal fun drawTicks(canvas: Canvas,
                           bounds: RectF,
                           startDegree: Float,
                           tickGapInDegrees: Float,
                           totalNoOfTicks: Int,
                           bigTickMultiple: Int,
                           bigTickLength: Float,
                           bigTickWidth: Float,
                           smallTickLength: Float,
                           smallTickWidth: Float,
                           tickMarkerStart: Int,
                           tickMarkerDiff: Int,
                           textSize: Float,
                           textMarkerMargin: Float,
                           tickTextPaint: Paint,
                           tickPaint: Paint,
                           drawCriticalZone: Boolean = false,
                           criticalZoneDegreesAtEnd: Float = 0f,
                           criticalZoneColor: Int = -1) {
        val endDegree = startDegree + (totalNoOfTicks - 1) * tickGapInDegrees

        tickTextPaint.textSize = textSize
        canvas.save()
        var tickLength: Float
        var tickMarker = tickMarkerStart
        var tickMarkerText: String
        var totalRotationSoFar = startDegree


        var lineYBound = 0F
        canvas.rotate(startDegree, bounds.centerX(), bounds.centerY())

        val originalTickColor = tickPaint.color
        val originalTickTextColor = tickTextPaint.color
        for (i in 0 until totalNoOfTicks) {

            if (i % bigTickMultiple == 0) {
                tickLength = bigTickLength
                tickPaint.strokeWidth = bigTickWidth
            } else {
                tickLength = smallTickLength
                tickPaint.strokeWidth = smallTickWidth
            }

            //make tick displayed inside the gauge completely
            lineYBound = bounds.centerY()
            if (i == 0) {
                lineYBound += tickPaint.strokeWidth / 2
            }
            if (i == totalNoOfTicks - 1) {
                lineYBound -= tickPaint.strokeWidth / 2
            }


            lineBounds.set(bounds.right - tickLength, lineYBound, bounds.right, lineYBound)
            canvas.drawLine(lineBounds.left, lineBounds.top, lineBounds.right, lineBounds.bottom, tickPaint)

            if (i % bigTickMultiple == 0) {
                canvas.save()
                tickMarkerText = tickMarker.toString() + ""
                tickTextPaint.getTextBounds(tickMarkerText, 0, tickMarkerText.length, textBounds)

                textDrawingBounds.set((lineBounds.left - textBounds.width().toFloat() - textMarkerMargin).toInt(), (bounds.centerY() - textBounds.height() / 2).toInt(),
                        (lineBounds.left - textMarkerMargin).toInt(), bounds.centerY().toInt() + textBounds.height() / 2)
                canvas.rotate(-totalRotationSoFar, textDrawingBounds.centerX().toFloat(), textDrawingBounds.centerY().toFloat())
                canvas.drawText(tickMarkerText, textDrawingBounds.centerX().toFloat(), textDrawingBounds.bottom.toFloat(), tickTextPaint)
                tickMarker += tickMarkerDiff
                canvas.restore()

            }

            canvas.rotate(tickGapInDegrees, bounds.centerX(), bounds.centerY())
            totalRotationSoFar += tickGapInDegrees

            if (drawCriticalZone && (endDegree - totalRotationSoFar <= criticalZoneDegreesAtEnd)) {
                tickPaint.color = criticalZoneColor
                tickTextPaint.color = criticalZoneColor
            }


        }
        tickPaint.color = originalTickColor
        tickTextPaint.color = originalTickTextColor

        canvas.restore()
    }


    internal fun getDarkerColor(originalColor: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(originalColor, hsv)
        hsv[2] *= 0.6f // value globalComponent
        return Color.HSVToColor(hsv)
    }

    @CallSuper
    override fun onDraw(canvas: Canvas) {
        gaugeBackgroundDrawable.setBounds(gaugeBounds.left.toInt(),
                gaugeBounds.top.toInt(),
                gaugeBounds.right.toInt(),
                gaugeBounds.bottom.toInt())
        gaugeBackgroundDrawable.draw(canvas)
    }
}

internal abstract class MiddleGaugeView(context: Context,
                                        attrs: AttributeSet? = null,
                                        defStyleAttr: Int = 0,
                                        defStyleRes: Int = -1,
                                        onlineColor: Int,
                                        offlineColor: Int,
                                        gaugeBackgroundDrawable: Drawable) : GaugeView(context,
        attrs,
        defStyleAttr,
        defStyleRes,
        onlineColor,
        offlineColor,
        gaugeBackgroundDrawable)

internal abstract class LeftGaugeView(context: Context,
                                      attrs: AttributeSet? = null,
                                      defStyleAttr: Int = 0,
                                      defStyleRes: Int = -1,
                                      onlineColor: Int,
                                      offlineColor: Int,
                                      gaugeBackgroundDrawable: Drawable) : GaugeView(context,
        attrs,
        defStyleAttr,
        defStyleRes,
        onlineColor,
        offlineColor,
        gaugeBackgroundDrawable)

internal abstract class RightGaugeView(context: Context,
                                       attrs: AttributeSet? = null,
                                       defStyleAttr: Int = 0,
                                       defStyleRes: Int = -1,
                                       onlineColor: Int,
                                       offlineColor: Int,
                                       gaugeBackgroundDrawable: Drawable) : GaugeView(context,
        attrs,
        defStyleAttr,
        defStyleRes,
        onlineColor,
        offlineColor,
        gaugeBackgroundDrawable)