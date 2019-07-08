package com.sanlorng.coromusic.ui.main

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.sanlorng.coromusic.R
import com.sanlorng.coromusic.model.MusicModel
import com.sanlorng.coromusic.service.PlayMusicService
import com.sanlorng.coromusic.work.mvp.MusicScanImpl
import kotlinx.android.synthetic.main.activity_music_play.*
import kotlinx.android.synthetic.main.fragment_item_list_dialog_item.view.*
import kotlinx.android.synthetic.main.layout_music_play_mini_bar.*
import kotlinx.coroutines.*

class MusicPlayActivity : AppCompatActivity() {

    private var musicBinder: PlayMusicService.PlayMusicBinder? = null
    private var colorJob: Job? = null
    private var controlTag = "playingActivity"
    private var isDrag = false
    private val strFormat1 = "%02d : %02d"
    private val strFormat2 = "%03d : %02d"
    private val nextPlayTypes = arrayOf(PlayMusicService.NEXT_PLAY_RANDOM,PlayMusicService.NEXT_PLAY_CIRCLE,PlayMusicService.NEXT_PLAY_SINGLE)
    private var nextPlayTypeIndex = 0
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>
    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            musicBinder = service as PlayMusicService.PlayMusicBinder
            musicBinder?.apply {
                addOnPreparedListener(controlTag, MediaPlayer.OnPreparedListener {
                    playingMusic.apply {
                        setPlayInfo(this)
                    }
                })

                addCompletionListener(controlTag, MediaPlayer.OnCompletionListener {
                })

                addPlayingCallBack(controlTag) { currentPosition, _ ->
                    onPlaying(currentPosition)
                }

                addOnPauseListener(controlTag) {
                    setPlayInfo(it)
                }

                addOnPlayListener(controlTag) {
                    setPlayInfo(it)
                }

                addOnResumeListener(controlTag) {
                    setPlayInfo(it)
                }
                supportStartPostponedEnterTransition()
                seekBarPlayingActivity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (musicBinder?.playList?.size?:0 > 0) {
                            val temp = progress / 1000
                            textPlayTime.text =
                                String.format(if (temp < 6000) strFormat1 else strFormat2, temp / 60, temp % 60)
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                        isDrag = true
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        musicBinder?.apply {
                            if (playList.size >0)
                                seekTo(seekBar?.progress ?: 0)
                        }
                        isDrag = false
                    }
                })

                buttonNextPlayMusicActivity.setOnClickListener {
                    if (checkPlay())
                        musicBinder?.nextPlay()
                }

                buttonLastMusicActivity.setOnClickListener {
                    if (checkPlay())
                        musicBinder?.lastPlay()
                }

                buttonPlayMusicActivity.setOnClickListener {
                    musicBinder?.apply {
                        if (checkPlay()) {
                            if (isPlaying) {
                                pause()
                            }
                            else {
                                resume()
                            }
                        }
                    }
                }
                nextPlayTypeIndex = nextPlayTypes.find {
                    it == nextPlayType
                }?:PlayMusicService.NEXT_PLAY_RANDOM
                switchNextPlayIcon()
                nextPlayTypeMusicActivity.setOnClickListener {
                    musicBinder?.apply {
                        nextPlayTypeIndex = (nextPlayTypeIndex+1)%nextPlayTypes.size
                        nextPlayType = nextPlayTypes[nextPlayTypeIndex]
                        switchNextPlayIcon()
                    }
                }
                listDragBottomSheetMusicList.layoutManager = LinearLayoutManager(this@MusicPlayActivity)
                listDragBottomSheetMusicList.adapter = ItemAdapter(playList)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicBinder?.apply {
                Log.e("onDisc","true")
                removePlayingCallBack(controlTag)
                removeOnPreparedListener(controlTag)
                removeCompletionListener(controlTag)
            }
        }
    }

    private fun adjustViewMargin() {
        DisplayMetrics().apply {
            getSystemService(WindowManager::class.java).defaultDisplay.getRealMetrics(this)
            val percent:Float = widthPixels*420f/1080f/densityDpi
//            toolbar_music_play.adjustMargin(percent)
//            cardAlbumPlayActivity.adjustMargin(percent)
//            textTitlePlayActivity.adjustMargin(percent)
//            textSubtitlePlayActivity.adjustMargin(percent)
//            textPlayingFileInfo.adjustMargin(percent)
//            layoutPlayControlMusicActivity.adjustMargin(percent)
//            seekBarPlayingActivity.adjustMargin(percent)
//            layoutPlaySeekBarMusicActivity.adjustMargin(percent)
//            buttonPlayMusicActivity.adjustMargin(percent)
//            textDragBottomSheetMusicList.paddingTopStatusBarHeight()
//            toolbarMusicMiniBar.paddingTopStatusBarHeight()
//            layoutBottomSheetHeader.paddingTopStatusBarHeight()
        }
    }

    private fun switchNextPlayIcon() {
        nextPlayTypeMusicActivity.setImageResource(
            when(musicBinder?.nextPlayType) {
                PlayMusicService.NEXT_PLAY_RANDOM -> R.drawable.ic_shuffle_black_24dp
                PlayMusicService.NEXT_PLAY_CIRCLE -> R.drawable.ic_repeat_black_24dp
                else -> R.drawable.ic_repeat_one_black_24dp
            })
        toolbarMusicMiniBar.menu.findItem(R.id.nextPlayTypeToolbar).icon = nextPlayTypeMusicActivity.drawable
    }

    private fun onPlaying(current: Int) {
        musicBinder?.playingMusic?.apply {
            if (isDrag.not()) {
                seekBarPlayingActivity.progress = current
            }

        }
    }
    private fun setPlayInfo(model: MusicModel) {
        model.apply {
            textSubtitlePlayActivity.text = String.format("%s - %s", artist, album)
            textTitlePlayActivity.text = title
            titleMusicMiniBar.text = title
            subTitleMusicMiniBar.text = textSubtitlePlayActivity.text
            if (albumCover != null)
                imageAlbumPlayActivity.setImageBitmap(albumCover)
            else
                imageAlbumPlayActivity.setImageResource(R.drawable.ic_album_black_24dp)
            if (albumCover != null) {
                Palette.from(albumCover!!).generate {
                    it?.apply {
                        colorJob?.cancel()
                        colorJob = GlobalScope.launch {

                            lightVibrantSwatch?.let {
                                withContext(Dispatchers.Main) {
//                                    layoutMusicPlay.background.alpha = 160
                                    layoutMusicPlay.background.setTint(it.rgb)
                                    layoutBottomSheetHeader.background = layoutMusicPlay.background.mutate().constantState?.newDrawable()
                                    toolbar_music_play.background = layoutMusicPlay.background.mutate().constantState?.newDrawable()
                                    textPlayTime.setTextColor(it.bodyTextColor)
                                    textTotalTime.setTextColor(it.bodyTextColor)
                                    textTitlePlayActivity.setTextColor(it.titleTextColor)
                                    textSubtitlePlayActivity.setTextColor(it.bodyTextColor)
                                    textDragBottomSheetMusicList.setTextColor(it.bodyTextColor)

                                    val colorList = ColorStateList.valueOf(it.bodyTextColor)
                                    seekBarPlayingActivity.progressBackgroundTintList = colorList
                                    seekBarPlayingActivity.progressTintList = ColorStateList.valueOf(it.titleTextColor)
                                    seekBarPlayingActivity.thumbTintList = ColorStateList.valueOf(it.titleTextColor)
                                    textDragBottomSheetMusicList.compoundDrawableTintList = colorList
                                    buttonLastMusicActivity.imageTintList = colorList
                                    buttonNextPlayMusicActivity.imageTintList = colorList
                                    nextPlayTypeMusicActivity.imageTintList = colorList
                                }
//                                delay(5000)
                            }

                        }

                    }
                }
            }
            albumMusicMiniBar.setImageDrawable(imageAlbumPlayActivity.drawable)
            musicBinder?.apply {
                switchPlayIconState(isPlaying)
                seekBarPlayingActivity.max = playDuration
                seekBarPlayingActivity.progress = playPosition
                val total = playDuration /1000
                val current = playPosition / 1000
                textTotalTime.text = String.format(if (total<6000) strFormat1 else strFormat2,total/60,total%60)
                textPlayTime.text = String.format(if (current<6000) strFormat1 else strFormat2,current/60,current%60)
                if (listDragBottomSheetMusicList.layoutManager == null) {
                    listDragBottomSheetMusicList.layoutManager = LinearLayoutManager(this@MusicPlayActivity)
                    listDragBottomSheetMusicList.adapter = ItemAdapter(playList)
                }
            }
        }
    }
    fun checkPlay():Boolean{
        musicBinder?.apply {
            return when {
                playList.isEmpty() -> {
                    musicBinder?.playList = MusicScanImpl.musicList
                    nextPlay()
                    false
                }
                else -> true
            }
        }
        return false
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeEnterTransition()
        setContentView(R.layout.activity_music_play)
        window.statusBarColor = Color.parseColor("#00000000")
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        titleMusicMiniBar.transitionName = ""
        subTitleMusicMiniBar.transitionName = ""
        albumMusicMiniBar.transitionName = ""
        indicatorMusicMiniBar.transitionName = ""
        indicatorMusicMiniBar.isVisible = false
        toolbarMusicMiniBar.inflateMenu(R.menu.toolbar_play_control)
        toolbarMusicMiniBar.menu.findItem(R.id.buttonPlayListToolbar).isVisible = false
        toolbarMusicMiniBar.menu.findItem(R.id.nextPlayTypeToolbar).isVisible = true
        layoutMusicMiniBar.alpha = 0f
        layoutMusicPlay.background = ColorDrawable(Color.parseColor("#ffffff")).mutate().constantState?.newDrawable()
//        layoutMusicMiniBar.isVisible = true
        toolbarMusicMiniBar.setOnMenuItemClickListener {
            musicBinder?.apply {

                when (it.itemId) {
                    R.id.buttonPlayMusicToolbar -> if (isPlaying) pause() else resume()
                    R.id.nextPlayTypeToolbar -> {
                        nextPlayTypeIndex = (nextPlayTypeIndex+1)%nextPlayTypes.size
                        nextPlayType = nextPlayTypes[nextPlayTypeIndex]
                        switchNextPlayIcon()
                    }
                }
            }
            true
        }
        val background = layoutBottomSheetMusicList.background.mutate().constantState?.newDrawable()
        layoutBottomSheetMusicList.background = background
        layoutBottomSheetMusicList.background.alpha = 0
        val statusBarInt = resources.getDimensionPixelOffset(resources.getIdentifier("status_bar_height", "dimen", "android"))
//        val params = viewStatusBarPadding.layoutParams as ViewGroup.MarginLayoutParams
        val foreground = ColorDrawable(Color.parseColor("#000000"))
        foreground.alpha = 0
//        toolbar_music_play.foreground = foreground
        layoutMusicPlay.foreground = foreground
        bottomSheetBehavior = BottomSheetBehavior.from(layoutBottomSheetMusicList).apply {
            setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
//                    Log.e("offset",slideOffset.toString())
//                    toolbar_music_play.foreground.alpha = (128 * slideOffset).toInt()
                    foreground.alpha = (128 * slideOffset).toInt()
                    layoutMusicMiniBar.alpha = slideOffset
                    listDragBottomSheetMusicList.alpha = slideOffset
                    textDragBottomSheetMusicList.alpha = 1 - slideOffset
                    layoutMusicMiniBar.isVisible = slideOffset!= 0f
//                    layoutBottomSheetMusicList.background.alpha = (255 * (0.8 + 0.2 *slideOffset)).toInt()
                    layoutBottomSheetMusicList.updatePadding(top = (statusBarInt * slideOffset).toInt())
                }


                override fun onStateChanged(bottomSheet: View, newState: Int) {

                        layoutBottomSheetMusicList.background.alpha = if (newState == BottomSheetBehavior.STATE_EXPANDED) 255 else 0
                }
            })
        }
        layoutBottomSheetHeader.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        toolbar_music_play.setNavigationOnClickListener { supportFinishAfterTransition() }
        toolbar_music_play.inflateMenu(R.menu.toolbar_music_play)
