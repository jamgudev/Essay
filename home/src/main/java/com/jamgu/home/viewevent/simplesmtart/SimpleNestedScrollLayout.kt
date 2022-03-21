package com.jamgu.home.viewevent.simplesmtart

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.View.MeasureSpec.AT_MOST
import android.view.View.MeasureSpec.EXACTLY
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.Interpolator
import android.widget.Scroller
import androidx.core.view.NestedScrollingParent3
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat
import com.jamgu.common.util.log.JLog
import com.jamgu.home.viewevent.DIRECTION_NONE
import com.jamgu.home.viewevent.simplesmtart.api.IRefreshComponent
import com.jamgu.home.viewevent.simplesmtart.api.IRefreshContent
import com.jamgu.home.viewevent.simplesmtart.api.IRefreshFooter
import com.jamgu.home.viewevent.simplesmtart.api.IRefreshHeader
import com.jamgu.home.viewevent.simplesmtart.impl.RefreshContentWrapper
import com.jamgu.home.viewevent.simplesmtart.impl.RefreshFooterWrapper
import com.jamgu.home.viewevent.simplesmtart.impl.RefreshHeaderWrapper
import com.jamgu.home.viewevent.simplesmtart.interpolator.INTERPOLATOR_VISCOUS_FLUID
import com.jamgu.home.viewevent.simplesmtart.interpolator.ReboundInterpolator
import com.jamgu.home.viewevent.simplesmtart.util.SmartUtil
import com.jamgu.home.viewevent.simplesmtart.util.WidgetUtil
import kotlin.math.absoluteValue
import kotlin.math.log
import kotlin.math.pow
import kotlin.math.roundToInt

private const val TAG = "NestedHorizontalScrollView2"

/**
 * Created by jamgu on 2022/02/18
 *
 * 在 [com.jamgu.home.viewevent.HorizontalScrollView] 的基础上，支持嵌套滑动
 *
 */
class SimpleNestedScrollLayout : ViewGroup, NestedScrollingParent3 {

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

    private var mIsBeingDragged = false
    private var mSuperDispatchTouchEvent = false
    private var mNestedInProgress = false
    private var mIsAllowOverScroll = true       // 是否允许过渡滑动
    private var mPreConsumedNeeded = 0          // 在子 View 滑动前，此View需要滑动的距离
    private var mSpinner = 0f                    // 当前竖直方向上 translationY 的距离

    private var mReboundAnimator: ValueAnimator? = null
    private var mReboundInterpolator = ReboundInterpolator(INTERPOLATOR_VISCOUS_FLUID)

    private var mRefreshContent: IRefreshContent? = null
    private var mRefreshHeader: IRefreshComponent? = null
    private var mRefreshFooter: IRefreshComponent? = null

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

