package com.jamgu.home.viewevent

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.Scroller
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.marginRight
import androidx.core.view.marginStart
import androidx.recyclerview.widget.RecyclerView
import com.jamgu.common.util.log.JLog
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private const val TAG = "HorizontalScrollView"

/**
 * Created by jamgu on 2022/02/18
 *
 * 解决父-左右，子上下滑动冲突（异向）、同向以及混合滑动冲突场景
 */
class HorizontalScrollView: ViewGroup, IOverScroll {

    private var mLastX = 0.0f
    private var mLastY = 0.0f
    private var mLastInterceptX = 0.0f
    private var mLastInterceptY = 0.0f
    private var mTracker = VelocityTracker.obtain()
    private var mScroller = Scroller(context)
    // 记录当前显示子 View 的索引
    private var mChildCurIdx = 0
    // 记录当前滑动的方向
    private var mTouchDirection = DIRECTION_NONE

    constructor(context: Context) : super(context) {
        init()
    }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        setWillNotDraw(false)
    }

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
                mTouchDirection = if (dX > dY) {
                    if (x - mLastX >= 0) DIRECTION_RIGHT
                    else DIRECTION_LEFT
                } else {
                    if (y - mLastY >= 0) {
                        DIRECTION_DOWN
                    } else DIRECTION_UP
                }
                if (dX > dY || isUDOverScroll(ev)) {
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
        val child = getChildAt(0) ?: return true

        mTracker.addMovement(event)
        val x = event.x
        val y = event.y

        when(event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!mScroller.isFinished) {
                    mScroller.abortAnimation()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = x - mLastX
                val deltaY = y - mLastY
                if (isTouchDirectionHorizontal(mTouchDirection)) {
                    if (isLROverScroll(event)) {
                        scrollBy(-deltaX.roundToInt() / 2, 0)
                    } else {
                        scrollBy(-deltaX.roundToInt(), 0)
                    }
                } else {
                    scrollBy(0, -deltaY.roundToInt() / 2)
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isTouchDirectionHorizontal(mTouchDirection)) {
                    mTracker.computeCurrentVelocity(1000)
                    val xVelocity = mTracker.xVelocity
                    child.let {
                        val childWidth = it.width
                        var childIdx = (scrollX / childWidth)
                        childIdx = if (xVelocity.absoluteValue >= 100) {
                            if (xVelocity > 0) childIdx else childIdx + 1
                        } else {
                            // 在用户抬起手指时，当前的childIdx已经-1了，所以不用再减1了
                            (scrollX + childWidth / 2) / childWidth
                        }
                        childIdx = childIdx.coerceAtLeast(0).coerceAtMost(childCount - 1)
                        val dx = childIdx * childWidth - scrollX

                        smoothScrollBy(dx, 0)
                        mTracker.clear()

                        mChildCurIdx = childIdx
                    }
                } else {
                    smoothScrollBy(0, -scrollY)
                }

                mTouchDirection = DIRECTION_NONE
            }
        }

        mLastX = x
        mLastY = y

        return super.onTouchEvent(event)
    }

    private fun smoothScrollBy(dx: Int, dy: Int) {
        mScroller.startScroll(scrollX, scrollY, dx, dy, 500)
        invalidate()
    }

    override fun computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.currX, mScroller.currY)
            postInvalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var finalWidth = paddingLeft + paddingRight
        var finalHeight = paddingTop + paddingBottom
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
            var childLeft = paddingLeft
            var childTop = paddingTop
            var childBottom = paddingBottom
            children.forEach { view ->
                if (! view.isVisible) return@forEach

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

    override fun isUDOverScroll(ev: MotionEvent?): Boolean {
        val childAt = getChildAt(mChildCurIdx) ?: return false
        var isUDOverScroll = false

        when(childAt) {
            is RecyclerView -> {
                ev?.let {
                    isUDOverScroll = isRecyclerViewAbout2OverScroll(childAt, mTouchDirection)
                }
            }

            is ScrollView -> {

            }
        }

        JLog.d(TAG, "isUDOverScroll = $isUDOverScroll")
        return isUDOverScroll
    }

    override fun isLROverScroll(ev: MotionEvent?): Boolean {
        var wholeWidth = 0L
        children.forEach {
            wholeWidth += it.width + it.marginStart + it.marginRight
        }
        return scrollX < 0 || scaleX > (wholeWidth + paddingRight + paddingLeft - width)
    }
}