package com.jamgu.home.recyclerview

import android.os.Bundle
import com.jamgu.common.page.activity.ViewBindingActivity
import com.jamgu.common.util.statusbar.StatusBarUtil
import com.jamgu.home.Schemes
import com.jamgu.home.databinding.ActivityQQContactBinding
import com.jamgu.krouter.annotation.KRouter

@KRouter(value = [Schemes.QQContactPage.HOME_NAME])
class QQContactActivity : ViewBindingActivity<ActivityQQContactBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusBarUtil.fitStatusLayout(this, mBinding.toolbar, true)
    }

    override fun getViewBinding(): ActivityQQContactBinding = ActivityQQContactBinding.inflate(layoutInflater)

}