package com.jamgu.home.viewevent

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.jamgu.common.util.log.JLog

private const val TAG = "EventButton"

/**
 * Created by jamgu on 2022/02/18
 */
class EventButton: ViewGroup {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        val result = super.onTouchEvent(event)
        JLog.d(TAG, "result = $result")
        return result
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