package com.dress.game.core.custom.drawview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt


class CropView : AppCompatImageView {

    private var paint = Paint()
    private val initial_size = 300
    private var leftTop: Point? = null
    private var rightBottom: Point? = null
    private var center: Point? = null
    private var previous: Point? = null
    private val cropRect = RectF()
    private val prevRect = RectF()
    private var cornerRadius = 0f

    private val DRAG = 0
    private val LEFT = 1
    private val TOP = 2
    private val RIGHT = 3
    private val BOTTOM = 4
    private var activeSide = -1

    private var imageScaledWidth = 0
    private var imageScaledHeight = 0

    var onChanged: ((isResize: Boolean) -> Unit)? = null

    // Adding parent class constructors
    constructor(context: Context) : this(context, null) {
        initCropView()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initCropView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initCropView()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cornerRadius = 12f * resources.displayMetrics.density
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (leftTop!!.equals(0, 0)) resetPoints()
        cropRect.set(leftTop!!.x.toFloat(), leftTop!!.y.toFloat(), rightBottom!!.x.toFloat(), rightBottom!!.y.toFloat())
        canvas.drawRoundRect(cropRect, cornerRadius, cornerRadius, paint)
    }

    @SuppressLint("ClickableViewAccessibility") override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                previous!!.set(event.x.toInt(), event.y.toInt())
                activeSide = if (isActionInsideRectangle(event.x, event.y))
                    getAffectedSide(event.x, event.y)
                else -1
            }
            MotionEvent.ACTION_MOVE -> if (activeSide != -1) {
                prevRect.set(cropRect)
                adjustRectangle(event.x.toInt(), event.y.toInt())
                val pad = 10f
                invalidate(
                    (minOf(prevRect.left, leftTop!!.x.toFloat()) - pad).toInt(),
                    (minOf(prevRect.top, leftTop!!.y.toFloat()) - pad).toInt(),
                    (maxOf(prevRect.right, rightBottom!!.x.toFloat()) + pad).toInt(),
                    (maxOf(prevRect.bottom, rightBottom!!.y.toFloat()) + pad).toInt()
                )
                previous!!.set(event.x.toInt(), event.y.toInt())
                onChanged?.invoke(activeSide != DRAG)
            }
            MotionEvent.ACTION_UP -> {
                previous = Point()
                activeSide = -1
            }
        }
        return true
    }

    private fun initCropView() {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.pathEffect = DashPathEffect(floatArrayOf(16f, 10f), 0f)
        paint.isAntiAlias = false
        leftTop = Point()
        rightBottom = Point()
        center = Point()
        previous = Point()
    }

    fun resetPoints() {
        center!!.set(width / 2, height / 2)
        leftTop!![(width - initial_size) / 2] = (height - initial_size) / 2
        rightBottom!!.set(leftTop!!.x + initial_size, leftTop!!.y + initial_size)
        invalidate()
    }

    fun centerPoints() {
        val currentW = rightBottom!!.x - leftTop!!.x
        val currentH = rightBottom!!.y - leftTop!!.y
        center!!.set(width / 2, height / 2)
        leftTop!!.set((width - currentW) / 2, (height - currentH) / 2)
        rightBottom!!.set(leftTop!!.x + currentW, leftTop!!.y + currentH)
        invalidate()
    }

    private fun isActionInsideRectangle(x: Float, y: Float): Boolean {
        val buffer = 10
        return x >= leftTop!!.x - buffer && x <= rightBottom!!.x + buffer && y >= leftTop!!.y - buffer && y <= rightBottom!!.y + buffer
    }

    private fun isInImageRange(point: PointF): Boolean {
        // Get image matrix values and place them in an array
        val f = FloatArray(9)
        imageMatrix.getValues(f)

        // Calculate the scaled dimensions
        imageScaledWidth = (drawable.intrinsicWidth * f[Matrix.MSCALE_X]).roundToInt()
        imageScaledHeight = (drawable.intrinsicHeight * f[Matrix.MSCALE_Y]).roundToInt()
        return point.x >= center!!.x - imageScaledWidth / 2 && point.x <= center!!.x + imageScaledWidth / 2 && point.y >= center!!.y - imageScaledHeight / 2 && point.y <= center!!.y + imageScaledHeight / 2
    }

    private fun adjustRectangle(x: Int, y: Int) {
        when (activeSide) {
            LEFT -> {
                val movement = x - leftTop!!.x
                val newX = leftTop!!.x + movement
                val newY = leftTop!!.y + movement
                if (newX < rightBottom!!.x && isInViewBounds(newX, newY)) {
                    leftTop!!.set(newX, newY)
                }
            }
            TOP -> {
                val movement = y - leftTop!!.y
                val newX = leftTop!!.x + movement
                val newY = leftTop!!.y + movement
                if (newY < rightBottom!!.y && isInViewBounds(newX, newY)) {
                    leftTop!!.set(newX, newY)
                }
            }
            RIGHT -> {
                val movement = x - rightBottom!!.x
                val newX = rightBottom!!.x + movement
                val newY = rightBottom!!.y + movement
                if (newX > leftTop!!.x && isInViewBounds(newX, newY)) {
                    rightBottom!!.set(newX, newY)
                }
            }
            BOTTOM -> {
                val movement = y - rightBottom!!.y
                val newX = rightBottom!!.x + movement
                val newY = rightBottom!!.y + movement
                if (newY > leftTop!!.y && isInViewBounds(newX, newY)) {
                    rightBottom!!.set(newX, newY)
                }
            }
            DRAG -> {
                val dx = x - previous!!.x
                val dy = y - previous!!.y
                if (isInImageRange(PointF((leftTop!!.x + dx).toFloat(), (leftTop!!.y + dy).toFloat())) &&
                    isInImageRange(PointF((rightBottom!!.x + dx).toFloat(), (rightBottom!!.y + dy).toFloat()))) {
                    leftTop!!.set(leftTop!!.x + dx, leftTop!!.y + dy)
                    rightBottom!!.set(rightBottom!!.x + dx, rightBottom!!.y + dy)
                }
            }
        }
    }

    private fun isInViewBounds(x: Int, y: Int): Boolean {
        return x >= 0 && x <= width && y >= 0 && y <= height
    }

    private fun getAffectedSide(x: Float, y: Float): Int {
        val buffer = 10
        return if (x >= leftTop!!.x - buffer && x <= leftTop!!.x + buffer) LEFT else if (y >= leftTop!!.y - buffer && y <= leftTop!!.y + buffer) TOP else if (x >= rightBottom!!.x - buffer && x <= rightBottom!!.x + buffer) RIGHT else if (y >= rightBottom!!.y - buffer && y <= rightBottom!!.y + buffer) BOTTOM else DRAG
    }

    fun getCroppedImage(): ByteArray? {
        val drawable = drawable as BitmapDrawable
        val x: Float = (leftTop!!.x - center!!.x + drawable.bitmap.width / 2).toFloat()
        val y: Float = (leftTop!!.y - center!!.y + drawable.bitmap.height / 2).toFloat()
        val cropped = Bitmap.createBitmap(drawable.bitmap, x.toInt(), y.toInt(), rightBottom!!.x - leftTop!!.x, rightBottom!!.y - leftTop!!.y)
        val stream = ByteArrayOutputStream()
        cropped.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}