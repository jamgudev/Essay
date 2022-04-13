package com.jamgu.home.viewevent.nested

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.View.MeasureSpec.AT_MOST
import android.view.View.MeasureSpec.EXACTLY
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import android.widget.Scroller
import androidx.core.view.NestedScrollingParent3
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat
import com.jamgu.base.widget.dp2px
import com.jamgu.common.util.log.JLog
import com.jamgu.home.viewevent.simplesmtart.interpolator.INTERPOLATOR_VISCOUS_FLUID
import com.jamgu.home.viewevent.simplesmtart.interpolator.ReboundInterpolator
import com.jamgu.home.viewevent.simplesmtart.util.SmartUtil
import com.jamgu.home.viewevent.simplesmtart.util.WidgetUtil
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.log
import kotlin.math.pow
import kotlin.math.roundToInt

private const val TAG = "SimpleNestedScrollLayout"

/**
 * Created by jamgu on 2022/02/18
 *
 * 处理上下滑动冲突、实现无缝、阻尼嵌套滑动
 *
 */
open class NestedOverScrollLayout : ViewGroup, NestedScrollingParent3 {

    // 上一次触碰的位置
    private var mTouchX = 0f
    private var mTouchY = 0f
    private var mVelocityTracker = VelocityTracker.obtain()
    private var mScroller = Scroller(context)
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

    private var mHandler: Handler? = null
    private var mIsBeingDragged = false
    private var mSuperDispatchTouchEvent = false
    private var mNestedInProgress = false
    private var mIsAllowOverScroll = true           // 是否允许过渡滑动
    private var mPreConsumedNeeded = 0              // 在子 View 滑动前，此View需要滑动的距离
    private var mSpinner = 0f                       // 当前竖直方向上 translationY 的距离

    private var mReboundAnimator: ValueAnimator? = null
    private var mReboundInterpolator = ReboundInterpolator(INTERPOLATOR_VISCOUS_FLUID)

    private var mAnimationRunnable: Runnable? = null    // 用来实现fling时，先过度滑动再回弹的效果
    private var mVerticalPermit = false                 // 控制fling时等待contentView回到translation = 0 的位置

