package com.exp.carconnect.dashboard.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import java.util.*

class VINView(context: Context,
              attrs: AttributeSet? = null,
              defStyleAttr: Int = 0,
              defStyleRes: Int = -1) : View(context,
        attrs,
        defStyleAttr,
        defStyleRes) {

    companion object {
        private const val VIN_START_ANGLE = 130f
        private const val VIN_SWEEP = 85f
        private const val VIN_LENGTH = 17
    }


    var vin = "-------VIN-------"
        set(value) {
            if (value.length == 17 && value != field) {
                field = value
                invalidate()
            }
        }

    private val vinCharGapInDegrees = VIN_SWEEP / VIN_LENGTH
    private val vinPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var vinCharSize: Float = 0.0f
    private val vinOnlineColor = context.getColor(android.R.color.holo_orange_dark)
    private val vinOfflineColor = context.getColor(android.R.color.white)

    init {
        vinPaint.textLocale = Locale.US
        vinPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        vinPaint.textAlign = Paint.Align.CENTER
    }

    fun onConnected() {
        vinPaint.color = vinOnlineColor
    }

    fun onDisconnected() {
        vinPaint.color = vinOfflineColor
    }

    override fun onDraw(canvas: Canvas) {
        drawVin(canvas, gaugeBounds)
    }

    private lateinit var gaugeBounds: RectF
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        gaugeBounds = RectF(0.0f, 0.0f, w.toFloat(), h.toFloat())
        val middleGaugeCircumference: Float = (Math.PI * gaugeBounds.width()).toFloat()
        val areaToDrawVinText = middleGaugeCircumference * (VIN_SWEEP / 360f)
        vinCharSize = areaToDrawVinText / VIN_LENGTH
    }


    private fun drawVin(canvas: Canvas, middleGaugeBounds: RectF) {
        if (vin.isNotEmpty()) {
            vinPaint.textSize = vinCharSize
            var totalRotationSoFar = VIN_START_ANGLE
            canvas.save()
            canvas.rotate(VIN_START_ANGLE,
                    middleGaugeBounds.centerX(),
                    middleGaugeBounds.centerY())
            val x: Float = middleGaugeBounds.centerX() + (middleGaugeBounds.width() / 2)
            val y: Float = middleGaugeBounds.centerY()
            for (i in 0 until 17) {
                canvas.save()
                canvas.rotate(-totalRotationSoFar, x, y)
                canvas.drawText(vin.substring(i, i + 1), x, y, vinPaint)
                canvas.restore()

                canvas.rotate(-vinCharGapInDegrees,
                        middleGaugeBounds.centerX(),
                        middleGaugeBounds.centerY())
                totalRotationSoFar -= vinCharGapInDegrees
            }

            canvas.restore()
        }

    }
}