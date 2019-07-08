package com.sanlorng.coromusic.ui.main

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.IBinder
import androidx.core.util.Pair
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import com.google.android.material.tabs.TabLayoutMediator
import com.sanlorng.coromusic.work.helper.ItemClickListener
import com.sanlorng.coromusic.R
import com.sanlorng.coromusic.adapter.MusicSortAdapter
import com.sanlorng.coromusic.model.MusicModel
import com.sanlorng.coromusic.model.RequestType
import com.sanlorng.coromusic.service.PlayMusicService
import com.sanlorng.coromusic.work.mvp.MusicScanImpl
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_music_play_mini_bar.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(),ServiceConnection,
    ItemClickListener {
    private var binder: PlayMusicService.PlayMusicBinder? = null
    private val tag = javaClass.name
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED)
            MusicScanImpl.scanMediaStore(this) {
                onMediaScanFinish(it)
            }
        else
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),1)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] ==
            PackageManager.PERMISSION_GRANTED && requestCode == 1)

            MusicScanImpl.scanMediaStore(this) {
                onMediaScanFinish(it)
            }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun onMediaScanFinish(isEmpty: Boolean) {

        toolbarMusicMiniBar.setOnClickListener {
            val intent = Intent(this, MusicPlayActivity::class.java)
            val option = ActivityOptionsCompat
                .makeSceneTransitionAnimation(this,
                    Pair(albumMusicMiniBar,"imageAlbum"),
                    Pair(titleMusicMiniBar,"musicTitle"),
                    Pair(subTitleMusicMiniBar, "musicSubTitle"),
                    Pair(indicatorMusicMiniBar,"musicSeekBar")
                )
            startActivity(intent,option.toBundle())
        }
        val list = listOf(RequestType.LIST, RequestType.ARTIST,
            RequestType.ALBUM, RequestType.FOLDER)
        viewPagerMain.offscreenPageLimit = 4
        viewPagerMain.adapter =
            MusicSortAdapter(list) { list, position ->
                if (position == -1)
                    MusicListDialogFragment(ArrayList(list), this@MainActivity)
                        .show(supportFragmentManager, "musicFragment")
                else
                    onItemClick(ArrayList(list), position)
            }
        TabLayoutMediator(tabLayoutMain, viewPagerMain) { tab, position ->
            tab.text = when(list[position]) {
                RequestType.ALBUM -> getString(R.string.album_list)
                RequestType.ARTIST -> getString(R.string.artist)
                RequestType.FOLDER -> getString(R.string.folder)
                RequestType.LIST -> getString(R.string.all_music)
                else -> ""
            }
        }.attach()
        val intent = Intent(this, PlayMusicService::class.java)
        ContextCompat.startForegroundService(this,intent)
        startService(intent)
        bindService(intent,this, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        binder?.addPlayingCallBack(tag) { currentPosition, totalLength ->
            indicatorMusicMiniBar.progress = currentPosition
            indicatorMusicMiniBar.max = totalLength
        }
    }

    override fun onStop() {
        super.onStop()
        binder?.removePlayingCallBack(tag)
    }

    override fun onDestroy() {
        unbindService(this)
        binder?.removeAllListeners(tag)
        super.onDestroy()
    }

    override fun onItemClick(list: ArrayList<MusicModel>, position: Int) {
        binder?.playList = list
        binder?.playIndex = position
        binder?.play(list[position].path)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        (service as PlayMusicService.PlayMusicBinder).apply {
            binder = this
            addPlayingCallBack(tag) { currentPosition, totalLength ->
                indicatorMusicMiniBar.max = totalLength
                indicatorMusicMiniBar.progress = currentPosition
            }

            addOnPreparedListener(tag, MediaPlayer.OnPreparedListener {
                playingMusic.apply {
                    if (albumCover != null)
                        albumMusicMiniBar.setImageBitmap(albumCover)
                    else
                        albumMusicMiniBar.setImageResource(R.drawable.ic_album_black_24dp)
                }
            })
            toolbarMusicMiniBar.inflateMenu(R.menu.toolbar_play_control)
            toolbarMusicMiniBar.setOnMenuItemClickListener {
                when(it.itemId) {
                    R.id.buttonPlayMusicToolbar -> {
                        binder?.apply {
                            if (checkPlay()) {
                                if (isPlaying)
                                    pause()
                                else
                                    resume()
                            }
                            else {

                            }
                        }
                    }

                    R.id.buttonPlayListToolbar -> {

                        if (playList.isNotEmpty())
                            MusicListDialogFragment(playList,this@MainActivity).show(supportFragmentManager,"musicFragment")
                                else
                                Toast.makeText(this@MainActivity,"音乐列表为空",Toast.LENGTH_SHORT).show()
                            }

                    }
                true
            }
            addOnPauseListener(tag) {
                setMusicInfo(it)
            }

            addOnResumeListener(tag) {
                setMusicInfo(it)
            }

            addOnPlayListener(tag) {
                setMusicInfo(it)
            }
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {

    }

    private fun setMusicInfo(model: MusicModel) {
        model.apply {
            titleMusicMiniBar.text = title
            subTitleMusicMiniBar.text = String.format("%s - %s",artist,album)
            if (albumCover != null)
                albumMusicMiniBar.setImageBitmap(albumCover)
            else
                albumMusicMiniBar.setImageResource(R.drawable.ic_album_black_24dp)
            switchPlayIconState(binder?.isPlaying?: false)
        }
    }

    private fun switchPlayIconState(isPlaying: Boolean) {
        toolbarMusicMiniBar.menu.findItem(R.id.buttonPlayMusicToolbar).apply {
            icon = if (isPlaying.not())  getDrawable(R.drawable.ic_play_arrow_black_24dp)
            else getDrawable(R.drawable.ic_pause_black_24dp)
        }
    }

    private fun checkPlay():Boolean{
        binder?.apply {
            return when {
                playList.isEmpty() -> {
                    playList = MusicScanImpl.musicList
                    false
                }
                else -> true
            }
        }
        return false
    }
}