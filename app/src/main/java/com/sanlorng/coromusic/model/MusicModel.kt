package com.sanlorng.coromusic.model

import android.graphics.Bitmap

abstract class BaseMusicModel(val info: String)
abstract class ListMusicModel(info: String):BaseMusicModel(info) {
    var list :ArrayList<MusicModel>? = null
}
data class ArtistModel(val name: String): ListMusicModel(name)
data class FolderModel(val path: String): ListMusicModel(path) {
    val folderName: String = path.split("/").last()
}
data class AlbumModel(val name: String): ListMusicModel(name)
data class MusicModel(val id: Long,
                      val path: String,
                      val title: String,
                      val artist: String,
                      val album: String,
                      val albumId: Long,
                      val duration: Long,
                      val fileName: String,
                      val fileSize: Long):BaseMusicModel(fileName) {
    var albumCover: Bitmap? = null
}

data class MusicRequest(val type:String, val key: String)
enum class RequestType {
    ARTIST,
    ALBUM,
    FOLDER,
    LIST,
    NULL
}