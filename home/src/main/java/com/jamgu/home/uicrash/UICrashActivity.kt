package com.jamgu.home.uicrash

import android.os.Bundle
import android.os.Looper
import com.jamgu.common.page.activity.ViewBindingActivity
import com.jamgu.common.thread.ThreadPool
import com.jamgu.common.util.log.JLog
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

/*    override fun onResume() {
        super.onResume()

        // 正常运行
        ThreadPool.runOnNonUIThread{
            mBinding.vTvRandom.text = "在非UI线程修改UI"
        }
        // 报错，500ms 后，ViewRootImpl 已经创建完成
//        ThreadPool.runOnNonUIThread({
//            mBinding.vTvRandom.text = "在非UI线程修改UI"
//        }, 500)
    }*/

    override fun onResume() {
        super.onResume()

        mBinding.vTvRandom.text = "点击进行网络请求"

        mBinding.vTvRandom.setOnClickListener {
            ThreadPool.runOnNonUIThread({
                Looper.prepare()
                val dialog = CommonProgressDialog2.show(
                    this, "正在加载",
                    null, true, null
                )

                JLog.d(TAG, "runOnNonUIThread is in main thread = ${ThreadPool.isMainThread()}，" +
                        "currentThread = ${Thread.currentThread()}")

                dialog?.getLoadingTextView()?.setOnClickListener {
                    ThreadPool.runUITask {
                        JLog.d(TAG, "setOnClickListener is in main thread = ${ThreadPool.isMainThread()}" +
                                "，currentThread = ${Thread.currentThread()}")
                        val loadingMsg = dialog.getLoadingMsg()
                        dialog.getLoadingTextView().text = "$loadingMsg, 正在加载"
                    }
                }

                Looper.loop()
            }, 200)
        }

    }

    override fun getViewBinding(): ActivityUICrashBinding = ActivityUICrashBinding.inflate(layoutInflater)
}