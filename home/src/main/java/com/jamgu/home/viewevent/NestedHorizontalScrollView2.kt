package com.jamgu.home.viewevent

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.Scroller
import androidx.core.view.NestedScrollingParent3
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.marginRight
import androidx.core.view.marginStart
import androidx.recyclerview.widget.RecyclerView
import com.jamgu.common.util.log.JLog
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.roundToInt

private const val TAG = "NestedHorizontalScrollView2"

/**
 * Created by jamgu on 2022/02/18
 *
 * 在 [HorizontalScrollView] 的基础上，支持嵌套滑动
 *
 */
class NestedHorizontalScrollView2 : ViewGroup, IOverScroll, NestedScrollingParent3 {

    // 上一次触碰的位置
    private var mTouchX = 0f
    private var mTouchY = 0f
    private var mVelocityTracker = VelocityTracker.obtain()
    private var mScroller = Scroller(context)

    // 记录当前显示子 View 的索引
    private var mChildCurIdx = 0

    // 记录当前滑动的方向
    private var mTouchDirection = DIRECTION_NONE

    // 处理多点触碰，用于记录当前处理滑动的触摸点ID
    private var mScrollPointerId = 0

    private var mParentHelper: NestedScrollingParentHelper? = null

    private var mTouchSlop: Int = 0
    private var mMinimumVelocity: Float = 0f
    private var mMaximumVelocity: Float = 0f
    private var mCurrentVelocity: Float = 0f

    // 阻尼滑动参数
    private val mMaxDragRate = 2.5f
    private val mMaxDragHeight = 250
    private val mScreenHeightPixels = context.resources.displayMetrics.heightPixels
//    private var mNeedPreConsumed = 0

