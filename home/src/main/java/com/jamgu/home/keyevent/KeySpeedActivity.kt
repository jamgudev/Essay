package com.jamgu.home.keyevent

import android.view.View
import com.jamgu.common.page.activity.ViewBindingActivity
import com.jamgu.common.util.log.JLog
import com.jamgu.home.Schemes
import com.jamgu.home.databinding.ActivityKeySpeedBinding
import com.jamgu.krouter.annotation.KRouter


private const val TAG = "KeySpeedActivity"

@KRouter([Schemes.KeySpeedPage.HOST_NAME])
class KeySpeedActivity : ViewBindingActivity<ActivityKeySpeedBinding>() {

    private val mKeySpeedManager = KeySpeedManager()

    override fun getViewBinding(): ActivityKeySpeedBinding = ActivityKeySpeedBinding.inflate(layoutInflater)

    override fun initWidget() {
        mKeySpeedManager.init(this, mBinding.vEtKeySpeed, object : IKeySpeedMonitor{
            override fun onSpeed(speed: Float) {
                JLog.d(TAG, "key speed = $speed")
            }
        })
    }

    override fun onDestroy() {
        mKeySpeedManager.reveal()
        super.onDestroy()
    }
}