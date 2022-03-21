package com.jamgu.home.viewevent.simplesmtart.impl

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import com.jamgu.home.viewevent.simplesmtart.RefreshState
import com.jamgu.home.viewevent.simplesmtart.api.IRefreshComponent

/**
 * Created by jamgu on 2022/03/20
 */
open class SimpleComponent : RelativeLayout, IRefreshComponent {

    protected lateinit var mWrappedView: View
    protected var mWrappedComponent: IRefreshComponent? = null

    protected constructor(wrappedView: View) : this(wrappedView, null)

    protected constructor(wrappedView: View, wrappedComponent: IRefreshComponent?) : super(
        wrappedView.context,
        null,
        0
    ) {
        this.mWrappedView = mWrappedView
        this.mWrappedComponent = mWrappedComponent
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun getView() = mWrappedView

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