package com.jamgu.home.viewevent

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewConfiguration
import androidx.core.view.NestedScrollingParent3
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat
import com.jamgu.common.util.log.JLog

private const val TAG = "NestedHorizontalScrollView"

/**
 * Created by jamgu on 2022/02/18
 *
 * 在 [HorizontalScrollView] 的基础上，支持嵌套滑动
 *
 */
class NestedHorizontalScrollView: HorizontalScrollView, NestedScrollingParent3 {

    private var mParentHelper: NestedScrollingParentHelper? = null

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
            mMinimumVelocity = it.scaledMinimumFlingVelocity
            mMaximumVelocity = it.scaledMaximumFlingVelocity
        }
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
            smoothScrollBy(0, -scrollY)
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
//        JLog.d(TAG, "type = $type")
        if (type == ViewCompat.TYPE_NON_TOUCH) {
            val oldScrollY = scrollY
            JLog.d(TAG, "dyUnconsumed = $dyUnconsumed")
//            val fixedUnConsumed = dyUnconsumed.coerceAtMost(3).coerceAtLeast(-3)
            moveSpinnerDamping(-dyUnconsumed * 0.3f)
            val myConsumed = dyUnconsumed
            if (consumed != null) {
                consumed[1] += myConsumed
            }
        } else {
            if (consumed != null && dyUnconsumed != 0) {
                consumed[1] += dyUnconsumed
            }
        }
    }

    override fun getNestedScrollAxes(): Int {
        return super.getNestedScrollAxes()
    }
}