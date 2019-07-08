package com.sanlorng.coromusic.adapter

import android.database.Cursor
import android.provider.MediaStore
import android.view.ViewGroup
import com.sanlorng.coromusic.R
import com.sanlorng.coromusic.ui.main.BaseViewHolder
import kotlinx.android.synthetic.main.fragment_item_list_dialog_item.view.*

class MusicListCursorAdapter(cursor: Cursor): RecyclerCursorAdapter<BaseViewHolder>(cursor) {

    override fun onBindViewHolder(cursor: Cursor, holder: BaseViewHolder, position: Int) {
        val titleIndex =
            cursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE)
        val artistIndex =
            cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST)
        val albumIndex =
            cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM)
        val view = holder.itemView
        view.textMusicTitle.text = cursor.getString(titleIndex)
        view.textMusicAlbum.text = String.format("%s - %s",
            cursor.getString(artistIndex), cursor.getString(albumIndex))
        val positionString = when(itemCount) {
            in 0..99 -> "%02d"
            in 100..999 -> "%03d"
            in 1000..9999 -> "%04d"
            else -> "%05d"
        }
        view.textMusicPosition.text = String.format(positionString,position + 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return BaseViewHolder(
            R.layout.fragment_item_list_dialog_item,
            parent
        )
    }
}