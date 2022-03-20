package com.jamgu.home.viewevent.simplesmtart.api

import com.jamgu.home.viewevent.simplesmtart.RefreshState

/**
 * Created by jamgu on 2022/03/20
 *
 * 刷新状态监听器
 */
interface IOnRefreshStateChangedListener {
    fun onRefreshStateChanged(oldState: RefreshState, newState: RefreshState)
}