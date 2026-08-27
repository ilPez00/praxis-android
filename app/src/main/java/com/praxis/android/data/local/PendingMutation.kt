package com.praxis.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_mutations")
data class PendingMutation(
    @PrimaryKey(autoGenerate = true) val mutationId: Long = 0,
    val endpoint: String,
    val method: String,
    val bodyJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * Generated once per logical write and sent on BOTH the initial attempt and
     * every replay. The backend's idempotency middleware stores the first
     * response per (key, user) and returns it on retries, so a request that
     * reached the server but whose response was lost cannot create a second row.
     */
    val idempotencyKey: String = ""
)