    private var mRefreshContent: View? = null
    private var mRefreshHeader: View? = null
    private var mRefreshFooter: View? = null

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
        mHandler = Handler(Looper.getMainLooper())
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
                mRefreshContent = childView
            }
            when (childView) {
                is IRefreshHeader -> {
                    mRefreshHeader = childView
                }
                is IRefreshFooter -> {
                    mRefreshFooter = childView
                }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 让 contentView 显示在最前面
        if (mRefreshContent != null) {
            super.bringChildToFront(mRefreshContent)
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
        mAnimationRunnable = null
        mVelocityTracker.clear()
        if (mHandler != null) {
            mHandler?.removeCallbacksAndMessages(null)
        }

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var minimumWidth = 0
        var minimumHeight = 0
        val thisView = this
        for (i in 0 until super.getChildCount()) {
            val childView = super.getChildAt(i)
            if (childView == null || childView.visibility == GONE) continue

            if (mRefreshHeader == childView) {
                mRefreshHeader?.let { headerView ->
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

            if (mRefreshFooter == childView) {
                mRefreshFooter?.let { footerView ->
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

            if (mRefreshContent == childView) {
                mRefreshContent?.let { contentView ->
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

            if (mRefreshHeader == childView) {
                mRefreshHeader?.let { headerView ->
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

            if (mRefreshFooter == childView) {
                mRefreshFooter?.let { footerView ->
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

            if (mRefreshContent == childView) {
                mRefreshContent?.let { contentView ->
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
                /*val touchX = ev.getX(pIdx)
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
                    computeDampedSlipDistance((dy + mSpinner).roundToInt())
                    return true
                }*/

            }
            MotionEvent.ACTION_UP -> {
                JLog.d(TAG, "ACTION_UP comes.")
                mVelocityTracker.addMovement(ev)
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity)
                mCurrentVelocity = mVelocityTracker.yVelocity
                startFlingIfNeed(0f)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                onPointerUp(ev)
            }
            MotionEvent.ACTION_CANCEL -> {
                mVelocityTracker.clear()
                overSpinner()
                if (mIsBeingDragged) {
                    mIsBeingDragged = false
                    return true
                }
            }
        }

        return super.dispatchTouchEvent(ev)
    }

    /**
     * 根据条件，是否拦截事件
     * 如果是 down 事件，会终止回弹动画
     */
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

    /**
     * 通过动画模拟滑动到translationY = [endSpinner] 处
     * @param endSpinner 最终要滑动到的距离
     * @param startDelay 动画延迟开始时间 ms
     * @param interpolator 动画插值器
     * @param duration 动画持续时间
     * @return ValueAnimator 执行动画的对象
     */
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
            mAnimationRunnable = null
            val endListener = object : AnimatorListenerAdapter() {
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

    /**
     * 越界回弹动画
     * @param velocity 速度
     */
    protected fun animSpinnerBounce(velocity: Float) {
        if (mReboundAnimator == null) {
            JLog.d(TAG, "animSpinnerBounce = $mSpinner")
            if (mSpinner == 0f && mIsAllowOverScroll) {
                mAnimationRunnable = BounceRunnable(velocity, 0)
            }
        }
    }

    /**
     * 给出阻尼计算的距离，计算原始滑动距离
     * @param dampedDistance 阻尼计算过后的距离
     * @return Float, 计算结果
     */
    private fun reverseComputeFromDamped2Origin(dampedDistance: Float): Int {
        return if (dampedDistance >= 0) {
            // X = -H * log((1 - y / m), 100)
            val dragRate = 0.5f
            val m = if (mMaxDragRate < 10) mMaxDragRate * mMaxDragHeight else mMaxDragRate
            val h = (mScreenHeightPixels / 2).coerceAtLeast(this.height)
            val y = dampedDistance
            JLog.d(TAG, "reverse ${(-h * log((1 - y / m), 100f))}")
            ((-h * log((1 - y / m), 100f)) / dragRate).roundToInt()
        } else {
            val dragRate = 0.5f
            val m = if (mMaxDragRate < 10) mMaxDragRate * mMaxDragHeight else mMaxDragRate
            val h = (mScreenHeightPixels / 2).coerceAtLeast(this.height)
            val y = -dampedDistance
            -((-h * log((1 - y / m), 100f)) / dragRate).roundToInt()
        }
    }

    /**
     * 计算阻尼滑动距离
     * @param originTranslation 原始应该滑动的距离
     * @return Float, 计算结果
     */
    private fun computeDampedSlipDistance(originTranslation: Int): Float {
//        JLog.d(TAG, "dy = $translationY")
        if (originTranslation >= 0) {
            /**
            final double M = mHeaderMaxDragRate < 10 ? mHeaderHeight * mHeaderMaxDragRate : mHeaderMaxDragRate;
            final double H = Math.max(mScreenHeightPixels / 2, thisView.getHeight());
            final double x = Math.max(0, spinner * mDragRate);
            final double y = Math.min(M * (1 - Math.pow(100, -x / (H == 0 ? 1 : H))), x);// 公式 y = M(1-100^(-x/H))
             */
            val dragRate = 0.5f
            val m = if (mMaxDragRate < 10) mMaxDragRate * mMaxDragHeight else mMaxDragRate
            val h = (mScreenHeightPixels / 2).coerceAtLeast(this.height)
            val x = (originTranslation * dragRate).coerceAtLeast(0f)
            val y = m * (1 - 100f.pow(-x / (if (h == 0) 1 else h)))
//            JLog.d(TAG, "down y = $y, m = $m , h = $h, x = $x")
            return y
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
            val x = -(originTranslation * dragRate).coerceAtMost(0f)
            val y = -m * (1 - 100f.pow(-x / if (h == 0) 1 else h))
//            JLog.d(TAG, "up y = $y, m = $m , h = $h, x = $x")
//            scrollBy(0, -y.roundToInt())
            return y
        }
    }

    private fun moveTranslation(dy: Float) {
        for (i in 0 until super.getChildCount()) {
            super.getChildAt(i).translationY = dy
        }
        mSpinner = dy
//        invalidate()
    }

    private fun startFlingIfNeed(flingVelocity: Float): Boolean {
        val velocity = if (flingVelocity == 0f) mCurrentVelocity else flingVelocity
        if (velocity.absoluteValue > mMinimumVelocity) {
            if (velocity < 0 && mIsAllowOverScroll && mSpinner == 0f
                    || velocity > 0 && mIsAllowOverScroll && mSpinner == 0f
            ) {
                mScroller.fling(0, 0, 0, (-velocity).toInt(), 0, 0, -Int.MAX_VALUE, Int.MAX_VALUE)
                mScroller.computeScrollOffset()
                val thisView: View = this
                thisView.invalidate()
            }
        }

        return false
    }

    protected inner class BounceRunnable internal constructor(var mVelocity: Float, var mSmoothDistance: Int) :
        Runnable {
        var mFrame = 0
        var mFrameDelay = 10
        var mLastTime: Long
        var mOffset = 0f
        override fun run() {
            if (mAnimationRunnable === this) {
                mVelocity *= if (abs(mSpinner) >= abs(mSmoothDistance)) {
                    if (mSmoothDistance != 0) {
                        0.45.pow((++mFrame * 2).toDouble()).toFloat() //刷新、加载时回弹滚动数度衰减
                    } else {
                        0.85.pow((++mFrame * 2).toDouble()).toFloat() //回弹滚动数度衰减
                    }
                } else {
                    0.95.pow((++mFrame * 2).toDouble()).toFloat() //平滑滚动数度衰减
                }
                val now = AnimationUtils.currentAnimationTimeMillis()
                val t = 1f * (now - mLastTime) / 1000
                val velocity = mVelocity * t
                if (abs(velocity) >= 1) {
                    mLastTime = now
                    mOffset += velocity
                    moveTranslation(computeDampedSlipDistance(mOffset.roundToInt()))
                    mHandler?.postDelayed(this, mFrameDelay.toLong())
                } else {
                    mAnimationRunnable = null
                    if (abs(mSpinner) >= abs(mSmoothDistance)) {
                        val duration = 10L * (abs(mSpinner - mSmoothDistance).dp2px(context))
                                .coerceAtLeast(30).coerceAtMost(100)
                        animSpinner(mSmoothDistance.toFloat(), 0, mReboundInterpolator, duration)
                    }
                }
            }
        }

        init {
            mLastTime = AnimationUtils.currentAnimationTimeMillis()
            mHandler?.postDelayed(this, mFrameDelay.toLong())
        }
    }

    override fun computeScroll() {
        if (mScroller.computeScrollOffset()) {
            val finalY = mScroller.finalY
            if (finalY < 0 && WidgetUtil.canRefresh(mRefreshContent, null)
                    || finalY > 0 && WidgetUtil.canLoadMore(mRefreshContent, null)
            ) {
                if (mVerticalPermit) {
                    val velocity = if (finalY > 0) -mScroller.currVelocity else mScroller.currVelocity
                    animSpinnerBounce(velocity)
                }
                mScroller.forceFinished(true)
            } else {
                mVerticalPermit = true
                val thisView = this
                thisView.invalidate()
            }
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
        mNestedInProgress = true
        mPreConsumedNeeded = reverseComputeFromDamped2Origin(mSpinner)
        JLog.d(
            TAG, "onNestedScrollAccepted, type = $type, mSpinner = $mSpinner," +
                    " mPreConsumedNeeded = $mPreConsumedNeeded"
        )

        interceptReboundByAction(MotionEvent.ACTION_DOWN)
    }

    override fun onStopNestedScroll(target: View, type: Int) {
//        JLog.d(TAG, "onStopNestedScroll")
        mParentHelper?.onStopNestedScroll(target, type)
        mNestedInProgress = false
        overSpinner()
    }

    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
        return startFlingIfNeed(-velocityY)
    }

    override fun onNestedFling(target: View, velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {
        return super.onNestedFling(target, velocityX, velocityY, consumed)
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
        if (type == ViewCompat.TYPE_TOUCH) {
            onNestedScrollInternal(dyUnconsumed, type, consumed)
        } else {
            consumed[1] += dyUnconsumed
        }
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int
    ) {
        if (type == ViewCompat.TYPE_TOUCH) {
            onNestedScrollInternal(dyUnconsumed, type, null)
        }
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        if (dy == 0) return

        // 触摸事件的嵌套滑动才处理
        if (type == ViewCompat.TYPE_TOUCH) {
            val consumedY: Int
            // 两者异向，加剧过度滑动
            if (mPreConsumedNeeded * dy < 0) {
                consumedY = dy
                mPreConsumedNeeded -= dy
                moveTranslation(computeDampedSlipDistance(mPreConsumedNeeded))
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
                    moveTranslation(computeDampedSlipDistance(mPreConsumedNeeded))
                }
            }
            consumed[1] = consumedY
        }
    }

    @Synchronized
    private fun onNestedScrollInternal(dyUnconsumed: Int, type: Int, consumed: IntArray?) {
        if (dyUnconsumed == 0) return
        // dy > 0 向上滚
        val dy = dyUnconsumed
        if (type == ViewCompat.TYPE_NON_TOUCH) {
            // fling 不处理，直接消耗
            if (consumed != null) {
                consumed[1] += dy
            }
        } else {
            if ((dy < 0 && mIsAllowOverScroll && mPreConsumedNeeded == 0 && WidgetUtil.canRefresh(getChildAt(0), null))
                    || (dy > 0 && mIsAllowOverScroll && mPreConsumedNeeded == 0 && WidgetUtil.canLoadMore(
                        getChildAt(0),
                        null
                    ))
            ) {
                mPreConsumedNeeded -= dy
                moveTranslation(computeDampedSlipDistance(mPreConsumedNeeded))
//                JLog.d(TAG, "mPreConsumedNeeded = $mPreConsumedNeeded")
                if (consumed != null) {
                    consumed[1] += dy
                }
            }
        }

    }

    override fun getNestedScrollAxes(): Int {
        return super.getNestedScrollAxes()
    }
}