package com.sanlorng.coromusic.work.helper

import com.sanlorng.coromusic.model.MusicModel

import kotlin.collections.ArrayList

interface ItemClickListener {
    fun onItemClick(list: ArrayList<MusicModel>, position: Int)
}
