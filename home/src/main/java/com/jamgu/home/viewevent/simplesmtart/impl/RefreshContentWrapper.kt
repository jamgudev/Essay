package com.jamgu.home.viewevent.simplesmtart.impl

import android.view.View
import com.jamgu.home.viewevent.simplesmtart.api.IRefreshContent

/**
 * Created by jamgu on 2022/03/20
 */
open class RefreshContentWrapper(contentView: View?) : IRefreshContent {

    protected var mContentView: View? = contentView

    override fun getContentView() = mContentView
}