package com.sanlorng.coromusic.adapter

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.sanlorng.coromusic.R
import com.sanlorng.coromusic.model.RequestType
import com.sanlorng.coromusic.ui.main.TabItemFragment

private val TAB_TITLES = arrayOf(
    R.string.tab_text_1,
    R.string.tab_text_2
)

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter(private val context: Context,fm: FragmentManager,private val list: List<TabItemFragment>) : FragmentPagerAdapter(fm) {

    override fun getCount(): Int {
        return list.size
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when(list[position].type) {
            RequestType.ALBUM -> {
                context.getString(R.string.album_list)
            }
            RequestType.ARTIST -> {
                context.getString(R.string.artist)
            }
            RequestType.FOLDER -> {
                context.getString(R.string.folder)
            }
            RequestType.LIST -> {
                context.getString(R.string.all_music)
            }
            RequestType.NULL -> {
                ""
            }
        }
    }
    override fun getItem(position: Int): Fragment {
        return list[position]
    }

}