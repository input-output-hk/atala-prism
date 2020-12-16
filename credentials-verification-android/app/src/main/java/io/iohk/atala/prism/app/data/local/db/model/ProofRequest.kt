package io.iohk.atala.prism.app.data.local.db.model

import androidx.room.*

@Entity(
        tableName = "proofRequests",
        indices = [Index("connection_id"), Index("message_id", unique = true)],
        foreignKeys = [
            ForeignKey(entity = Contact::class,
                    parentColumns = ["connection_id"],
                    childColumns = ["connection_id"],
                    onDelete = ForeignKey.CASCADE)
        ]
)
data class ProofRequest(
        @ColumnInfo(name = "connection_id") val connectionId: String,
        @ColumnInfo(name = "message_id") val messageId: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}