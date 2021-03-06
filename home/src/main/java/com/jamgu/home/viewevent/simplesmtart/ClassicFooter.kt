package com.jamgu.home.viewevent.simplesmtart

import android.content.Context
import android.util.AttributeSet
import com.jamgu.home.viewevent.simplesmtart.api.IRefreshComponent
import com.jamgu.home.viewevent.simplesmtart.api.IRefreshFooter
import com.jamgu.home.viewevent.simplesmtart.impl.SimpleComponent

/**
 * Created by jamgu on 2022/03/21
 */
class ClassicFooter: SimpleComponent, IRefreshComponent, IRefreshFooter {
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
}