package com.sanlorng.coromusic;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.*;
import android.preference.PreferenceManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.sanlorng.coromusic.model.MusicModel;
import com.sanlorng.coromusic.ui.main.MusicPlayActivity;
import com.sanlorng.coromusic.work.mvp.MusicScanImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Stack;

//这是java 示例，实际运行的service 在service 包里
public class PlayMusicService extends Service {
    static String MUSIC_CHANNEL = "1";
    static int MUSIC_CHANNEL_INT = 1 ;
    static String PACKAGE_NAME = BuildConfig.APPLICATION_ID;
    static String ACTION_PLAY = PACKAGE_NAME + ".play";
    static String ACTION_PAUSE = PACKAGE_NAME + ".pause";
    static String ACTION_NEXT = PACKAGE_NAME + ".next";
    static String ACTION_PREV = PACKAGE_NAME + ".prev";
    static String PLAY_TYPE_KEY = "next_play_type";
    static int NEXT_PLAY_RANDOM = 0;
    static int NEXT_PLAY_CIRCLE = 1;
    static int NEXT_PLAY_SINGLE = 2;
    static int NEXT_PLAY_ONCE = 3;
    private MusicBinder mBinder = new MusicBinder();
    private MediaPlayer mPlayer = new MediaPlayer();
    private MusicReceiver mReceiver = new MusicReceiver();
    private SharedPreferences defaultSharedPreference;
    private MediaSessionCompat mediaSession;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationCompat.Action resumeAction;
    private PendingIntent pauseIntent;
    private PendingIntent resumeIntent;
    private MediaMetadataCompat.Builder metaDataBuilder;
    private PlaybackStateCompat.Builder stateBuilder;
    @Override
    public void onCreate() {
        super.onCreate();
        defaultSharedPreference = PreferenceManager.getDefaultSharedPreferences(PlayMusicService.this);
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mBinder.onPlayCompletion(mp);
            }
        });

        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mBinder.onPlayPrepared(mp);
            }
        });

        mBinder.setNextPlayType(defaultSharedPreference.getInt(PLAY_TYPE_KEY,NEXT_PLAY_RANDOM));
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_NEXT);
        intentFilter.addAction(ACTION_PAUSE);
        intentFilter.addAction(ACTION_PREV);
        intentFilter.addAction(ACTION_PLAY);
        registerReceiver(mReceiver,intentFilter);

        mediaSession = new MediaSessionCompat(this, PACKAGE_NAME);
        try {
            new MediaControllerCompat(this, mediaSession.getSessionToken());
        }catch (RemoteException e) {
            e.printStackTrace();
        }

        pauseIntent = PendingIntent.getBroadcast(this,MUSIC_CHANNEL_INT,
                new Intent(ACTION_PAUSE),PendingIntent.FLAG_UPDATE_CURRENT);
        resumeIntent = PendingIntent.getBroadcast(this,MUSIC_CHANNEL_INT,
                new Intent(ACTION_PLAY),PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action playPrevAction = new NotificationCompat.Action.Builder(
                R.drawable.ic_skip_previous_black_24dp,
                getString(R.string.play_prev),
                PendingIntent.getBroadcast(this,MUSIC_CHANNEL_INT,
                        new Intent(ACTION_PREV),PendingIntent.FLAG_UPDATE_CURRENT))
                .build();

        NotificationCompat.Action playNextAction = new NotificationCompat.Action.Builder(
                R.drawable.ic_skip_next_black_24dp,
                getString(R.string.play_next),
                PendingIntent.getBroadcast(this,MUSIC_CHANNEL_INT,
                        new Intent(ACTION_NEXT),PendingIntent.FLAG_UPDATE_CURRENT))
                .build();
        resumeAction = new NotificationCompat.Action.Builder(
                R.drawable.ic_play_arrow_black_24dp,
                getString(R.string.play_pause),
                resumeIntent)
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannel();
        stateBuilder = new PlaybackStateCompat.Builder();
        metaDataBuilder = new MediaMetadataCompat.Builder();
        notificationBuilder = new NotificationCompat.Builder(this,MUSIC_CHANNEL)
                .setSmallIcon(R.drawable.ic_music_note_black_24dp)
                .setShowWhen(false)
                .setContentIntent(PendingIntent.getActivity(this,MUSIC_CHANNEL_INT
                        , new Intent(this, MusicPlayActivity.class),PendingIntent.FLAG_UPDATE_CURRENT
            ))
            .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.getSessionToken())
                .setShowActionsInCompactView(0,1,2))
                .addAction(playPrevAction)
                .addAction(resumeAction)
                .addAction(playNextAction);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            notificationBuilder.setOngoing(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SharedPreferences.Editor editor = defaultSharedPreference.edit();
        editor.putInt(PLAY_TYPE_KEY,mBinder.nextPlayType);
        editor.apply();
        mPlayer.stop();
        mPlayer.release();
        mediaSession.release();
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(MUSIC_CHANNEL,"音乐控制", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("显示正在播放的音乐，控制音乐播放" );
            manager.createNotificationChannel(channel);
    }
    class MusicBinder extends Binder {
        private HashMap<String,MediaPlayCallback> callbackMap = new HashMap<>();
        private Stack<Integer> playHistory = new Stack<>();
        private String playingPath = "";
        private int playIndex = 0;
        private ArrayList<MusicModel> playList = new ArrayList<>();
        private int nextPlayType = NEXT_PLAY_RANDOM;
        private Thread job;
        void setPlayList(ArrayList<MusicModel> playList) {
            if (playList != this.playList) {
                this.playList = playList;
                playHistory.clear();
                playIndex = 0;
            }
        }
        MusicModel getPlayingMusic() {
            if (playList.size() == 0)
                playList = MusicScanImpl.Companion.getMusicList();
            return playList.get(playIndex);
        }

        void play(String path) {
            if (playingPath.equals(path)) {
                if (!isPlaying()) {
                    resume();
                }
                return;
            }else
                playingPath = path;
            if (isPlaying())
                mPlayer.stop();
            mPlayer.reset();
            try{
                if (path.isEmpty())
                    playNext();
                else
                    mPlayer.setDataSource(path);
                mPlayer.prepare();
                mPlayer.start();
                for (MediaPlayCallback item:
                     callbackMap.values()) {
                    item.onPlay(getPlayingMusic());
                }
            }catch (IOException e) {
                e.printStackTrace();
            }
        }

        void pause() {
            mPlayer.pause();
            stopPlayingCallback();
            switchPlayStatus(false);
            for (MediaPlayCallback item:
                    callbackMap.values()) {
                item.onPause(getPlayingMusic());
            }
        }

        void resume() {
            mPlayer.start();
            startPlayingCallback();
            switchPlayStatus(true);
            for (MediaPlayCallback item:
                    callbackMap.values()) {
                item.onResume(getPlayingMusic());
            }
        }
        void seekTo(int msec) {
            mPlayer.seekTo(msec);
        }
        void playNext() {
            if (nextPlayType != NEXT_PLAY_ONCE||nextPlayType != NEXT_PLAY_SINGLE)
                playHistory.push(playIndex);

            if (!playList.isEmpty()) {
                if (nextPlayType == NEXT_PLAY_CIRCLE)
                    playIndex = (playIndex + 1) % playList.size();
                else if (nextPlayType == NEXT_PLAY_RANDOM)
                    while (true) {
                        int temp = new Random().nextInt(playList.size());
                        if (playList.size() == 1 || temp != playIndex) {
                            playIndex = temp;
                            break;
                        }
                    }
            }
            if (nextPlayType != NEXT_PLAY_ONCE)
                play(playList.get(playIndex).getPath());
        }

        void playPrevious() {
            if (playHistory.isEmpty())
                playNext();
            else {
                playIndex = playHistory.pop();
                play(playList.get(playIndex).getPath());
            }
        }
        private void switchPlayStatus(boolean isPlay) {
            if (isPlay) {
                resumeAction.icon = R.drawable.ic_pause_black_24dp;
                resumeAction.actionIntent = pauseIntent;
                startForeground(MUSIC_CHANNEL_INT,notificationBuilder.build());
            }else {
                resumeAction.icon = R.drawable.ic_play_arrow_black_24dp;
                resumeAction.actionIntent = resumeIntent;
                NotificationManagerCompat.from(PlayMusicService.this)
                    .notify(MUSIC_CHANNEL_INT,notificationBuilder.build());
                stopForeground(false);
            }
        }

        private Bitmap loadAlbumCover(String path) {
            MediaMetadataRetriever metadata = new MediaMetadataRetriever();
            metadata.setDataSource(path);
            if (metadata.getEmbeddedPicture() != null)
                return BitmapFactory.decodeByteArray(metadata.getEmbeddedPicture(),0,metadata.getEmbeddedPicture().length);
            else
                return null;
        }
        private void startPlayingCallback() {
            if (job == null && !callbackMap.isEmpty())
                job = new Thread() {
                    @Override
                    public void run() {
                        while (true) {
                            Handler handler = new Handler(getMainLooper());
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (callbackMap.isEmpty()) {
                                        stopPlayingCallback();
                                    }else
                                        for (MediaPlayCallback item:
                                                callbackMap.values()
                                        ) {
                                            item.onPlaying(getPlayItemCurrentPosition(),getPlayItemDuration());
                                        }
                                    int state;
                                    if (isPlaying())
                                        state = PlaybackStateCompat.STATE_PLAYING;
                                    else
                                        state = PlaybackStateCompat.STATE_PAUSED;
                                    mediaSession.setPlaybackState(stateBuilder
                                            .setState(state,getPlayItemCurrentPosition(),1.0f)
                                            .build());
                                }
                            });

                          try {
                              sleep(200);
                          }catch (InterruptedException e) {
                              e.printStackTrace();
                          }
                        }

                    }
                };
            if (job != null)
                job.start();
        }

        private void stopPlayingCallback() {
            if (job != null)
                job.interrupt();
            job = null;
        }

        void setNextPlayType(int nextPlayType) {
            SharedPreferences.Editor editor = defaultSharedPreference.edit();
            editor.putInt(PLAY_TYPE_KEY,nextPlayType);
            editor.apply();
            this.nextPlayType = nextPlayType;
        }
        boolean isPlaying() {
            return mPlayer.isPlaying();
        }

        long getPlayItemDuration() {
            return mPlayer.getDuration();
        }

        long getPlayItemCurrentPosition() {
            return mPlayer.getCurrentPosition();
        }
        private void onPlayPrepared(MediaPlayer mediaPlayer) {
            mBinder.startPlayingCallback();
            MusicModel item = getPlayingMusic();
            item.setAlbumCover(loadAlbumCover(playingPath));
            metaDataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST,item.getArtist())
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, item.getAlbum())
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, item.getArtist())
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART,item.getAlbumCover())
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,mediaPlayer.getDuration());
            mediaSession.setMetadata(metaDataBuilder.build());
            notificationBuilder
                    .setContentTitle(item.getTitle())
                    .setContentText("$artist - $album")
                    .setLargeIcon(item.getAlbumCover());
            switchPlayStatus(true);
            for (MediaPlayCallback callback: callbackMap.values()
                 ) {
                callback.onPlayPrepared(mediaPlayer);
            }
        }

        private void onPlayCompletion(MediaPlayer mediaPlayer) {
            mBinder.stopPlayingCallback();
            switchPlayStatus(false);
            stopForeground(false);
            for (MediaPlayCallback item: callbackMap.values()
            ) {
                item.onPlayCompletion(mediaPlayer);
            }
            getPlayingMusic().setAlbumCover(null);
            playNext();
        }

        void addMediaPlayCallback(String tag, MediaPlayCallback callback) {
            callbackMap.put(tag, callback);
            if (isPlaying()) {
                callback.onPlay(getPlayingMusic());
                callback.onPlaying(getPlayItemCurrentPosition(), getPlayItemDuration());
                startPlayingCallback();
            }else if (!playList.isEmpty()){
                callback.onPause(getPlayingMusic());
            }
        }

        void removeMediaPlayCallback(String tag) {
            callbackMap.remove(tag);
        }
    }

    interface MediaPlayCallback {
        void onPlayCompletion(MediaPlayer mediaPlayer);
        void onPlayPrepared(MediaPlayer mediaPlayer);
        void onPlay(MusicModel playItem);
        void onPause(MusicModel playItem);
        void onResume(MusicModel playItem);
        void onPlaying(long currentPosition, long duration);
    }
    class MusicReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(ACTION_PLAY))
                    mBinder.resume();
                else if (action.equals(ACTION_PAUSE))
                    mBinder.pause();
                else if (action.equals(ACTION_NEXT))
                    mBinder.playNext();
                else if (action.equals(ACTION_PREV))
                    mBinder.playPrevious();
            }
        }
    }
}
