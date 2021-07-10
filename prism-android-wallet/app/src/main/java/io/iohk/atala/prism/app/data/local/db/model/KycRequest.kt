package io.iohk.atala.prism.app.data.local.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "kycRequests",
    indices = [Index("connection_id"), Index("message_id", unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = Contact::class,
            parentColumns = ["connection_id"],
            childColumns = ["connection_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class KycRequest(
    @ColumnInfo(name = "connection_id") val connectionId: String,
    @ColumnInfo(name = "message_id") val messageId: String,
    @ColumnInfo(name = "bearer_token") val bearerToken: String,
    @ColumnInfo(name = "instance_id") val instanceId: String,
    @ColumnInfo(name = "skipped", defaultValue = "0") var skipped: Boolean = false
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}
