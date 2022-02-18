package com.jamgu.home.viewevent

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatButton
import com.jamgu.common.util.log.JLog

private const val TAG = "EventButton"

/**
 * Created by jamgu on 2022/02/18
 */
class EventButton: AppCompatButton {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when(event?.action) {
            MotionEvent.ACTION_DOWN -> {
                JLog.d(TAG, "received down event.")
            }
            MotionEvent.ACTION_MOVE -> {
                JLog.d(TAG, "event_type = ${event.action}, (x, y) = (${event?.x}, ${event?.y}).")
            }
            MotionEvent.ACTION_UP -> {
                JLog.d(TAG, "received up event.")
            }
            MotionEvent.ACTION_CANCEL -> {
                JLog.d(TAG, "received cancel event.")
            }
        }
        return true
    }
}