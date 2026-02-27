package com.freytube.app.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ═══════════════════════════════════════════════════════════════
// Download Entity
// ═══════════════════════════════════════════════════════════════

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val videoId: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "thumbnail") val thumbnail: String,
    @ColumnInfo(name = "uploader") val uploader: String,
    @ColumnInfo(name = "duration") val duration: Long,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "file_size") val fileSize: Long = 0,
    @ColumnInfo(name = "quality") val quality: String = "",
    @ColumnInfo(name = "download_progress") val downloadProgress: Int = 0,
    @ColumnInfo(name = "status") val status: DownloadStatus = DownloadStatus.PENDING,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)

enum class DownloadStatus {
    PENDING, DOWNLOADING, COMPLETED, FAILED, PAUSED
}

// ═══════════════════════════════════════════════════════════════
// Watch History Entity
// ═══════════════════════════════════════════════════════════════

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val videoId: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "thumbnail") val thumbnail: String,
    @ColumnInfo(name = "uploader") val uploader: String,
    @ColumnInfo(name = "uploader_url") val uploaderUrl: String = "",
    @ColumnInfo(name = "duration") val duration: Long,
    @ColumnInfo(name = "progress_position") val progressPosition: Long = 0,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)

// ═══════════════════════════════════════════════════════════════
// Subscription Entity (Local subscriptions)
// ═══════════════════════════════════════════════════════════════

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val channelId: String,
    @ColumnInfo(name = "channel_name") val channelName: String,
    @ColumnInfo(name = "avatar_url") val avatarUrl: String = "",
    @ColumnInfo(name = "subscriber_count") val subscriberCount: Long = 0,
    @ColumnInfo(name = "verified") val verified: Boolean = false,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)

// ═══════════════════════════════════════════════════════════════
// DAOs
// ═══════════════════════════════════════════════════════════════

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE videoId = :videoId")
    suspend fun getDownload(videoId: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Delete
    suspend fun deleteDownload(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE videoId = :videoId")
    suspend fun deleteById(videoId: String)

    @Query("UPDATE downloads SET download_progress = :progress, status = :status WHERE videoId = :videoId")
    suspend fun updateProgress(videoId: String, progress: Int, status: DownloadStatus)
}

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 20): Flow<List<WatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE videoId = :videoId")
    suspend fun deleteById(videoId: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearAll()

    @Query("UPDATE watch_history SET progress_position = :position, timestamp = :timestamp WHERE videoId = :videoId")
    suspend fun updateProgress(videoId: String, position: Long, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY channel_name ASC")
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE channelId = :channelId")
    suspend fun getSubscription(channelId: String): SubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: SubscriptionEntity)

    @Delete
    suspend fun deleteSubscription(subscription: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE channelId = :channelId")
    suspend fun deleteById(channelId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM subscriptions WHERE channelId = :channelId)")
    suspend fun isSubscribed(channelId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM subscriptions WHERE channelId = :channelId)")
    fun isSubscribedFlow(channelId: String): Flow<Boolean>
}

// ═══════════════════════════════════════════════════════════════
// Database
// ═══════════════════════════════════════════════════════════════

@Database(
    entities = [
        DownloadEntity::class,
        WatchHistoryEntity::class,
        SubscriptionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun subscriptionDao(): SubscriptionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "freytube_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
