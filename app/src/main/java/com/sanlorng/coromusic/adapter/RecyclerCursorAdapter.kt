package com.sanlorng.coromusic.adapter

import android.database.Cursor
import androidx.recyclerview.widget.RecyclerView

abstract class RecyclerCursorAdapter<T:RecyclerView.ViewHolder>(private val cursor: Cursor): RecyclerView.Adapter<T>() {
    override fun getItemCount(): Int {
        return cursor.count
    }

    override fun onBindViewHolder(holder: T, position: Int) {
        cursor.moveToPosition(position)
        onBindViewHolder(cursor,holder, position)
    }

    abstract fun onBindViewHolder(cursor: Cursor,holder: T,position: Int)
}