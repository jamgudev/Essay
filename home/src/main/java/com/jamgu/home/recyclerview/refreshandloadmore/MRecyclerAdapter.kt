package com.jamgu.home.recyclerview.refreshandloadmore

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.jamgu.home.R

const val TYPE_FOOTER = -1001
const val TYPE_NORMAL = -1000

/**
 * Created by jamgu on 2022/05/11
 */
class MRecyclerAdapter(private val mDataList: ArrayList<RecyclerItem>): RecyclerView.Adapter<MViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MViewHolder {
        val view = if (viewType == TYPE_FOOTER) {
            LayoutInflater.from(parent.context).inflate(R.layout.loading_foot, parent, false)
        } else {
            LayoutInflater.from(parent.context).inflate(R.layout.item_recycler, parent, false)
        }

        return MViewHolder(view)
    }

    override fun onBindViewHolder(holder: MViewHolder, position: Int) {
        val item = mDataList[position]
        if (item.type != TYPE_FOOTER) {
            (holder.mItemView as? AppCompatTextView)?.text = item.name
        }
    }

    override fun getItemCount() = mDataList.size

    override fun getItemViewType(position: Int): Int = mDataList[position].type

    fun addLoadItem() {
        if (mDataList.size > 0 && mDataList[mDataList.size - 1].type == TYPE_FOOTER) {
            return
        }

        val footItem = RecyclerItem(TYPE_FOOTER, "正在加载")
        mDataList.add(footItem)
        notifyItemInserted(mDataList.size - 1)
    }

    fun removeLoadItem() {
        if (mDataList.isEmpty() || mDataList[mDataList.size - 1].type != TYPE_FOOTER) return

        val lastIdx = mDataList.size - 1
        mDataList.removeAt(lastIdx)
        notifyItemChanged(lastIdx)
    }

    fun addData(data: ArrayList<RecyclerItem>?) {
        if (data.isNullOrEmpty()) return

        val lastIdx = mDataList.size - 1
        mDataList.addAll(data)
        notifyItemChanged(lastIdx, data.size)
    }
}

class MViewHolder(val mItemView: View): RecyclerView.ViewHolder(mItemView)

class RecyclerItem(val type: Int, val name: String)