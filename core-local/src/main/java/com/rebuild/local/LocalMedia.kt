package com.rebuild.local

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.room.*

/**
 * M2 · 本地片单 + 播放记录（SAF 扫描本地视频 + Room 持久化）。
 * 纯自研：MediaStore 读系统相册，Room 存收藏/记录，不复用某商业闭源影音App。
 */

@Entity(tableName = "video")
data class LocalVideo(
    @PrimaryKey val _id: Long,
    val title: String,
    val uri: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val lastSeenMs: Long
)

@Entity(tableName = "play_progress")
data class PlayProgress(
    @PrimaryKey val vodId: String,
    val episode: String,
    val positionMs: Long,
    val updatedAtMs: Long
)

@Dao
interface VideoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun upsert(v: LocalVideo)
    @Query("SELECT * FROM video ORDER BY lastSeenMs DESC") fun recent(limit: Int = 50): List<LocalVideo>
    @Query("DELETE FROM video") fun clear()
}

@Dao
interface ProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun upsert(p: PlayProgress)
    @Query("SELECT * FROM play_progress WHERE vodId = :id") fun get(vodId: String): PlayProgress?
}

@Database(entities = [LocalVideo::class, PlayProgress::class], version = 1)
abstract class AppDatabase {
    abstract fun videoDao(): VideoDao
    abstract fun progressDao(): ProgressDao
}

/**
 * 本地视频扫描器（SAF/MediaStore）。
 * 用 MediaStore 查内部/外部存储的视频，映射成 LocalVideo，供"本地片单"页。
 */
class LocalVideoScanner(private val ctx: Context) {

    fun scan(): List<LocalVideo> {
        val out = mutableListOf<LocalVideo>()
        val cols = arrayOf(
            MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION, MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA
        )
        ctx.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cols,
            null, null, MediaStore.Video.Media.DATE_ADDED + " DESC"
        )?.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val name = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dur = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val size = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val data = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            while (c.moveToNext()) {
                out += LocalVideo(
                    _id = c.getLong(id),
                    title = c.getString(name),
                    uri = "content://media/external/video/$data",
                    durationMs = c.getLong(dur),
                    sizeBytes = c.getLong(size),
                    lastSeenMs = System.currentTimeMillis()
                )
            }
        }
        return out
    }
}
