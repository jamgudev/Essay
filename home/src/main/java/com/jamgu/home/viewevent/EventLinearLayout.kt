package com.jamgu.home.viewevent

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import androidx.appcompat.widget.LinearLayoutCompat
import com.jamgu.common.util.log.JLog

private const val TAG = "EventLinearLayout"

/**
 * Created by jamgu on 2022/02/18
 */
class EventLinearLayout: LinearLayoutCompat {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    /**
     * 拦截除了 ACTION_DOWN 以外的事件
     */
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return ev?.action != ACTION_DOWN
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        JLog.d(TAG, "view group received event = ${event?.action}")
        return super.onTouchEvent(event)
    }

}