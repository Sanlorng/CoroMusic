package com.sanlorng.coromusic.work.mvp

import android.content.Context
import android.provider.BaseColumns
import android.provider.MediaStore
import com.sanlorng.coromusic.R
import com.sanlorng.coromusic.model.AlbumModel
import com.sanlorng.coromusic.model.ArtistModel
import com.sanlorng.coromusic.model.FolderModel
import com.sanlorng.coromusic.model.MusicModel
import kotlinx.coroutines.*

class MusicScanImpl {
    private var scanJob: Job? = null
    private val musicList = ArrayList<MusicModel>()
    private val albumList = ArrayList<AlbumModel>()
    private val artistList = ArrayList<ArtistModel>()
    private val folderList = ArrayList<FolderModel>()
    fun scanMediaStore(context: Context,callback: (isEmpty: Boolean) -> Unit) {
        scanJob?.cancel()
        scanJob = GlobalScope.launch {
            musicList.clear()
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                musicSelection,
                null,
                null,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER
            )?.apply {
                val idIndex =
                    getColumnIndex(BaseColumns._ID)
                val durationIndex =
                    getColumnIndex(MediaStore.Audio.Media.DURATION)
                val titleIndex =
                    getColumnIndex(MediaStore.Audio.AudioColumns.TITLE)
                val artistIndex =
                    getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST)
                val albumIndex =
                    getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM)
                val albumIdIndex =
                    getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID)
                val pathIndex =
                    getColumnIndex(MediaStore.Audio.AudioColumns.DATA)
                val fileNameIndex =
                    getColumnIndex(MediaStore.Audio.AudioColumns.DISPLAY_NAME)
                val fileSizeIndex =
                    getColumnIndex(MediaStore.Audio.Media.SIZE)
                while (moveToNext()) {
                    if (getLong(durationIndex) > 30000)
                        musicList.add(MusicModel(
                            id = getLong(idIndex),
                            title = getString(titleIndex),
                            artist = getString(artistIndex),
                            album= getString(albumIndex),
                            albumId = getLong(albumIdIndex),
                            duration = getLong(durationIndex),
                            path= getString(pathIndex),
                            fileName= getString(fileNameIndex),
                            fileSize = getLong(fileSizeIndex)
                        ))
                }
                close()
            }
            folderList.apply {
                clear()
                musicList.groupBy {
                    it.path.split("/").run {
                        var temp = ""
                        for (index in 0 until lastIndex)
                            temp += get(index) + "/"
                        temp
                    }
                }.apply {
                    forEach {
                        add(FolderModel(it.key).apply {
                            list = ArrayList(it.value)
                        })
                    }
                }
            }
            artistList.apply {
                musicList.groupBy {
                    it.artist
                }.apply {
                    forEach {
                        add(ArtistModel(it.key).apply {
                            list = ArrayList(it.value)
                        })
                    }
                }
            }
            albumList.apply {
                musicList.groupBy {
                    it.album
                }.apply {
                    forEach {
                        add(AlbumModel(it.key).apply {
                            list = ArrayList(it.value)
                        })
                    }
                }
            }
            withContext(Dispatchers.Main) {
                callback.invoke(musicList.isEmpty())
            }
        }
    }

    companion object {
        private val instance = MusicScanImpl()
        val musicList
        get() = instance.musicList
        val albumList
        get() = instance.albumList
        val artistList
        get() = instance.artistList
        val folderList
        get() = instance.folderList
        private val musicSelection = arrayOf(
            BaseColumns._ID,
            MediaStore.Audio.AudioColumns.IS_MUSIC,
            MediaStore.Audio.AudioColumns.TITLE,
            MediaStore.Audio.AudioColumns.ARTIST,
            MediaStore.Audio.AudioColumns.ALBUM,
            MediaStore.Audio.AudioColumns.ALBUM_ID,
            MediaStore.Audio.AudioColumns.DATA,
            MediaStore.Audio.AudioColumns.DISPLAY_NAME,
            MediaStore.Audio.AudioColumns.SIZE,
            MediaStore.Audio.AudioColumns.DURATION)
        fun scanMediaStore(context: Context,callback:((isEmpty: Boolean) -> Unit)) = instance.scanMediaStore(context,callback)
    }
}