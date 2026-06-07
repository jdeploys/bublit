package com.bublit.app.cache

import androidx.room.ColumnInfo
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase

@Database(
    entities = [
        CachedImageRecord::class,
        CachedTranslationBlock::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class BublitDatabase : RoomDatabase()

@Entity(tableName = "cached_images")
data class CachedImageRecord(
    @PrimaryKey
    @ColumnInfo(name = "image_hash")
    val imageHash: String,
    @ColumnInfo(name = "source_url")
    val sourceUrl: String,
    @ColumnInfo(name = "page_url")
    val pageUrl: String,
    @ColumnInfo(name = "completed_bitmap_path")
    val completedBitmapPath: String?,
    @ColumnInfo(name = "updated_at_epoch_ms")
    val updatedAtEpochMs: Long,
)

@Entity(tableName = "cached_translation_blocks")
data class CachedTranslationBlock(
    @PrimaryKey
    @ColumnInfo(name = "block_hash")
    val blockHash: String,
    @ColumnInfo(name = "image_hash")
    val imageHash: String,
    @ColumnInfo(name = "source_text")
    val sourceText: String,
    @ColumnInfo(name = "translated_text")
    val translatedText: String,
    @ColumnInfo(name = "source_language")
    val sourceLanguage: String,
    @ColumnInfo(name = "engine_version")
    val engineVersion: String,
)
