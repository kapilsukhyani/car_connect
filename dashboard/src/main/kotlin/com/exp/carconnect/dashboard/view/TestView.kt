package com.exp.carconnect.dashboard.view

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.View
import com.exp.carconnect.base.R


class TestView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {


    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gaugeBGPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 20f
        paint.color = getContext().getColor(android.R.color.white)
//        paint.setShadowLayer(30f, 0f, 0f, getContext().getColor(android.R.color.holo_green_dark))
//        paint.maskFilter = BlurMaskFilter(30f, BlurMaskFilter.Blur.SOLID)
        paint.maskFilter = EmbossMaskFilter(floatArrayOf(1f, 1f, 1f), 0.4f, 4.0f, 10.0f)

        gaugeBGPaint.shader = BitmapShader((context.getDrawable(R.drawable.gauge_bg) as BitmapDrawable).bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        gaugeBGPaint.style = Paint.Style.FILL

    }

    fun getRainbowColor(index: Int): Int {
        var color: Int = 0
        when (index) {
            0 -> {
                color = context.getColor(android.R.color.holo_purple)
            }
            1 -> {
                color = context.getColor(android.R.color.holo_red_dark)
            }
            2 -> {
                color = context.getColor(android.R.color.holo_green_dark)
            }
            3 -> {
                color = context.getColor(android.R.color.holo_orange_dark)
            }
            4 -> {
                color = context.getColor(android.R.color.white)
            }
        }

        return color
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val width = width
        val height = height
        var length = Math.sqrt((width * width + height * height).toDouble())
        length = 1200.toDouble()

        paint.shader = LinearGradient(0f, 0f, length.toFloat(), length.toFloat(),
                kotlin.IntArray(5, { it -> getRainbowColor(it) }), null, Shader.TileMode.REPEAT)
        canvas!!.drawLine(0f, 0f, width.toFloat(), height.toFloat(), paint)
        canvas.drawCircle(width / 2.toFloat(), height / 2.toFloat(), width * .3.toFloat(), gaugeBGPaint)
    }
}