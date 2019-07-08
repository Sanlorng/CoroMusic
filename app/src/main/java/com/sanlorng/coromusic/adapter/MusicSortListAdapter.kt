package com.sanlorng.coromusic.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sanlorng.coromusic.R
import com.sanlorng.coromusic.model.MusicModel
import com.sanlorng.coromusic.model.RequestType
import com.sanlorng.coromusic.ui.main.BaseViewHolder
import com.sanlorng.coromusic.work.mvp.MusicScanImpl
import kotlinx.android.synthetic.main.fragment_item_list_dialog_item.view.*

typealias Listener = ((list: List<MusicModel>, position: Int) -> Unit)
class MusicSortListAdapter(val type: RequestType,
                           val listener: Listener? = null):
    RecyclerView.Adapter<BaseViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
                BaseViewHolder {
            return BaseViewHolder(
                R.layout.fragment_item_list_dialog_item,
                parent
            )
        }
        override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
            holder.itemView.run {
                when(type) {
                    RequestType.ALBUM -> {
                        val item = MusicScanImpl.albumList[position]
                        setListInfo(this,item.name,item.list)
                    }
                    RequestType.ARTIST -> {
                        val item = MusicScanImpl.artistList[position]
                        setListInfo(this,item.name,item.list)
                    }
                    RequestType.FOLDER -> {
                        val item = MusicScanImpl.folderList[position]
                        setListInfo(this,item.path,item.list)
                    }
                    RequestType.LIST -> {
                        val item = MusicScanImpl.musicList[position]
                        textMusicPosition.text = when(itemCount) {
                            in 0 .. 99 -> String.format("%02d",position + 1)
                            in 100 .. 999 -> String.format("%03d",position + 1)
                            in 1000 .. 9999 -> String.format("%04d",position + 1)
                            in 10000 .. 99999 -> String.format("%05d",position + 1)
                            else -> (position + 1).toString()
                        }
                        textMusicTitle.text = item.title
                        textMusicAlbum.text =
                            String.format("%s - %s",item.artist,item.album)
                        setOnClickListener {
                            listener?.invoke(MusicScanImpl.musicList,position)
                        }
                    }
                    RequestType.NULL -> {

                    }
                }
            }
        }

        override fun getItemCount(): Int {
            return when(type) {
                RequestType.ALBUM -> {
                    MusicScanImpl.albumList.size
                }
                RequestType.ARTIST -> {
                    MusicScanImpl.artistList.size
                }
                RequestType.FOLDER -> {
                    MusicScanImpl.folderList.size
                }
                RequestType.LIST -> {
                    MusicScanImpl.musicList.size
                }
                RequestType.NULL -> {
                    0
                }
            }
        }
    fun setListInfo(itemView:View,title: String, list: List<MusicModel>?) {
        itemView.textMusicTitle?.text = title
        itemView.textMusicAlbum.text = String.format("共有 %d 首歌",list?.size?: 0)
        itemView.setOnClickListener {
            listener?.invoke(list!!,-1)
        }
    }
}