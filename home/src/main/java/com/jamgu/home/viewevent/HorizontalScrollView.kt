package com.jamgu.home.viewevent

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
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
 * 兼容多点触摸事件【POINTER_DOWN, POINTER_UP】
 */
class HorizontalScrollView: ViewGroup, IOverScroll {

    private var mLastX = 0.0f
    private var mLastY = 0.0f
    private var mLastInterceptX = 0.0f
    private var mLastInterceptY = 0.0f
    private var mVelocityTracker = VelocityTracker.obtain()
    private var mScroller = Scroller(context)
    // 记录当前显示子 View 的索引
    private var mChildCurIdx = 0
    // 记录当前滑动的方向
    private var mTouchDirection = DIRECTION_NONE

    // 处理多点触碰，用于记录当前处理滑动的触摸点ID
    private var mScrollPointerId = 0

    private var mTouchSlop: Int = 0
    private var mMinimumVelocity: Int = 0
    private var mMaxmumVolocity: Int = 0

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
        ViewConfiguration.get(context).let {
            mTouchSlop = it.scaledTouchSlop
            mMinimumVelocity = it.scaledMinimumFlingVelocity
            mMaxmumVolocity = it.scaledMaximumFlingVelocity
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        ev ?: return false

        val actionIndex = ev.actionIndex
        // action without idx
        val action = ev.actionMasked

        var intercepted = false
        when(action) {
            MotionEvent.ACTION_DOWN -> {
                if (!mScroller.isFinished) {
                    mScroller.abortAnimation()
                }
                mScrollPointerId = ev.getPointerId(0)
                mLastX = ev.x.also { mLastInterceptX = it }
                mLastY = ev.y.also { mLastInterceptY = it }
                intercepted = false
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                mScrollPointerId = ev.getPointerId(actionIndex)
                mLastX = ev.getX(actionIndex).also { mLastInterceptX = it }
                mLastY = ev.getY(actionIndex).also { mLastInterceptY = it }
            }
            MotionEvent.ACTION_MOVE -> {
                val pIdx = ev.findPointerIndex(mScrollPointerId)
                if (pIdx < 0) {
                    JLog.e(TAG, "pointer index for id $mScrollPointerId not found. Did any MotionEvent get skipped?")
                    return false
                }
                val x = ev.getX(pIdx)
                val y = ev.getY(pIdx)
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
                if ((dX > dY && dY > mTouchSlop) || isUDOverScroll(ev)) {
                    intercepted = true
                }
                mLastX = x
                mLastY = y
            }
            MotionEvent.ACTION_UP -> {
                mVelocityTracker.clear()
                intercepted = false
            }
            MotionEvent.ACTION_POINTER_UP -> {
                onPointerUp(ev)
            }
            MotionEvent.ACTION_CANCEL -> {
                mVelocityTracker.clear()
                intercepted = false
            }
        }
        return intercepted
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return true
        val child = getChildAt(0) ?: return true

        val action = event.actionMasked
        val actionIndex = event.actionIndex
        mVelocityTracker.addMovement(event)

        when(action) {
            MotionEvent.ACTION_DOWN -> {
                if (!mScroller.isFinished) {
                    mScroller.abortAnimation()
                }

                // 记录第一个手指头的触摸 iD
                mScrollPointerId = event.getPointerId(0)
                mLastX = event.x.also { mLastInterceptX = it }
                mLastY = event.y.also { mLastInterceptY = it }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                // 当屏幕上已经有手指头的时候，再按一个手指头下去就会触发这个事件
                // 当第2个手指头点击屏幕的时候，就会用这个手指头来接管这次事件流
                mScrollPointerId = event.getPointerId(actionIndex)
                mLastX = event.getX(actionIndex).also { mLastInterceptX = it }
                mLastY = event.getY(actionIndex).also { mLastInterceptY = it }
            }
            MotionEvent.ACTION_MOVE -> {
                // 通过pointer id 拿到需要处理的 index
                val pIdx = event.findPointerIndex(mScrollPointerId)
                if (pIdx < 0) {
                    JLog.e(TAG, "pointer index for id $mScrollPointerId not found. Did any MotionEvent get skipped?")
                    return false
                }
                val x = event.getX(pIdx)
                val deltaX = x - mLastX
                val y = event.getY(pIdx)
                var deltaY = y - mLastY
                // 处理 mTouchSlop 偏差
                if (mTouchDirection == DIRECTION_DOWN && deltaY.absoluteValue >= mTouchSlop) {
                    deltaY -= mTouchSlop
                } else if (mTouchDirection == DIRECTION_UP && deltaY.absoluteValue >= mTouchSlop) {
                    deltaY += mTouchSlop
                }
                if (isTouchDirectionHorizontal(mTouchDirection)) {
                    if (isLROverScroll(event)) {
                        scrollBy(-deltaX.roundToInt() / 2, 0)
                    } else {
                        scrollBy(-deltaX.roundToInt(), 0)
                    }
                } else {
                    scrollBy(0, -deltaY.roundToInt() / 2)
                }
                mLastX = x
                mLastY = y
            }
            MotionEvent.ACTION_UP -> {
                if (isTouchDirectionHorizontal(mTouchDirection)) {
                    mVelocityTracker.computeCurrentVelocity(1000)
                    val xVelocity = mVelocityTracker.xVelocity
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
                        mVelocityTracker.clear()

                        mChildCurIdx = childIdx
                    }
                } else {
                    smoothScrollBy(0, -scrollY)
                }

                mTouchDirection = DIRECTION_NONE
            }
            MotionEvent.ACTION_POINTER_UP -> {
                // 当手指头离开屏幕，同时屏幕上还有手指头的时候就会触发这个事件。
                onPointerUp(event)
            }
            MotionEvent.ACTION_CANCEL -> {
                mVelocityTracker.clear()
            }
        }

        return super.onTouchEvent(event)
    }

    private fun onPointerUp(e: MotionEvent?) {
        e ?: return
        val actionIndex = e.actionIndex
        // 如果离开的那个点的id正好是我们接管触摸的那个点，那么我们就需要重新再找一个pointer来接管，反之不用管
        if (e.getPointerId(actionIndex) == mScrollPointerId) {
            // Pick a new pointer to pick up the slack.
            val newIndex = if (actionIndex == 0) 1 else 0
            mScrollPointerId = e.getPointerId(newIndex)
            mLastX = e.getX(newIndex)
            mLastInterceptX = mLastX
            mLastY = e.getY(newIndex)
            mLastInterceptY = mLastY
        }
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
                view.layout(
                    childLeft, childTop,
                    childLeft + measuredWidth - paddingRight - lp.rightMargin, childTop + measuredHeight - childBottom
                )
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