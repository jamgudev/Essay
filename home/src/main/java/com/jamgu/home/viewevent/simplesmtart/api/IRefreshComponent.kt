package com.jamgu.home.viewevent.simplesmtart.api

import android.view.View

/**
 * Created by jamgu on 2022/03/20
 */
interface IRefreshComponent: IOnRefreshStateChangedListener {

    fun getView(): View?

}