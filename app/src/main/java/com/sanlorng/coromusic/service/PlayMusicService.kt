package com.sanlorng.coromusic.service

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.preference.PreferenceManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import com.sanlorng.coromusic.BuildConfig
import com.sanlorng.coromusic.ui.main.MusicPlayActivity
import com.sanlorng.coromusic.R
import com.sanlorng.coromusic.model.MusicModel
import com.sanlorng.coromusic.work.helper.bitmap
import com.sanlorng.coromusic.work.mvp.MusicScanImpl
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.random.Random

class PlayMusicService : Service() {
    companion object {
        private const val PACKAGE_NAME = BuildConfig.APPLICATION_ID
        const val MUSIC_CHANNEL = "1"
        const val MUSIC_CHANNEL_INT = 1
        const val ACTION_PLAY = "$PACKAGE_NAME.play"
        const val ACTION_PAUSE = "$PACKAGE_NAME.pause"
        const val ACTION_NEXT = "$PACKAGE_NAME.next"
        const val ACTION_PREV = "$PACKAGE_NAME.prev"
        const val NEXT_PLAY_RANDOM = 0
        const val NEXT_PLAY_CIRCLE = 1
        const val NEXT_PLAY_SINGLE = 2
        const val NEXT_PLAY_ONCE = 3
    }
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var playAction: NotificationCompat.Action
    private lateinit var pauseIntent: PendingIntent
    private lateinit var playIntent: PendingIntent
    private lateinit var metaDataBuilder: MediaMetadataCompat.Builder
    private lateinit var stateBuilder: PlaybackStateCompat.Builder
    private val mPlayer = MediaPlayer()
    private val mBinder = PlayMusicBinder()
    private val musicReceiver = MusicNotificationReceiver()
    private lateinit var defaultSharedPreference: SharedPreferences
    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    override fun onCreate() {
        super.onCreate()
        defaultSharedPreference = PreferenceManager.getDefaultSharedPreferences(this)
        mPlayer.setOnCompletionListener {
            mBinder.onPlayCompletion(it)
        }
        mPlayer.setOnPreparedListener {
            mBinder.onPlayPrepared(it)
        }
        mBinder.nextPlayType = defaultSharedPreference.getInt("musicNextPlayType", NEXT_PLAY_RANDOM)
        registerReceiver(musicReceiver, IntentFilter().apply {
            addAction(ACTION_PREV)
            addAction(ACTION_NEXT)
            addAction(ACTION_PAUSE)
            addAction(ACTION_PLAY)
        })
        mediaSession = MediaSessionCompat(baseContext,"music")
        MediaControllerCompat(this@PlayMusicService,mediaSession.sessionToken)
        pauseIntent = PendingIntent.getBroadcast(
            this,
            MUSIC_CHANNEL_INT,
            Intent(ACTION_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        playIntent = PendingIntent.getBroadcast(
            this,
            MUSIC_CHANNEL_INT,
            Intent(ACTION_PLAY),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val playLastAction = NotificationCompat.Action.Builder(
            R.drawable.ic_skip_previous_black_24dp,
            getString(R.string.play_prev),
            PendingIntent.getBroadcast(
                this,
                MUSIC_CHANNEL_INT,
                Intent(ACTION_PREV),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        ).build()

        val playNextAction = NotificationCompat.Action.Builder(
            R.drawable.ic_skip_next_black_24dp,
            getString(R.string.play_next),
            PendingIntent.getBroadcast(
                this,
                MUSIC_CHANNEL_INT,
                Intent(ACTION_NEXT),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        ).build()
        playAction = NotificationCompat.Action.Builder(
            R.drawable.ic_play_arrow_black_24dp,
            getString(R.string.play_pause),
            playIntent)
            .build()
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O )
            createNotificationChannel()
        stateBuilder = PlaybackStateCompat.Builder()
        metaDataBuilder = MediaMetadataCompat.Builder()
        notificationBuilder = NotificationCompat.Builder(this,MUSIC_CHANNEL)
            .setSmallIcon(R.drawable.ic_music_note_black_24dp)
            .setShowWhen(false)
            .setContentIntent(
                PendingIntent
                    .getActivity(
                        this,MUSIC_CHANNEL_INT,
                        Intent(this,
                            MusicPlayActivity::class.java
                        ),
                PendingIntent.FLAG_UPDATE_CURRENT
            ))
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0,1,2)
            )
            .addAction(playLastAction)
            .addAction(playAction)
            .addAction(playNextAction).apply {
                if ( Build.VERSION.SDK_INT < Build.VERSION_CODES.O )
                    setOngoing(true)
            }

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(musicReceiver)
        defaultSharedPreference.edit {
            putInt("musicNextPlayType",mBinder.nextPlayType)
        }
        mPlayer.stop()
        mPlayer.release()
        mediaSession.release()
    }

    @TargetApi(Build.VERSION_CODES.O)
    fun createNotificationChannel() {
        getSystemService(NotificationManager::class.java).run {
            createNotificationChannel(NotificationChannel(MUSIC_CHANNEL,
                "音乐控制", NotificationManager.IMPORTANCE_LOW)
                .apply {
                description = "显示正在播放的音乐，控制音乐播放"
            })
        }
    }

    inner class MusicNotificationReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            mBinder.apply {
                when (intent?.action) {
                    ACTION_PLAY -> resume()
                    ACTION_PAUSE -> pause()
                    ACTION_NEXT -> nextPlay()
                    ACTION_PREV -> lastPlay()
                }
            }
        }
    }
    inner class PlayMusicBinder: Binder() {
        private val playHistory = Stack<Int>()
        private val listeners = HashMap<String,MediaPlayer.OnCompletionListener>()
        private val callBacks = HashMap<String,((Int, Int) -> Unit)>()
        private val onPlayListeners = HashMap<String,((MusicModel) -> Unit)>()
        private val onPauseListeners = HashMap<String,((MusicModel) -> Unit)>()
        private val onResumeListeners = HashMap<String,((MusicModel) -> Unit)>()
        private val prepareListeners =
            HashMap<String,MediaPlayer.OnPreparedListener>()
        private var job: Job? = null
        var playIndex = 0
        val isPlaying
            get() = mPlayer.isPlaying
        val playPosition
            get() = mPlayer.currentPosition
        val playDuration
            get() = mPlayer.duration
        var playingPath = ""
        var playList = ArrayList<MusicModel>()
            set(value) {
                if (field != value) {
                    field = value
                    playHistory.clear()
                    playIndex = 0
                }
            }
        val playingMusic:MusicModel
            get() {
                if (playList.size == 0)
                    playList = MusicScanImpl.musicList
                return playList[playIndex]
            }
        var nextPlayType = NEXT_PLAY_RANDOM
            set(value) {
                defaultSharedPreference.edit {
                    putInt("musicNextPlayType", value)
                }
                field = value
            }
        fun play(path: String){
            if (playingPath == path) {
                if (isPlaying.not())
                    resume()
                return
            }
            else
                playingPath = path
            mPlayer.apply {
                if (isPlaying)
                    stop()
                    reset()
                when {
                    path.startsWith("android.resource://") -> setDataSource(this@PlayMusicService, path.toUri())
                    path.isEmpty() -> nextPlay()
                    else -> setDataSource(path)
                }
                    prepare()
                    start()
                onPlayListeners.forEach {
                    it.value.invoke(playingMusic)
                }
            }
        }

        fun pause() {
            mPlayer.pause()
            stopCallBack()
            switchPlayStatus(false)
            onPauseListeners.forEach {
                it.value.invoke(playingMusic)
            }
        }

        fun resume() {
            mPlayer.start()
            startCallBack()
            switchPlayStatus(true)
            onResumeListeners.forEach {
                it.value.invoke(playingMusic)
            }
        }

        fun seekTo(msec: Int) {
            mPlayer.seekTo(msec)
        }

        fun nextPlay() {
            if (nextPlayType != NEXT_PLAY_ONCE||nextPlayType != NEXT_PLAY_SINGLE)
                playHistory.push(playIndex)
            if (playList.isNotEmpty())
            when(nextPlayType) {
                NEXT_PLAY_CIRCLE -> playIndex = (playIndex+1)%playList.size
                NEXT_PLAY_RANDOM -> while (true) {
                    val temp = Random.nextInt(playList.size)
                    if (playList.size==1||temp != playIndex) {
                        playIndex = temp
                        break
                    }
                }
            }
            if (nextPlayType != NEXT_PLAY_ONCE)
                play(playList[playIndex].path)
        }

        fun lastPlay() {
            if (playHistory.isEmpty())
                nextPlay()
            else {
                playIndex = playHistory.pop()
                play(playList[playIndex].path)
            }
        }

        private fun switchPlayStatus(play: Boolean) {
            if (play) {
                playAction.icon = R.drawable.ic_pause_black_24dp
                playAction.actionIntent = pauseIntent
                startForeground(MUSIC_CHANNEL_INT,notificationBuilder.build())
            }else {
                playAction.icon = R.drawable.ic_play_arrow_black_24dp
                playAction.actionIntent = playIntent
                NotificationManagerCompat.from(this@PlayMusicService)
                    .notify(MUSIC_CHANNEL_INT,notificationBuilder.build())
                stopForeground(false)
            }
        }
        private fun loadAlbumCover(path: String): Bitmap? {
            return MediaMetadataRetriever().run {
                if (path.startsWith("android.resource://$packageName/"))
                    setDataSource(this@PlayMusicService,path.toUri())
                else
                    setDataSource(path)
                if (embeddedPicture != null)
                    BitmapFactory
                        .decodeByteArray(embeddedPicture,0,embeddedPicture.size)
                else null
            }
        }


        private fun startCallBack(){
            if (job == null&&callBacks.isNotEmpty())
                job = GlobalScope.launch {
                    while (true) {
                        withContext(Dispatchers.Main) {
                            if (callBacks.isEmpty()) {
                                stopCallBack()
                            }
                            else
                                callBacks.forEach {
                                    it.value.invoke(
                                        mPlayer.currentPosition,
                                        mPlayer.duration
                                    )
                            }
                            mediaSession.setPlaybackState(stateBuilder
                                .setState(
                                    if (isPlaying)
                                        PlaybackStateCompat.STATE_PLAYING
                                    else
                                        PlaybackStateCompat.STATE_PAUSED
                                    ,
                                    playPosition.toLong(),1.0f)
                                .build()
                            )
                        }
                        delay(200)
                    }
                }
        }
        private fun stopCallBack() {
            job?.cancel()
            job = null
        }
        fun onPlayCompletion(mediaPlayer: MediaPlayer){
            mBinder.stopCallBack()
            switchPlayStatus(false)
            stopForeground(false)
            listeners.forEach {
                it.value.onCompletion(mediaPlayer)
            }
            playingMusic.albumCover = null
            nextPlay()
        }

        fun onPlayPrepared(mediaPlayer: MediaPlayer) {
            mBinder.startCallBack()
            playingMusic.albumCover = loadAlbumCover(playingPath)
            playingMusic.apply {
                metaDataBuilder
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,artist)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, artist)
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART,albumCover
                        ?:getDrawable(R.drawable.drawable_gradient)?.bitmap)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
                        mediaPlayer.duration.toLong())
                mediaSession.setMetadata(metaDataBuilder.build())
                notificationBuilder
                    .setContentTitle(title)
                    .setContentText("$artist - $album")
                    .setLargeIcon(albumCover
                        ?: getDrawable(R.drawable.drawable_gradient)?.bitmap)
                switchPlayStatus(true)
            }
            prepareListeners.forEach {
                it.value.onPrepared(mediaPlayer)
            }
        }

        fun addCompletionListener(tag: String,
                                  listener: MediaPlayer.OnCompletionListener) {
            listeners[tag] = listener
        }
        fun addOnPreparedListener(tag: String,
                                  listener: MediaPlayer.OnPreparedListener) {
            prepareListeners[tag] = listener
        }

        fun addOnPlayListener(tag: String,
                              callBack:(it:MusicModel) -> Unit) {
            onPlayListeners[tag] = callBack
            if (isPlaying)
                callBack.invoke(playingMusic)
        }

        fun addOnPauseListener(tag: String,
                               callBack:(it:MusicModel) -> Unit) {
            onPauseListeners[tag] = callBack
            if (playList.isNotEmpty()&&isPlaying.not())
                callBack.invoke(playingMusic)
        }

        fun addOnResumeListener(tag: String,
                                callBack:(it:MusicModel) -> Unit) {
            onResumeListeners[tag] = callBack
        }

        fun addPlayingCallBack(tag: String,
                               callBack:(currentPosition: Int, total: Int) -> Unit) {
            callBacks[tag] = callBack
            if (isPlaying)
                startCallBack()
        }

        fun removeOnResumeListener(tag: String) {
            onResumeListeners.remove(tag)
        }

        fun removeOnPauseListener(tag: String) {
            onPauseListeners.remove(tag)
        }
        fun removeOnPlayListener(tag: String) {
            onPlayListeners.remove(tag)
        }
        fun removeCompletionListener(tag: String) {
            listeners.remove(tag)
        }
        fun removePlayingCallBack(tag: String) {
            callBacks.remove(tag)
        }
        fun removeOnPreparedListener(tag: String) {
            prepareListeners.remove(tag)
        }

        fun removeAllListeners(tag: String) {
            removePlayingCallBack(tag)
            removeCompletionListener(tag)
            removeOnPreparedListener(tag)
            removeOnPauseListener(tag)
            removeOnResumeListener(tag)
            removeOnPlayListener(tag)
        }
    }
}