package com.sanlorng.coromusic.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sanlorng.coromusic.work.helper.ItemClickListener
import com.sanlorng.coromusic.R
import com.sanlorng.coromusic.model.MusicModel
import kotlinx.android.synthetic.main.fragment_item_list_dialog.*
import kotlinx.android.synthetic.main.fragment_item_list_dialog_item.view.*
class MusicListDialogFragment(private val listMusic : ArrayList<MusicModel>, private var listener: ItemClickListener? = null) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_item_list_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        list.layoutManager = LinearLayoutManager(context)
        list.adapter = ItemAdapter(listMusic)
        list.minimumHeight = resources.displayMetrics.heightPixels /2
    }

    override fun onDestroy() {
        listener = null
        super.onDestroy()
    }

    interface Listener {
        fun onItemClicked(list:ArrayList<MusicModel>,position: Int)
    }

    private inner class ViewHolder(inflater: LayoutInflater, parent: ViewGroup) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.fragment_item_list_dialog_item, parent, false)) {

    }

    private inner class ItemAdapter(private val list: ArrayList<MusicModel>) :
        RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context), parent)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            list[position].run {
                holder.itemView.apply {
                    textMusicTitle?.text = title
                    textMusicAlbum?.text = String.format("%s - %s",artist,album)
                    setOnClickListener {
                        listener?.onItemClick(list,position)
                    }
                    textMusicPosition.text = when(itemCount) {
                        in 0 .. 99 -> String.format("%02d",position + 1)
                        in 100 .. 999 -> String.format("%03d",position + 1)
                        in 1000 .. 9999 -> String.format("%04d",position + 1)
                        in 10000 .. 99999 -> String.format("%05d",position + 1)
                        else -> (position + 1).toString()
                    }
                }
            }
        }

        override fun getItemCount(): Int {
            return list.size
        }
    }

    companion object {

        // TODO: Customize parameters
        fun newInstance(listMusic: ArrayList<MusicModel>,listener: ItemClickListener?): MusicListDialogFragment =
            MusicListDialogFragment(listMusic,listener).apply {
                arguments = Bundle().apply {
                    putInt("size", listMusic.size)
                }
            }

    }
}