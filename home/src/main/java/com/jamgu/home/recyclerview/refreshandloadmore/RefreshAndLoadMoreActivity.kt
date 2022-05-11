package com.jamgu.home.recyclerview.refreshandloadmore

import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jamgu.common.page.activity.ViewBindingActivity
import com.jamgu.common.thread.ThreadPool
import com.jamgu.common.util.log.JLog
import com.jamgu.common.widget.toast.JToast
import com.jamgu.home.Schemes
import com.jamgu.home.databinding.ActivityRefreshAndLoadMoreBinding
import com.jamgu.krouter.annotation.KRouter
import java.util.ArrayList

private const val TAG = "RefreshAndLoadMoreActiv"

@KRouter([Schemes.RefreshAndLoadMorePage.HOST_NAME])
class RefreshAndLoadMoreActivity : ViewBindingActivity<ActivityRefreshAndLoadMoreBinding>() {

    private var isRefreshing = false

    private var isLoadingNextPage = false

    private lateinit var mAdapter: MRecyclerAdapter

    override fun getViewBinding(): ActivityRefreshAndLoadMoreBinding = ActivityRefreshAndLoadMoreBinding.inflate(
        layoutInflater
    )

    override fun initWidget() {
        super.initWidget()

        // 下拉刷新
        mBinding.vSwipeRefreshLayout.let { vSwipeRefresh ->
            vSwipeRefresh.setOnRefreshListener {
                vSwipeRefresh.isRefreshing = true
                this@RefreshAndLoadMoreActivity.isRefreshing = true

                ThreadPool.runUITask({
                    vSwipeRefresh.isRefreshing = false
                    this@RefreshAndLoadMoreActivity.isRefreshing = false

                    JToast.showToast(this@RefreshAndLoadMoreActivity, "刷新完毕。")
                }, 2000)
            }
        }

        mBinding.vRecycler.layoutManager = LinearLayoutManager(this)

        mBinding.vRecycler.adapter = MRecyclerAdapter(generateData()).also { mAdapter = it }

        mBinding.vRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val lastVisibleItem = lm.findLastVisibleItemPosition()
                val totalItemCount = lm.itemCount

                if (lastVisibleItem == totalItemCount - 1 && !isLoadingNextPage && !isRefreshing) {
                    if (totalItemCount > 0) {
                        JLog.d(TAG, "totalItemCount = $totalItemCount")
                        mAdapter.addLoadItem()
                    }

                    // 在前面addLoadItem后，itemCount已经变化
                    // 增加一层判断，确保用户是滑到了正在加载的地方，才加载更多
                    val findLastVisibleItemPosition = lm.findLastVisibleItemPosition()
                    if (findLastVisibleItemPosition == lm.itemCount - 1) {
                        ThreadPool.runOnNonUIThread({
                            isLoadingNextPage = true
                            pullData()
                        }, 2000)
                    }
                }
            }
        })

        // 分割线
        mBinding.vRecycler.addItemDecoration(DividerItemDecoration(this,DividerItemDecoration.VERTICAL))
    }

    private fun pullData() {
        ThreadPool.runUITask {
            mAdapter.removeLoadItem()
            mAdapter.addData(generateData())
            isLoadingNextPage = false
        }
    }

    private fun generateData(): ArrayList<RecyclerItem> {
        val items = ArrayList<RecyclerItem>()
        for (i in 0 until 20) {
            val item = RecyclerItem(TYPE_NORMAL, "第 $i 个item")
            items.add(item)
        }
        return items
    }

}