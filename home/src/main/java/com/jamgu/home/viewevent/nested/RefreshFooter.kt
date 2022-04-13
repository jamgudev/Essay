package com.jamgu.home.viewevent.nested

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout

/**
 * Created by jamgu on 2022/04/13
 */
class RefreshFooter:RelativeLayout, IRefreshFooter {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    )
}