package com.jamgu.home.keyevent

import android.app.Activity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText

private const val TAG = "KeySpeedManager"

/**
 * Created by jamgu on 2022/05/20
 *
 * 检测键盘打字速度：从键盘弹起到，键盘隐藏这段时间内，用户的打字速度，单位：字/s
 */
class KeySpeedManager {

    private var mTotalCount = 0
    private var mStartTime = 0L

    private var mKeyBoardShowHideListener: KeyBoardShowHideListener? = null

    private var mTextWatcher: TextWatcher? = null
    private var mEditText: EditText? = null
    private var mInterval: Long? = 0L

    fun init(activity: Activity?, editText: EditText?, IKeySpeedMonitor: IKeySpeedMonitor?) {
        if (activity == null || editText == null || IKeySpeedMonitor == null) return

        mKeyBoardShowHideListener = KeyBoardShowHideListener(activity).apply {
            setOnKeyBoardStatusChangedListener(object : OnKeyBoardStatusChangedListener {
                override fun onKeyBoardShow() {
                    mStartTime = System.currentTimeMillis()
                }

                override fun onKeyBoardHide() {
                    mTotalCount = 0
                }
            })
        }

        mEditText = editText
        mEditText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                mTotalCount++
                val nowTime = System.currentTimeMillis()
                val duration = (nowTime - mStartTime)
                if ((mInterval ?: 0L) < duration) {
                    mTotalCount = 1
                    mStartTime = System.currentTimeMillis()
//                    Log.d(TAG, "monitor internal($mInterval) reset, passed duration = $duration")
                    return
                }
                IKeySpeedMonitor.onSpeed(mTotalCount * 1.0f / (duration / 1000f))
                Log.d(TAG, "mStartTime = $mStartTime, nowTime = $nowTime, duration = $duration, mTotalCount = $mTotalCount")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
            }

        }.also { mTextWatcher = it })
    }

    /**
     * 设置速度监听间隔
     * @param internal，监听间隔，单位ms
     */
    fun setSpeedMonitorInternal(internal: Long?) {
        this.mInterval = internal
    }

    /**
     * 不用时需调用此方法，回收资源
     */
    fun reveal() {
        mKeyBoardShowHideListener?.reveal()
        mEditText?.removeTextChangedListener(mTextWatcher)
    }

}