package com.jamgu.home.viewevent.simplesmtart

/**
 * Created by jamgu on 2022/03/20
 */
enum class RefreshState(
    role: Int,
    dragging: Boolean,
    opening: Boolean,
    finishing: Boolean,
    twoLevel: Boolean,
    releaseToOpening: Boolean
) {
    None(0, false, false, false, false, false), PullDownToRefresh(1, true, false, false, false, false), PullUpToLoad(
        2,
        true,
        false,
        false,
        false,
        false
    ),
    PullDownCanceled(1, false, false, false, false, false), PullUpCanceled(
        2,
        false,
        false,
        false,
        false,
        false
    ),
    ReleaseToRefresh(1, true, false, false, false, true), ReleaseToLoad(
        2,
        true,
        false,
        false,
        false,
        true
    ),
    ReleaseToTwoLevel(1, true, false, false, true, true), TwoLevelReleased(
        1,
        false,
        false,
        false,
        true,
        false
    ),
    RefreshReleased(1, false, false, false, false, false), LoadReleased(
        2,
        false,
        false,
        false,
        false,
        false
    ),
    Refreshing(1, false, true, false, false, false), Loading(2, false, true, false, false, false), TwoLevel(
        1,
        false,
        true,
        false,
        true,
        false
    ),
    RefreshFinish(1, false, false, true, false, false), LoadFinish(2, false, false, true, false, false), TwoLevelFinish(
        1,
        false,
        false,
        true,
        true,
        false
    );

    val isHeader: Boolean
    val isFooter: Boolean
    val isTwoLevel // 二级刷新 ReleaseToTwoLevel TwoLevelReleased TwoLevel
            : Boolean
    val isDragging // 正在拖动状态：PullDownToRefresh PullUpToLoad ReleaseToRefresh ReleaseToLoad ReleaseToTwoLevel
            : Boolean
    val isOpening // 正在刷新状态：Refreshing Loading TwoLevel
            : Boolean
    val isFinishing //正在完成状态：RefreshFinish LoadFinish TwoLevelFinish
            : Boolean
    val isReleaseToOpening // 释放立马打开 ReleaseToRefresh ReleaseToLoad ReleaseToTwoLevel
            : Boolean

    fun toFooter(): RefreshState {
        return if (isHeader && !isTwoLevel) {
            values()[ordinal + 1]
        } else this
    }

    fun toHeader(): RefreshState {
        return if (isFooter && !isTwoLevel) {
            values()[ordinal - 1]
        } else this
    }

    init {
        isHeader = role == 1
        isFooter = role == 2
        isDragging = dragging
        isOpening = opening
        isFinishing = finishing
        isTwoLevel = twoLevel
        isReleaseToOpening = releaseToOpening
    }
}