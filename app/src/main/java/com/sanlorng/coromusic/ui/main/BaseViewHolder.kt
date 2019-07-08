package com.sanlorng.coromusic.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class BaseViewHolder(view: View): RecyclerView.ViewHolder(view) {
    constructor(resId: Int,parent: ViewGroup):this(LayoutInflater.from(parent.context).inflate(resId,parent,false))
}