package com.jamgu.home.keyevent

import android.app.Activity
import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver

/**
 * Created by jamgu on 2022/05/19
 */
class KeyBoardShowHideListener(val activity: Activity) {

    private val mDecorView = activity.window.decorView
    private var mDecorViewVisibleHeight = Rect().also { mDecorView.getWindowVisibleDisplayFrame(it) }.height()
    private var mOnKeyBoardStatusChangedListener: OnKeyBoardStatusChangedListener? = null

    private val onGlobalLayoutListener = object : ViewTreeObserver.OnGlobalLayoutListener{
        override fun onGlobalLayout() {
            val r = Rect()
            mDecorView.getWindowVisibleDisplayFrame(r)
            val visibleHeight: Int = r.height()

            //作软键盘显示／隐藏状态没有改变
            if (mDecorViewVisibleHeight == visibleHeight) {
                return
            }

            if (mDecorViewVisibleHeight - visibleHeight > 200) {
                mOnKeyBoardStatusChangedListener?.onKeyBoardShow()
                mDecorViewVisibleHeight = visibleHeight
                return
            }

            if (visibleHeight - mDecorViewVisibleHeight > 200) {
                mOnKeyBoardStatusChangedListener?.onKeyBoardHide()
                mDecorViewVisibleHeight = visibleHeight
                return
            }
        }

    }

    init {
        mDecorView.viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
    }

    fun setOnKeyBoardStatusChangedListener(listener: OnKeyBoardStatusChangedListener?) {
        mOnKeyBoardStatusChangedListener = listener
    }

    fun reveal() {
        mDecorView.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener)
    }

}

/**
 * 键盘拉起/隐藏接口
 */
interface OnKeyBoardStatusChangedListener {
    fun onKeyBoardShow()

    fun onKeyBoardHide()
}

/**
 * 打字速度回调接口
 */
interface IKeySpeedMonitor {
    fun onSpeed(speed: Float)
}