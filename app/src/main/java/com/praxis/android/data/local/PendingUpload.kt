package com.praxis.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A captured media file (photo / audio / video) waiting to reach Supabase
 * Storage.
 *
 * Uploads are their own queue rather than a flavour of [PendingMutation] for
 * one reason: the payload is a file on disk, not JSON. The mutation queue can
 * replay bytes from a column; this queue needs the file to still exist, so it
 * records everything needed to re-upload (local path, destination path,
 * content type) and is drained by the sync worker before mutations — an entry
 * that references a media URL must not be sent before its upload landed.
 */
@Entity(tableName = "pending_uploads")
data class PendingUpload(
    @PrimaryKey(autoGenerate = true) val uploadId: Long = 0,
    /** Absolute path of the captured file in app-private storage. */
    val localPath: String,
    /** Destination key inside the notebook-files bucket. */
    val storagePath: String,
    val mimeType: String,
    val createdAt: Long = System.currentTimeMillis(),
)
