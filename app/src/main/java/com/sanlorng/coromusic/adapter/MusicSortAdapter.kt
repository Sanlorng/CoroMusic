package com.sanlorng.coromusic.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sanlorng.coromusic.R
import com.sanlorng.coromusic.model.MusicModel
import com.sanlorng.coromusic.model.RequestType
import com.sanlorng.coromusic.ui.main.BaseViewHolder
import kotlinx.android.synthetic.main.item_tab_main.view.*

class MusicSortAdapter(val list: List<RequestType>,val listener:((list: List<MusicModel>, position: Int) -> Unit)? = null): RecyclerView.Adapter<BaseViewHolder>() {

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.itemView.listMusic.adapter =
            MusicSortListAdapter(list[position], listener)
        holder.itemView.swipeMusic.setOnRefreshListener {
            holder.itemView.listMusic.adapter?.notifyDataSetChanged()
            holder.itemView.swipeMusic.isRefreshing = false
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return BaseViewHolder(R.layout.item_tab_main, parent)
    }
}