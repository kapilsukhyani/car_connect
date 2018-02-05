package com.exp.carconnect.dashboard.view

import android.graphics.*
import android.view.MotionEvent


internal abstract class Gauge(val dashboard: Dashboard,
                              val onlineColor: Int,
                              val offlineColor: Int) {
    companion object {
        val GAUGE_STROKE_WIDTH = 20f
    }

    protected val gaugePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    var connected: Boolean = false
        set(value) {
            if (value) {
                onConnected()
            } else {
                onDisconnected()
            }
        }

    init {
        gaugePaint.style = Paint.Style.STROKE
        gaugePaint.strokeWidth = GAUGE_STROKE_WIDTH
//        gaugePaint.maskFilter = BlurMaskFilter(30f, BlurMaskFilter.Blur.OUTER)
        gaugePaint.maskFilter = EmbossMaskFilter(floatArrayOf(1f, 5f, 1f), 0.8f, 6.0f, 20.0f)
    }

    open fun onConnected() {
        gaugePaint.color = onlineColor

    }

    open fun onDisconnected() {
        gaugePaint.color = offlineColor

    }

    abstract fun onBoundChanged(bounds: RectF)

    abstract fun drawGauge(canvas: Canvas, bounds: RectF)

    internal fun drawTicks(canvas: Canvas, bounds: RectF,
                           startDegree: Float, tickGapInDegrees: Float,
                           totalNoOfTicks: Int, bigTickMultiple: Int,
                           bigTickLength: Float, bigTickWidth: Float,
                           smallTickLength: Float, smallTickWidth: Float,
                           tickMarkerStart: Int, tickMarkerDiff: Int,
                           textSize: Float, textMarkerMargin: Float,
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

        val textBounds = Rect()
        val textDrawingBounds = Rect()
        val lineBounds = RectF()
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

    open fun onTap(event: MotionEvent): Boolean {
        return false
    }
}

internal abstract class MiddleGauge(dashboard: Dashboard, onlineColor: Int, offlineColor: Int) : Gauge(dashboard, onlineColor, offlineColor)

internal abstract class LeftGauge(dashboard: Dashboard, onlineColor: Int, offlineColor: Int) : Gauge(dashboard, onlineColor, offlineColor)

internal abstract class RightGauge(dashboard: Dashboard, onlineColor: Int, offlineColor: Int) : Gauge(dashboard, onlineColor, offlineColor)