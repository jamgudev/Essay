package com.jamgu.home.jobscheduler

import android.app.job.JobParameters
import android.app.job.JobService

/**
 * Created by jamgu on 2021/11/15
 */
class MJobService: JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        // return false 表在结束时，不重启
        return false
    }
}