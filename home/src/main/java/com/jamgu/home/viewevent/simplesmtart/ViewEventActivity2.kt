package com.jamgu.home.viewevent.simplesmtart

import androidx.recyclerview.widget.RecyclerView
import com.jamgu.common.page.activity.ViewBindingActivity
import com.jamgu.common.util.log.JLog
import com.jamgu.home.Schemes
import com.jamgu.home.databinding.ActivityViewEvent2Binding
import com.jamgu.krouter.annotation.KRouter
import com.jamgu.settingpie.model.DecorationProp
import com.jamgu.settingpie.model.SetItemBuilder
import com.jamgu.settingpie.model.SetListBuilder
import com.jamgu.settingpie.model.ViewType.VIEW_TYPE_NORMAL

private const val TAG = "ViewEventActivity"

/**
 * Created by jamgu on 2022/02/18
 */
@KRouter([Schemes.ViewEventPage.HOST_NAME2])
class ViewEventActivity2 : ViewBindingActivity<ActivityViewEvent2Binding>() {

    override fun getViewBinding(): ActivityViewEvent2Binding = ActivityViewEvent2Binding.inflate(layoutInflater)

    override fun initWidget() {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            mBinding.eventView.setAllowClickWhenDisabled(false)
        }
        mBinding.eventView.setOnClickListener {
            JLog.d(TAG, "onClick called.")
        }

        initRecycler(mBinding.vRecycler1)
    }

    private fun initRecycler(recyclerView: RecyclerView) {
        SetListBuilder(recyclerView).showDecoration(true).paddingPair(16, 16)
                .addItem {
                    SetItemBuilder().viewType(VIEW_TYPE_NORMAL).mainText("资料设置")
                }.addItem {
                    SetItemBuilder().viewType(VIEW_TYPE_NORMAL).mainText("角色设置")
                }.addItem {
                    SetItemBuilder().viewType(VIEW_TYPE_NORMAL).mainText("角色设置")
                }.addItem {
                    SetItemBuilder().viewType(VIEW_TYPE_NORMAL).mainText("角色设置")
                }.addItem {
                    SetItemBuilder().viewType(VIEW_TYPE_NORMAL).mainText("角色设置")
                }.addItem {
                    SetItemBuilder().viewType(VIEW_TYPE_NORMAL).mainText("角色设置")
                }.addItem {
                    SetItemBuilder().viewType(VIEW_TYPE_NORMAL).mainText("角色设置")
                }.addItem {
                    SetItemBuilder().viewType(VIEW_TYPE_NORMAL).mainText("角色设置")
                }.addItem {
                    SetItemBuilder().viewType(VIEW_TYPE_NORMAL).mainText("角色设置")
                }.addItem {
                    SetItemBuilder().viewType(VIEW_TYPE_NORMAL).mainText("角色设置")
                }.addItem {
                    SetItemBuilder().viewType(VIEW_TYPE_NORMAL).mainText("角色设置")
                }.addItem {
                    SetItemBuilder().viewType(VIEW_TYPE_NORMAL).mainText("角色设置")
                }.addItem {
                    SetItemBuilder().viewType(VIEW_TYPE_NORMAL).mainText("角色设置")
                }.addItem {
                    SetItemBuilder().viewType(VIEW_TYPE_NORMAL).mainText("角色设置")
                }.addItem {
                    SetItemBuilder().viewType(VIEW_TYPE_NORMAL).mainText("角色设置")
                }.addItem {
                    SetItemBuilder().viewType(VIEW_TYPE_NORMAL).mainText("角色设置")
                }.addGroupItem {
                    SetItemBuilder().viewType(VIEW_TYPE_NORMAL).mainText("分组设置1")
                }.addGroupItems(DecorationProp(2, 10, 0, "#000000")) {
                    arrayListOf(
                        SetItemBuilder().viewType(VIEW_TYPE_NORMAL).mainText("分组设置2"),
                        SetItemBuilder().viewType(VIEW_TYPE_NORMAL).mainText("分组设置3"),
                        SetItemBuilder().viewType(VIEW_TYPE_NORMAL).mainText("分组设置4"),
                        SetItemBuilder().viewType(VIEW_TYPE_NORMAL).mainText("分组设置5"),
                    )

                }.build()
    }

}