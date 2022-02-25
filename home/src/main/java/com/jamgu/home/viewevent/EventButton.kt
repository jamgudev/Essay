package com.jamgu.home.viewevent

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewGroup
import android.widget.Scroller
import androidx.core.view.children
import androidx.core.view.isVisible
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private const val TAG = "EventButton"

/**
 * Created by jamgu on 2022/02/18
 */
class EventButton: ViewGroup {

    var mLastX = 0.0f
    var mLastY = 0.0f
    var mLastInterceptX = 0.0f
    var mLastInterceptY = 0.0f
    var mTracker = VelocityTracker.obtain()
    var mScroller = Scroller(context)

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        ev ?: return false
        val x = ev.x
        val y = ev.y

        var intercepted = false
        when(ev.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!mScroller.isFinished) {
                    mScroller.abortAnimation()
                }
                intercepted = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dX = (x - mLastInterceptX).absoluteValue
                val dY = (y - mLastInterceptY).absoluteValue
                if (dX > dY) {
                    intercepted = true
                }
            }
            MotionEvent.ACTION_UP -> {
                intercepted = false
            }
        }
        mLastX = x
        mLastY = y
        mLastInterceptX = x
        mLastInterceptY = y
        return intercepted
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return true

        mTracker.addMovement(event)
        val x = event.x
        val y = event.y

        when(event.action) {
            MotionEvent.ACTION_DOWN -> {
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = x - mLastX
                val deltaY = y - mLastY
                scrollBy(-deltaX.roundToInt(), 0)
            }
            MotionEvent.ACTION_UP -> {
                mTracker.computeCurrentVelocity(1000)
                val xVelocity = mTracker.xVelocity
                val child = getChildAt(0)
                child?.let {
                    val childWidth = it.width
                    var childIdx = (scrollX / childWidth)
                    childIdx = if (xVelocity.absoluteValue >= 50) {
                        if (xVelocity > 0) childIdx else childIdx + 1
                    } else {
                        (scrollX + childWidth / 2) / childWidth
                    }
                    childIdx = childIdx.coerceAtLeast(0).coerceAtMost(childCount - 1)
                    val dx = childIdx * childWidth - scrollX

                    smoothScrollBy(dx, 0)
                    mTracker.clear()
                }
            }
        }

        mLastX = x
        mLastY = y

        return true
    }

    private fun smoothScrollBy(dx: Int, dy: Int) {
        mScroller.startScroll(scrollX, 0, dx, 0, 500)
        invalidate()
    }

    override fun computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.currX, mScroller.currY)
            postInvalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var finalWidth = 0
        var finalHeight = 0
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        if (childCount > 0) {
            children.forEach { view ->
                if (! view.isVisible) return@forEach

                val lp = view.layoutParams as? MarginLayoutParams ?: return
                finalWidth += view.measuredWidth + lp.marginStart + lp.marginEnd
                finalHeight += view.measuredHeight + lp.topMargin + lp.bottomMargin
            }
        }
        setMeasuredDimension(
            MeasureSpec.makeMeasureSpec(finalWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (childCount > 0) {
            var childLeft = 0
            children.forEach { view ->
                if (! view.isVisible) return@forEach

                childLeft += paddingLeft
                var childTop = paddingTop
                var childBottom = paddingBottom
                val measuredWidth = view.measuredWidth
                val measuredHeight = view.measuredHeight
                val lp = view.layoutParams as? MarginLayoutParams ?: return
                childLeft += lp.marginStart
                childTop += lp.topMargin
                childBottom += lp.bottomMargin
                view.layout(childLeft, childTop,
                    childLeft + measuredWidth - paddingRight - lp.rightMargin, childTop + measuredHeight - childBottom)
                childLeft += measuredWidth + paddingRight + lp.rightMargin
            }
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }
}