    override fun onFinishInflate() {
        super.onFinishInflate()
        val childCount = super.getChildCount()
        if (childCount > 3) {
            throw RuntimeException("最多支持3个子View")
        }

        for (i in 0 until childCount) {
            val childView = super.getChildAt(i)
            if (SmartUtil.isContentView(childView)) {
                mRefreshContent = RefreshContentWrapper(childView)
            }
            when (childView) {
                is IRefreshHeader -> {
                    mRefreshHeader = RefreshHeaderWrapper(childView)
                }
                is IRefreshFooter -> {
                    mRefreshFooter = RefreshFooterWrapper(childView)
                }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 让 contentView 显示在最前面
        if (mRefreshContent != null) {
            super.bringChildToFront(mRefreshContent?.getContentView())
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mReboundAnimator?.let {
            it.removeAllUpdateListeners()
            it.removeAllListeners()
            it.duration = 0
            it.cancel()
        }
        mReboundAnimator = null
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var minimumWidth = 0
        var minimumHeight = 0
        val thisView = this
        for (i in 0 until super.getChildCount()) {
            val childView = super.getChildAt(i)
            if (childView == null || childView.visibility == GONE) continue

            if (mRefreshHeader?.getView() == childView) {
                mRefreshHeader?.getView()?.let { headerView ->
                    val lp = headerView.layoutParams
                    val mlp = lp as? MarginLayoutParams
                    val leftMargin = mlp?.leftMargin ?: 0
                    val rightMargin = mlp?.rightMargin ?: 0
                    val bottomMargin = mlp?.bottomMargin ?: 0
                    val topMargin = mlp?.topMargin ?: 0
                    val widthSpec = getChildMeasureSpec(widthMeasureSpec, leftMargin + rightMargin, lp.width)

                    var height = lp.height
                    if (lp.height > 0) {
                        height = topMargin + bottomMargin + lp.height
                    } else if (lp.height == WRAP_CONTENT) {
                        val maxHeight =
                            (MeasureSpec.getSize(heightMeasureSpec) - topMargin - bottomMargin).coerceAtLeast(0)
                        headerView.measure(widthSpec, MeasureSpec.makeMeasureSpec(maxHeight, AT_MOST))
                        if (headerView.measuredHeight > 0) {
                            height = -1
                        }
                    }

                    if (height != -1) {
                        val exactHeight = (height - topMargin - bottomMargin).coerceAtLeast(0)
                        headerView.measure(widthSpec, MeasureSpec.makeMeasureSpec(exactHeight, EXACTLY))
                    }
                }
            }

            if (mRefreshFooter?.getView() == childView) {
                mRefreshFooter?.getView()?.let { footerView ->
                    val lp = footerView.layoutParams
                    val mlp = lp as? MarginLayoutParams
                    val leftMargin = mlp?.leftMargin ?: 0
                    val rightMargin = mlp?.rightMargin ?: 0
                    val bottomMargin = mlp?.bottomMargin ?: 0
                    val topMargin = mlp?.topMargin ?: 0
                    val widthSpec = getChildMeasureSpec(widthMeasureSpec, leftMargin + rightMargin, lp.width)

                    var height = lp.height
                    if (height > 0) {
                        height = lp.height + topMargin + bottomMargin
                    } else if (lp.height == WRAP_CONTENT) {
                        val maxHeight =
                            (MeasureSpec.getSize(heightMeasureSpec) - topMargin - bottomMargin).coerceAtLeast(0)
                        footerView.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(maxHeight, AT_MOST))
                        if (footerView.measuredHeight > 0) {
                            height - 1
                        }
                    }

                    if (height != -1) {
                        val exactHeight = (height - topMargin - bottomMargin).coerceAtLeast(0)
                        footerView.measure(widthSpec, MeasureSpec.makeMeasureSpec(exactHeight, EXACTLY))
                    }
                }
            }

            if (mRefreshContent?.getContentView() == childView) {
                mRefreshContent?.getContentView()?.let { contentView ->
                    val lp = contentView.layoutParams
                    val mlp = lp as? MarginLayoutParams
                    val leftMargin = mlp?.leftMargin ?: 0
                    val rightMargin = mlp?.rightMargin ?: 0
                    val bottomMargin = mlp?.bottomMargin ?: 0
                    val topMargin = mlp?.topMargin ?: 0
                    val widthSpec = getChildMeasureSpec(
                        widthMeasureSpec,
                        thisView.paddingLeft + thisView.paddingRight + leftMargin + rightMargin, lp.width
                    )
                    val heightSpec = getChildMeasureSpec(
                        heightMeasureSpec,
                        thisView.paddingTop + thisView.paddingBottom + topMargin + bottomMargin, lp.height
                    )
                    contentView.measure(widthSpec, heightSpec)
                    minimumWidth += contentView.measuredWidth
                    minimumHeight += contentView.measuredHeight
                }
            }
        }

        minimumWidth += thisView.paddingLeft + thisView.paddingRight
        minimumHeight += thisView.paddingTop + thisView.paddingBottom
        super.setMeasuredDimension(
            resolveSize(minimumWidth.coerceAtLeast(super.getSuggestedMinimumWidth()), widthMeasureSpec),
            resolveSize(minimumHeight.coerceAtLeast(super.getSuggestedMinimumHeight()), heightMeasureSpec)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val thisView = this
        for (i in 0 until super.getChildCount()) {
            val childView = super.getChildAt(i)
            if (childView == null || childView.visibility == GONE) continue

            if (mRefreshHeader?.getView() == childView) {
                mRefreshHeader?.getView()?.let { headerView ->
                    val lp = headerView.layoutParams
                    val mlp = lp as? MarginLayoutParams
                    val leftMargin = mlp?.leftMargin ?: 0
                    val topMargin = mlp?.topMargin ?: 0

                    val left = leftMargin
                    var top = topMargin
                    val right = left + headerView.measuredWidth
                    var bottom = top + headerView.measuredHeight

                    // 向上偏移
                    top -= headerView.measuredHeight
                    bottom -= headerView.measuredHeight

                    headerView.layout(left, top, right, bottom)
                }
            }

            if (mRefreshFooter?.getView() == childView) {
                mRefreshFooter?.getView()?.let { footerView ->
                    val lp = footerView.layoutParams
                    val mlp = lp as? MarginLayoutParams
                    val leftMargin = mlp?.leftMargin ?: 0
                    val topMargin = mlp?.topMargin ?: 0

                    val left = leftMargin
                    val top = topMargin + thisView.measuredHeight
                    val right = left + footerView.measuredWidth
                    val bottom = top + footerView.measuredHeight

                    footerView.layout(left, top, right, bottom)
                }
            }

            if (mRefreshContent?.getContentView() == childView) {
                mRefreshContent?.getContentView()?.let { contentView ->
                    val lp = contentView.layoutParams
                    val mlp = lp as? MarginLayoutParams
                    val leftMargin = mlp?.leftMargin ?: 0
                    val topMargin = mlp?.topMargin ?: 0

                    val left = leftMargin + thisView.paddingLeft
                    val top = topMargin + thisView.paddingTop
                    val right = left + contentView.measuredWidth
                    val bottom = top + contentView.measuredHeight

                    contentView.layout(left, top, right, bottom)
                }
            }

        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        ev ?: return false

        val actionIndex = ev.actionIndex
        // action without idx
        val action = ev.actionMasked

        val thisView = this
        // 如果此 View 在嵌套滑动的状态，则不需要往下走，按正常嵌套滑动的流程走
        if (mNestedInProgress) {
            // 如果正在进行嵌套滑动
//            JLog.d(TAG, "mNestedInProgress = $mNestedInProgress, 按正常嵌套滑动流程走")
            return super.dispatchTouchEvent(ev)
        } else if (!thisView.isEnabled || !mIsAllowOverScroll) {
            // 如果此View不可用，或不支持嵌套滑动，正常分发
            return super.dispatchTouchEvent(ev)
        }

        if (interceptReboundByAction(action)) {
            return false
        }

        JLog.d(TAG, "不在嵌套滑动流程：可能是 down事件，或者自己处理滑动事件")
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
                        dy -= mTouchSlop
                    } else if (dy < 0 && dy < -mTouchSlop) {
                        mIsBeingDragged = true
                        dy += mTouchSlop
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

                if (mIsBeingDragged) {
                    JLog.d(TAG, "dispatchTouchEvent dy = $dy, touchY = $touchY, mTouchY = $mTouchY")
                    compute2Moving(dy.roundToInt())
                    return true
                }

            }
            MotionEvent.ACTION_UP -> {
                mVelocityTracker.addMovement(ev)
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity)
                mCurrentVelocity = mVelocityTracker.yVelocity
//                startFlingIfNeed(0f)
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

    private fun interceptReboundByAction(action: Int): Boolean {
        if (action == MotionEvent.ACTION_DOWN) {
            mReboundAnimator?.let {
                it.duration = 0
                it.cancel()
            }
            mReboundAnimator = null
        }
        return mReboundAnimator != null
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

    private fun overSpinner() {
        animSpinner(0f, 0, mReboundInterpolator, 1000)
    }

    private fun animSpinner(
        endSpinner: Float,
        startDelay: Long,
        interpolator: Interpolator?,
        duration: Long
    ): ValueAnimator? {
        if (mSpinner != endSpinner) {
            JLog.d(TAG, "start anim")
            mReboundAnimator?.let {
                it.duration = 0
                it.cancel()
            }
            mReboundAnimator = null
            val endListener = object: AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    // cancel() 会导致 onAnimationEnd，通过设置duration = 0 来标记动画被取消
                    if (animation != null && animation.duration == 0L) {
                        return
                    }

                    mReboundAnimator?.let {
                        it.removeAllUpdateListeners()
                        it.removeAllListeners()
                    }
                    mReboundAnimator = null
                }
            }
            val updateListener = ValueAnimator.AnimatorUpdateListener {
                val spinner = it.animatedValue as? Int ?: 0
                moveTranslation(spinner.toFloat())
            }
            ValueAnimator.ofInt(mSpinner.roundToInt(), endSpinner.roundToInt())
                    .also { mReboundAnimator = it }.let {
                        it.duration = duration
                        it.interpolator = interpolator
                        it.startDelay = startDelay
                        it.addListener(endListener)
                        it.addUpdateListener(updateListener)
                        it.start()
                    }

            return mReboundAnimator
        }

        return null
    }

    private fun reverseCompute(spinner: Float): Int {
        var x = 0
        if (spinner >= 0) {
            // X = -H * log((1 - y / m), 100)
            val dragRate = 0.5f
            val m = if (mMaxDragRate < 10) mMaxDragRate * mMaxDragHeight else mMaxDragRate
            val h = (mScreenHeightPixels / 2).coerceAtLeast(this.height)
            val y = spinner
            JLog.d(TAG, "reverse ${(-h * log((1 - y / m), 100f))}")
            x = ((-h * log((1 - y / m), 100f)) / dragRate).roundToInt()
        } else {
            val dragRate = 0.5f
            val m = if (mMaxDragRate < 10) mMaxDragRate * mMaxDragHeight else mMaxDragRate
            val h = (mScreenHeightPixels / 2).coerceAtLeast(this.height)
            val y = -spinner
            x = -((-h * log((1 - y / m), 100f)) / dragRate).roundToInt()
        }
        return x
    }

    private fun compute2Moving(translationY: Int) {
//        JLog.d(TAG, "dy = $translationY")
        if (translationY >= 0) {
            /**
            final double M = mHeaderMaxDragRate < 10 ? mHeaderHeight * mHeaderMaxDragRate : mHeaderMaxDragRate;
            final double H = Math.max(mScreenHeightPixels / 2, thisView.getHeight());
            final double x = Math.max(0, spinner * mDragRate);
            final double y = Math.min(M * (1 - Math.pow(100, -x / (H == 0 ? 1 : H))), x);// 公式 y = M(1-100^(-x/H))
             */
            val dragRate = 0.5f
            val m = if (mMaxDragRate < 10) mMaxDragRate * mMaxDragHeight else mMaxDragRate
            val h = (mScreenHeightPixels / 2).coerceAtLeast(this.height)
            val x = (translationY * dragRate).coerceAtLeast(0f)
            val y = (m * (1 - 100f.pow(-x / if (h == 0) 1 else h))).coerceAtMost(x)
//            JLog.d(TAG, "down y = $y")
            moveTranslation(y)
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
            val h = (mScreenHeightPixels / 2).coerceAtLeast(this.height)
            val x = -(translationY * dragRate).coerceAtMost(0f)
            val y = -((m * (1 - 100f.pow(-x / if (h == 0) 1 else h))).coerceAtMost(x))
//            JLog.d(TAG, "up y = $y")
//            scrollBy(0, -y.roundToInt())
            moveTranslation(y)
        }
    }

    private fun moveTranslation(dy: Float) {
        mSpinner = dy
        for (i in 0 until super.getChildCount()) {
            super.getChildAt(i).translationY = dy
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

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        return axes and ViewCompat.SCROLL_AXIS_VERTICAL != 0
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {
        mParentHelper?.onNestedScrollAccepted(child, target, axes, type)
//        mPreConsumedNeeded = mSpinner.roundToInt()
        mNestedInProgress = true
        mPreConsumedNeeded = reverseCompute(mSpinner)
        JLog.d(TAG, "onNestedScrollAccepted, type = $type, mSpinner = $mSpinner," +
                " mPreConsumedNeeded = $mPreConsumedNeeded")

        interceptReboundByAction(MotionEvent.ACTION_DOWN)
    }

    override fun onStopNestedScroll(target: View, type: Int) {
        JLog.d(TAG, "onStopNestedScroll")
        mParentHelper?.onStopNestedScroll(target, type)
        if (type == ViewCompat.TYPE_NON_TOUCH) {
            smoothScrollBy(0, scrollY)
        }
        mNestedInProgress = false
        mPreConsumedNeeded = 0
        overSpinner()
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
        if (dy == 0) return

        JLog.d(
            TAG, "onNestedPreScroll call. mPreConsumedNeeded = $mPreConsumedNeeded," +
                    " mSpinner = ${mSpinner}, dy = $dy"
        )
        var consumedY = 0
        // 两者异向，加剧过度滑动
        if (mPreConsumedNeeded * dy < 0) {
            consumedY = dy
            mPreConsumedNeeded -= dy
            compute2Moving(mPreConsumedNeeded)
        } else {
            // 两者同向，需先将 mPreConsumedNeeded 消耗掉
            val lastConsumedNeeded = mPreConsumedNeeded
            if (dy.absoluteValue > mPreConsumedNeeded.absoluteValue) {
                consumedY = mPreConsumedNeeded
                mPreConsumedNeeded = 0
            } else {
                consumedY = dy
                mPreConsumedNeeded -= dy
            }
            if (lastConsumedNeeded != mPreConsumedNeeded) {
                compute2Moving(mPreConsumedNeeded)
            }
        }
        consumed[1] = consumedY
    }

    @Synchronized
    private fun onNestedScrollInternal(dyUnconsumed: Int, type: Int, consumed: IntArray?) {
        if (dyUnconsumed == 0) return
        JLog.d(
            TAG, "onNestedScrollInternal call. dyUnconsumed = $dyUnconsumed, type = $type," +
                    " mPreConsumedNeeded = $mPreConsumedNeeded, canLoadMore = ${
                        WidgetUtil.canLoadMore(
                            getChildAt(0),
                            null
                        )
                    }, " +
                    "canRefresh = ${WidgetUtil.canRefresh(getChildAt(0), null)}"
        )
        // dy > 0 向上滚
        val dy = dyUnconsumed
        if (type == ViewCompat.TYPE_NON_TOUCH) {
            // fling 不处理，直接消耗
            if (consumed != null) {
                consumed[1] += dyUnconsumed
            }
        } else {
            if ((dy < 0 && mIsAllowOverScroll && mPreConsumedNeeded == 0 && WidgetUtil.canRefresh(getChildAt(0), null))
                    || (dy > 0 && mIsAllowOverScroll && mPreConsumedNeeded == 0 && WidgetUtil.canLoadMore(
                        getChildAt(0),
                        null
                    ))
            ) {
                mPreConsumedNeeded -= dyUnconsumed
                compute2Moving(mPreConsumedNeeded)
//                JLog.d(TAG, "mPreConsumedNeeded = $mPreConsumedNeeded")
                if (consumed != null) {
                    consumed[1] += dyUnconsumed
                }
            }
        }

    }

    override fun getNestedScrollAxes(): Int {
        return super.getNestedScrollAxes()
    }
}