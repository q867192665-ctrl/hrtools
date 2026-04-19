package com.example.ipv6communicator

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class SignatureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private var isFullScreenMode = false
    private var strokeWidth = 8f
    
    private val paint: Paint = Paint().apply {
        color = Color.BLACK
        strokeWidth = this@SignatureView.strokeWidth
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    private var lastX = 0f
    private var lastY = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            canvas = Canvas(bitmap!!)
            canvas?.drawColor(Color.WHITE)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let { 
            canvas.drawBitmap(it, 0f, 0f, null)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = x
                lastY = y
            }
            MotionEvent.ACTION_MOVE -> {
                canvas?.drawLine(lastX, lastY, x, y, paint)
                lastX = x
                lastY = y
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                // 签名完成
            }
        }
        return true
    }

    fun setFullScreenMode(fullScreen: Boolean) {
        isFullScreenMode = fullScreen
        if (fullScreen) {
            strokeWidth = 12f  // 全屏模式下线条更粗
        } else {
            strokeWidth = 8f
        }
        paint.strokeWidth = strokeWidth
        invalidate()
    }

    fun clear() {
        bitmap = null
        canvas = null
        invalidate()
        // 重新初始化
        if (width > 0 && height > 0) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            canvas = Canvas(bitmap!!)
            canvas?.drawColor(Color.WHITE)
            invalidate()
        }
    }

    fun getSignatureBitmap(): Bitmap? {
        return bitmap
    }

    fun hasSignature(): Boolean {
        return bitmap?.let { 
            // 检查bitmap是否包含非白色像素
            for (x in 0 until it.width) {
                for (y in 0 until it.height) {
                    if (it.getPixel(x, y) != Color.WHITE) {
                        return true
                    }
                }
            }
            false
        } ?: false
    }
}