    private var mIsBeingDragged = false
    private var mSuperDispatchTouchEvent = false

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
        mParentHelper = NestedScrollingParentHelper(this)
        ViewConfiguration.get(context).let {
            mTouchSlop = it.scaledTouchSlop
            mMinimumVelocity = it.scaledMinimumFlingVelocity.toFloat()
            mMaximumVelocity = it.scaledMaximumFlingVelocity.toFloat()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        ev ?: return false

        val actionIndex = ev.actionIndex
        // action without idx
        val action = ev.actionMasked

        val thisView = this
//        if (!thisView.isEnabled) {
//            return super.dispatchTouchEvent(ev)
//        }

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (!mScroller.isFinished) {
                    mScroller.abortAnimation()
                }
                mScrollPointerId = ev.getPointerId(0)

                // 触摸事件初始化
                mTouchX = ev.x
                mTouchY = ev.y

                mSuperDispatchTouchEvent = super.dispatchTouchEvent(ev)
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                mScrollPointerId = ev.getPointerId(actionIndex)
                mTouchX = ev.getX(actionIndex)
                mTouchY = ev.getY(actionIndex)
            }
            MotionEvent.ACTION_MOVE -> {
                val pIdx = ev.findPointerIndex(mScrollPointerId)
                if (pIdx < 0) {
                    JLog.e(TAG, "pointer index for id $mScrollPointerId not found. Did any MotionEvent get skipped?")
                    return false
                }
                val touchX = ev.getX(pIdx)
                val touchY = ev.getY(pIdx)
                val dx = touchX - mTouchX
                var dy = touchY - mTouchY

                if (dx.absoluteValue < dy.absoluteValue) {
                    if (dy > 0 && dy > mTouchSlop) {
                        mIsBeingDragged = true
                        dy = touchY - mTouchSlop
                    } else if (dy < 0 && dy < -mTouchSlop) {
                        mIsBeingDragged = true
                        dy = touchY + mTouchSlop
                    }

                    if (mIsBeingDragged) {
                        // 如果事件经过正常分发后，被别的控件消耗了事件
                        // 分发一个取消事件
                        if (mSuperDispatchTouchEvent) {
                            ev.action = MotionEvent.ACTION_CANCEL
                            super.dispatchTouchEvent(ev)
                        }
                        val parent = thisView.parent
                        if (parent is ViewGroup) {
                            // 通知父控件不要拦截事件
                            parent.requestDisallowInterceptTouchEvent(true)
                        }
                    }
                }

                JLog.d(TAG, "mIsBeingDragged = $mIsBeingDragged")
                if (mIsBeingDragged) {
                    moveSpinnerDamping(dy)
                }

            }
            MotionEvent.ACTION_UP -> {
                mVelocityTracker.addMovement(ev)
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity)
                mCurrentVelocity = mVelocityTracker.yVelocity
                startFlingIfNeed(0f)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                onPointerUp(ev)
            }
            MotionEvent.ACTION_CANCEL -> {
                if (mIsBeingDragged) {
                    mIsBeingDragged = false
                    return true
                }
            }
        }

        return super.dispatchTouchEvent(ev)
    }

    private fun onPointerUp(e: MotionEvent?) {
        e ?: return
        val actionIndex = e.actionIndex
        // 如果离开的那个点的id正好是我们接管触摸的那个点，那么我们就需要重新再找一个pointer来接管，反之不用管
        if (e.getPointerId(actionIndex) == mScrollPointerId) {
            // Pick a new pointer to pick up the slack.
            val newIndex = if (actionIndex == 0) 1 else 0
            mScrollPointerId = e.getPointerId(newIndex)
            mTouchX = e.getX(newIndex)
            mTouchY = e.getY(newIndex)
        }
    }

    /**
     * 在必要的时候 开始 Fling 模式
     * @param flingVelocity 速度
     * @return true 可以拦截 嵌套滚动的 Fling
     */
    protected fun startFlingIfNeed(flingVelocity: Float): Boolean {
        val velocity = if (flingVelocity == 0f) mCurrentVelocity else flingVelocity
        //        //现在去掉了这个修复，反而有了回弹
//        if (Build.VERSION.SDK_INT > 27 && mRefreshContent != null) {
//            /*
//             * 修复 API 27 以上【上下颠倒模式】没有回弹效果的bug
//             */
//            float scaleY = getScaleY();
//            final View thisView = this;
//            final View contentView = mRefreshContent.getView();
//            if (thisView.getScaleY() == -1 && contentView.getScaleY() == -1) {
//                velocity = -velocity;
//            }
//        }
        if (abs(velocity) > mMinimumVelocity) {
//            if (velocity * mSpinner < 0) {
//                /*
//                 * 列表准备惯性滑行的时候，如果速度关系
//                 * velocity * mSpinner < 0 表示当前速度趋势，需要关闭 mSpinner 才合理
//                 * 但是在 mState.isOpening（不含二楼） 状态 和 noMoreData 状态 时 mSpinner 不会自动关闭
//                 * 需要使用 FlingRunnable 来关闭 mSpinner ，并在关闭结束后继续 fling 列表
//                 */
//                if (mState === RefreshState.Refreshing || mState === RefreshState.Loading || mSpinner < 0 && mFooterNoMoreData) {
//                    animationRunnable = FlingRunnable(velocity).start()
//                    return true
//                } else if (mState.isReleaseToOpening) {
//                    return true //拦截嵌套滚动时，即将刷新或者加载的 Fling
//                }
//            }
            if (velocity < 0
                    || velocity > 0 )
            {
                /*
                 * 用于监听越界回弹、Refreshing、Loading、noMoreData 时自动拉出
                 * 做法：使用 mScroller.fling 模拟一个惯性滚动，因为 AbsListView 和 ScrollView 等等各种滚动控件内部都是用 mScroller.fling。
                 *      所以 mScroller.fling 的状态和 它们一样，可以试试判断它们的 fling 当前速度 和 是否结束。
                 *      并再 computeScroll 方法中试试判读它们是否滚动到了边界，得到此时的 fling 速度
                 *      如果 当前的速度还能继续 惯性滑行，自动拉出：越界回弹、Refreshing、Loading、noMoreData
                 */
                mScroller.fling(0, 0, 0, (-velocity).toInt(), 0, 0, -Int.MAX_VALUE, Int.MAX_VALUE)
                mScroller.computeScrollOffset()
                val thisView: View = this
                thisView.invalidate()
            }
        }
        return false
    }

    private fun moveSpinnerDamping(dy: Float) {
        JLog.d(TAG, "dy = $dy")
        if (dy >= 0) {
            /**
            final double M = mHeaderMaxDragRate < 10 ? mHeaderHeight * mHeaderMaxDragRate : mHeaderMaxDragRate;
            final double H = Math.max(mScreenHeightPixels / 2, thisView.getHeight());
            final double x = Math.max(0, spinner * mDragRate);
            final double y = Math.min(M * (1 - Math.pow(100, -x / (H == 0 ? 1 : H))), x);// 公式 y = M(1-100^(-x/H))
             */
            val dragRate = 0.5f
            val m = if (mMaxDragRate < 10) mMaxDragRate * mMaxDragHeight else mMaxDragRate
            val h = (mScreenHeightPixels / 2).coerceAtLeast(this.height)
            val x = (dy * dragRate).coerceAtLeast(0f)
            val y = (m * (1 - 100f.pow(-x / if (h == 0) 1 else h))).coerceAtMost(x)
            JLog.d(TAG, "down y = $y")
            scrollBy(0, y.roundToInt())
        } else {
            /**
            final float maxDragHeight = mFooterMaxDragRate < 10 ? mFooterHeight * mFooterMaxDragRate : mFooterMaxDragRate;
            final double M = maxDragHeight - mFooterHeight;
            final double H = Math.max(mScreenHeightPixels * 4 / 3, thisView.getHeight()) - mFooterHeight;
            final double x = -Math.min(0, (spinner + mFooterHeight) * mDragRate);
            final double y = -Math.min(M * (1 - Math.pow(100, -x / (H == 0 ? 1 : H))), x);// 公式 y = M(1-100^(-x/H))
             */
            val dragRate = 0.5f
            val m = if (mMaxDragRate < 10) mMaxDragRate * mMaxDragHeight else mMaxDragRate
            val h = (mScreenHeightPixels / 2).coerceAtLeast(this.height - mMaxDragHeight)
            val x = -(dy * dragRate).coerceAtMost(0f)
            val y = -((m * (1 - 100f.pow(-x / if (h == 0) 1 else h))).coerceAtMost(x))
            JLog.d(TAG, "up y = $y")
            scrollBy(0, y.roundToInt())
        }
    }

    private fun smoothScrollBy(dx: Int, dy: Int) {
        mScroller.startScroll(scrollX, scrollY, -dx, -dy, 500)
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
                if (!view.isVisible) return@forEach

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
                if (!view.isVisible) return@forEach

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

        when (childAt) {
            is RecyclerView -> {
                ev?.let {
                    isUDOverScroll = isRecyclerViewAbout2OverScroll(childAt, mTouchDirection)
                }
            }

            is ScrollView -> {

            }
        }

//        JLog.d(TAG, "isUDOverScroll = $isUDOverScroll")
        return isUDOverScroll
    }

    override fun isLROverScroll(ev: MotionEvent?): Boolean {
        var wholeWidth = 0L
        children.forEach {
            wholeWidth += it.width + it.marginStart + it.marginRight
        }
        return scrollX < 0 || scaleX > (wholeWidth + paddingRight + paddingLeft - width)
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        return axes and ViewCompat.SCROLL_AXIS_VERTICAL != 0
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {
        mParentHelper?.onNestedScrollAccepted(child, target, axes, type)
    }

    override fun onStopNestedScroll(target: View, type: Int) {
        mParentHelper?.onStopNestedScroll(target, type)
        if (type == ViewCompat.TYPE_NON_TOUCH) {
            smoothScrollBy(0, scrollY)
        }
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        onNestedScrollInternal(dyUnconsumed, type, consumed)
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int
    ) {
        onNestedScrollInternal(dyUnconsumed, type, null)
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
    }

    @Synchronized
    private fun onNestedScrollInternal(dyUnconsumed: Int, type: Int, consumed: IntArray?) {
//        if (type == ViewCompat.TYPE_NON_TOUCH) {
//            val oldScrollY = scrollY
//            JLog.d(TAG, "dyUnconsumed = $dyUnconsumed")
//            moveSpinnerDamping(dyUnconsumed * 0.3f)
//            val myConsumed = dyUnconsumed
//            if (consumed != null) {
//                consumed[1] += myConsumed
//            }
//        } else {
            if (consumed != null && dyUnconsumed != 0) {
                consumed[1] += dyUnconsumed
            }
//        }
    }

    override fun getNestedScrollAxes(): Int {
        return super.getNestedScrollAxes()
    }
}