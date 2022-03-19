package com.jamgu.home.viewevent

import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

const val DIRECTION_NONE = 0
const val DIRECTION_UP = 1
const val DIRECTION_DOWN = 2
const val DIRECTION_LEFT = 3
const val DIRECTION_RIGHT = 4

/**
 * Created by jamgu on 2022/02/27
 *
 * 若一个 View 需要处理过度滑动，可实现该接口以获得一些处理过渡滑动的基础能力。
 */
interface IOverScroll {

    /**
     * 上下方向是否超过滑动范围
     */
    fun isUDOverScroll(): Boolean

    /**
     * 左右方向是否超过滑动范围
     */
    fun isLROverScroll(): Boolean

    /**
     * Only vertical linear manager is supported.
     */
    fun isRecyclerViewAbout2OverScroll(recyclerView: RecyclerView?, direction: Int = 0): Boolean {
        recyclerView ?: return false

        return (isRecyclerViewTop(recyclerView) && direction == DIRECTION_DOWN)
                || (isRecyclerViewBottom(recyclerView) && direction == DIRECTION_UP)
    }

    fun isRecyclerViewTop(recyclerView: RecyclerView?): Boolean {
        recyclerView ?: return false

        val layoutManager: RecyclerView.LayoutManager = recyclerView.layoutManager ?: return false
        if (layoutManager is LinearLayoutManager) {
            val firstVisibleItemPosition: Int =
                layoutManager.findFirstVisibleItemPosition()
            val childAt: View = recyclerView.getChildAt(0)
            if (firstVisibleItemPosition == 0 && childAt.top == 0) {
                return true
            }
        }
        return false
    }

    fun isRecyclerViewBottom(recyclerView: RecyclerView?): Boolean {
        recyclerView ?: return false

        val layoutManager: RecyclerView.LayoutManager = recyclerView.layoutManager ?: return false
        if (layoutManager is LinearLayoutManager) {
            val lastCompletelyVisibleItem: Int =
                layoutManager.findLastCompletelyVisibleItemPosition()
            val childAt: View = recyclerView.getChildAt(recyclerView.childCount - 1)
            if (lastCompletelyVisibleItem == layoutManager.itemCount - 1 && recyclerView.bottom == childAt.bottom) {
                return true
            }
        }
        return false
    }

    fun isTouchDirectionVertical(direction: Int?): Boolean {
        direction ?: return false

        return direction == DIRECTION_UP || direction == DIRECTION_DOWN
    }

    fun isTouchDirectionHorizontal(direction: Int?): Boolean {
        direction ?: return false

        return direction == DIRECTION_LEFT || direction == DIRECTION_RIGHT
    }
}