//        window.translucentSystemUI(true)
        adjustViewMargin()
        toolbarMusicMiniBar.measure(View.MeasureSpec.UNSPECIFIED,View.MeasureSpec.UNSPECIFIED)
        textDragBottomSheetMusicList.layoutParams = textDragBottomSheetMusicList.layoutParams.apply {
            height = toolbarMusicMiniBar.measuredHeight
        }

        bottomSheetBehavior.peekHeight = toolbarMusicMiniBar.measuredHeight
//        layoutMusicMiniBar.cardElevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,4f,resources.displayMetrics)
        bindService(Intent(this,PlayMusicService::class.java),conn, Service.BIND_AUTO_CREATE)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_music_play, menu)
        return true
    }

    private fun switchPlayIconState(isPlaying: Boolean) {
        buttonPlayMusicActivity.apply {
            if (isPlaying){
                icon = getDrawable(R.drawable.ic_pause_black_24dp)
                iconTint = getColorStateList(R.color.colorAccent)
                backgroundTintList = getColorStateList(R.color.colorWhite)
                rippleColor = getColorStateList(R.color.colorAccent)
            }else {
                icon = getDrawable(R.drawable.ic_play_arrow_black_24dp)
                iconTint = getColorStateList(R.color.colorWhite)
                backgroundTintList = getColorStateList(R.color.colorAccent)
                rippleColor = getColorStateList(R.color.colorWhite)
            }
        }
        toolbarMusicMiniBar.menu.findItem(R.id.buttonPlayMusicToolbar).icon =
            if (isPlaying) getDrawable(R.drawable.ic_pause_black_24dp) else getDrawable(
                R.drawable.ic_play_arrow_black_24dp
            )
    }

    override fun onResume() {
        super.onResume()
        musicBinder?.apply {
            addPlayingCallBack(controlTag) { currentPosition, _ ->
                onPlaying(currentPosition)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        musicBinder?.apply {
            removePlayingCallBack(controlTag)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        musicBinder?.removeAllListeners(controlTag)
        try {
            unbindService(conn)
        }catch (e: RuntimeException){
            e.printStackTrace()
        }
        musicBinder = null
    }

    override fun onBackPressed() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        else
            supportFinishAfterTransition()
    }

    private inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {

    }
    private inner class ItemAdapter internal constructor(private val list: java.util.ArrayList<MusicModel>) :
        RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.fragment_item_list_dialog_item, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            list[position].run {
                holder.itemView.apply {
                    textMusicTitle?.text = title
                    textMusicAlbum?.text = String.format("%s - %s",artist,album)
                    textMusicPosition.text = when(itemCount) {
                        in 0 .. 99 -> String.format("%02d",position + 1)
                        in 100 .. 999 -> String.format("%03d",position + 1)
                        in 1000 .. 9999 -> String.format("%04d",position + 1)
                        in 10000 .. 99999 -> String.format("%05d",position + 1)
                        else -> (position + 1).toString()
                    }
                    setOnClickListener {
                        musicBinder?.apply {
                            playList = this@ItemAdapter.list
                            playIndex = position
                            play(playList[playIndex].path)
                        }
                    }
                }
            }
        }

        override fun getItemCount(): Int {
            return list.size
        }
    }
}
