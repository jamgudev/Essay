package com.jamgu.home.uicrash

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.Window.ID_ANDROID_CONTENT
import androidx.core.view.children
import com.jamgu.common.page.activity.ViewBindingActivity
import com.jamgu.common.thread.ThreadPool
import com.jamgu.common.util.log.JLog
import com.jamgu.common.widget.dialog.CommonBottomDialog
import com.jamgu.common.widget.dialog.CommonProgressDialog
import com.jamgu.home.R
import com.jamgu.home.Schemes
import com.jamgu.home.databinding.ActivityUICrashBinding
import com.jamgu.krouter.annotation.KRouter

private const val TAG = "UICrashActivity"

@KRouter([Schemes.UICrashPage.HOST_NAME])
class UICrashActivity : ViewBindingActivity<ActivityUICrashBinding>() {

    override fun initWidget() {
        super.initWidget()
        title = javaClass.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        // 正常运行
        ThreadPool.runOnNonUIThread{
            mBinding.vTvRandom.text = "在非UI线程修改UI"
        }
        // 报错，500ms 后，ViewRootImpl 已经创建完成
//        ThreadPool.runOnNonUIThread({
//            mBinding.vTvRandom.text = "在非UI线程修改UI"
//        }, 500)
    }

    override fun getViewBinding(): ActivityUICrashBinding = ActivityUICrashBinding.inflate(layoutInflater)
}