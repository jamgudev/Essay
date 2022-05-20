package com.jamgu.home.keyevent

import android.app.Activity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import com.jamgu.common.util.log.JLog

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
                val duration = (nowTime - mStartTime) / 1000f
                IKeySpeedMonitor.onSpeed(mTotalCount * 1.0f / duration)
                JLog.d(TAG, "mStartTime = $mStartTime, nowTime = $nowTime, duration = $duration, mTotalCount = $mTotalCount")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
            }

        }.also { mTextWatcher = it })
    }

    fun reveal() {
        mKeyBoardShowHideListener?.reveal()
        mEditText?.removeTextChangedListener(mTextWatcher)
    }

}