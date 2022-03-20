package com.jamgu.home.viewevent.simplesmtart.impl

import android.view.View
import com.jamgu.home.viewevent.simplesmtart.RefreshState
import com.jamgu.home.viewevent.simplesmtart.api.IRefreshComponent

/**
 * Created by jamgu on 2022/03/20
 */
open class SimpleComponent: IRefreshComponent {

    protected var mWrappedView: View? = null
    protected var mWrappedComponent: IRefreshComponent? = null

    protected constructor(wrappedView: View?): this(wrappedView, null)

    protected constructor(mWrappedView: View?, mWrappedComponent: IRefreshComponent?) {
        this.mWrappedView = mWrappedView
        this.mWrappedComponent = mWrappedComponent
    }

    override fun getView() = if (mWrappedView == null) null else mWrappedView

    override fun equals(other: Any?): Boolean {
        if (!super.equals(other)) {
            if (other is IRefreshComponent) {
                val thisView = this
                return thisView.getView() == other.getView()
            }
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun onRefreshStateChanged(oldState: RefreshState, newState: RefreshState) {
        mWrappedComponent?.onRefreshStateChanged(oldState, newState)
    }

}