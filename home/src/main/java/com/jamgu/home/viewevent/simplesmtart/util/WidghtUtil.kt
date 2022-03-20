package com.jamgu.home.viewevent.simplesmtart.util

import android.graphics.PointF
import android.view.View
import android.view.ViewGroup

/**
 * Created by jamgu on 2022/03/19
 */
object WidgetUtil {

    /**
     * 判断触点([touchX], [touchY])是否在View[child]位置范围内
     *
     * @param parent    父 View
     * @param child     被 [parent] 包含的 子 View
     * @param touchX    触点 x 坐标
     * @param touchY    触点 y 坐标
     * @param outLocalPoint 存储的是[child]相对于[parent]的左上角位置坐标数组
     *      该参数用于传回给调用者，如不需要可不传
     *
     * @return Boolean, 在范围内，返回true 反之 false。
     */
    @JvmStatic
    fun isTransformedTouchPointInView(
        parent: View?,
        child: View?,
        touchX: Float?,
        touchY: Float?,
        outLocalPoint: PointF?
    ): Boolean {
        if (parent == null || child == null || touchX == null || touchY == null || outLocalPoint == null) {
            return false
        }
        if (child.visibility != View.VISIBLE) {
            return false
        }
        val point = FloatArray(2)
        point[0] += touchX + parent.scrollX - child.left
        point[1] += touchY + parent.scrollY - child.top
        val isInView = point[0] >= 0 && point[1] >= 0 && point[0] < child.width && point[1] < child.height
        if (isInView) {
            outLocalPoint.set(point[0] - touchX, point[1] - touchY)
        }
        return isInView
    }

    /**
     * @see [canVerticalOverScroll]
     */
    @JvmStatic
    fun canLoadMore(targetView: View?, touchPoint: PointF?): Boolean {
        return canVerticalOverScroll(targetView, 1, touchPoint)
    }

    /**
     * @see [canVerticalOverScroll]
     */
    @JvmStatic
    fun canRefresh(targetView: View?, touchPoint: PointF?): Boolean {
        return canVerticalOverScroll(targetView, -1, touchPoint)
    }

    /**
     *  判断View[targetView]是否可以过度滚动
     *  @param targetView View
     *  @param direction > 0 表向下滚动，< 0 表向上滚动
     *  @param touchPoint 触点坐标
     *  @return Boolean 可以过度滚动为 true，反之为 false
     */
    private fun canVerticalOverScroll(targetView: View?, direction: Int?, touchPoint: PointF?): Boolean {
        if (targetView != null && direction != null) {
            // 如果该View还可以滚动，说明还没到过度滚动的时候
            if (targetView.canScrollVertically(direction) && targetView.visibility == View.VISIBLE) {
                return false
            }
            // 传了touchPoint的话，会递归检查子View
            if (targetView is ViewGroup && touchPoint != null) {
                val childCount = targetView.childCount
                val pointF = PointF()
                for (i in childCount downTo 0) {
                    val child = targetView.getChildAt(i)
                    if (isTransformedTouchPointInView(targetView, child, touchPoint.x, touchPoint.y, pointF)) {
                        touchPoint.offset(pointF.x, pointF.y)
                        val can = canVerticalOverScroll(child, direction, touchPoint)
                        touchPoint.offset(-pointF.x, -pointF.y)
                        return can
                    }
                }
            }
            return true
        }
        return false
    }
}