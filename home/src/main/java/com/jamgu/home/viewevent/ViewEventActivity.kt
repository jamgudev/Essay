package com.jamgu.home.viewevent

import androidx.annotation.RequiresApi
import com.jamgu.common.page.activity.ViewBindingActivity
import com.jamgu.common.util.log.JLog
import com.jamgu.home.Schemes
import com.jamgu.home.databinding.ActivityViewEventBinding
import com.jamgu.krouter.annotation.KRouter

private const val TAG = "ViewEventActivity"

/**
 * Created by jamgu on 2022/02/18
 */
@KRouter([Schemes.ViewEventPage.HOST_NAME])
class ViewEventActivity : ViewBindingActivity<ActivityViewEventBinding>() {

    override fun getViewBinding(): ActivityViewEventBinding = ActivityViewEventBinding.inflate(layoutInflater)

    override fun initWidget() {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            mBinding.eventView.setAllowClickWhenDisabled(false)
        }
        mBinding.eventView.setOnClickListener {
            JLog.d(TAG, "onClick called.")
        }
        mBinding.eventView.isEnabled = false
    }

}