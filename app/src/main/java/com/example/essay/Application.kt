package com.example.essay

import android.app.Application
import com.jamgu.common.Common

/**
 * Created by jamgu on 2022/01/13
 */
class Application: Application() {

    override fun onCreate() {
        super.onCreate()

        Common.getInstance().init(this)
    }
}