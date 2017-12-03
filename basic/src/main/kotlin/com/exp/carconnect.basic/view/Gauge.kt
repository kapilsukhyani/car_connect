package com.exp.carconnect.basic.view

import android.graphics.*


internal abstract class Gauge(val onlineColor: Int,
                              val offlineColor: Int) {
    companion object {
         val GAUGE_STROKE_WIDTH = 20f
    }

    protected val gaugePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG);

    var connected: Boolean = false
        set(value) {
            if (value) {
                onConnected()
            } else {
                onDisconnected()
            }
        }

    init {
//        if (connected) {
//            onConnected()
//        } else {
//            onDisconnected()
//        }


        gaugePaint.style = Paint.Style.STROKE
        gaugePaint.strokeWidth = GAUGE_STROKE_WIDTH
        gaugePaint.maskFilter = BlurMaskFilter(.5f, BlurMaskFilter.Blur.OUTER)
    }

    open fun onConnected() {
        gaugePaint.color = onlineColor

    }

    open fun onDisconnected() {
        gaugePaint.color = offlineColor

    }

    abstract fun drawGauge(canvas: Canvas, bounds: RectF)

    internal fun drawTicks(canvas: Canvas, bounds: RectF,
                           startDegree: Float, tickGapInDegrees: Float,
                           totalNoOfTicks: Int, bigTickMultiple: Int,
                           bigTickLength: Float, bigTickWidth: Float,
                           smallTickLength: Float, smallTickWidth: Float,
                           tickMarkerStart: Int, tickMarkerDiff: Int,
                           textSize: Float, textMarkerMargin: Float,
                           tickTextPaint: Paint,
                           tickPaint: Paint) {
        tickTextPaint.textSize = textSize
        canvas.save()
        var tickLength: Float
        var tickMarker = tickMarkerStart
        var tickMarkerText: String
        var totalRotationSoFar = startDegree

        val textBounds = Rect()
        val textDrawingBounds = Rect()
        val lineBounds = RectF()

        canvas.rotate(startDegree, bounds.centerX(), bounds.centerY())

        for (i in 0 until totalNoOfTicks) {
            if (i % bigTickMultiple == 0) {
                tickLength = bigTickLength
                tickPaint.strokeWidth = bigTickWidth
            } else {
                tickLength = smallTickLength
                tickPaint.strokeWidth = smallTickWidth
            }
            lineBounds.set(bounds.right - tickLength, bounds.centerY(), bounds.right, bounds.centerY())
            canvas.drawLine(lineBounds.left, lineBounds.top, lineBounds.right, lineBounds.bottom, tickPaint)

            if (i % bigTickMultiple == 0) {
                canvas.save()
                tickMarkerText = tickMarker.toString() + ""
                tickTextPaint.getTextBounds(tickMarkerText, 0, tickMarkerText.length, textBounds)

                textDrawingBounds.set((lineBounds.left - textBounds.width().toFloat() - textMarkerMargin).toInt(), (bounds.centerY() - textBounds.height() / 2).toInt(),
                        (lineBounds.left - textMarkerMargin).toInt(), bounds.centerY().toInt())
                canvas.rotate(-totalRotationSoFar, textDrawingBounds.centerX().toFloat(), textDrawingBounds.centerY().toFloat())
                canvas.drawText(tickMarkerText, textDrawingBounds.centerX().toFloat(), textDrawingBounds.centerY().toFloat(), tickTextPaint)
                tickMarker += tickMarkerDiff
                canvas.restore()

            }

            canvas.rotate(tickGapInDegrees, bounds.centerX(), bounds.centerY())
            totalRotationSoFar += tickGapInDegrees


        }
        canvas.restore()
    }
}

internal abstract class MiddleGauge(onlineColor: Int, offlineColor: Int) : Gauge(onlineColor, offlineColor)

internal abstract class LeftGauge(onlineColor: Int, offlineColor: Int) : Gauge(onlineColor, offlineColor)

internal abstract class RightGauge(onlineColor: Int, offlineColor: Int) : Gauge(onlineColor, offlineColor)