package io.iohk.atala.prism.app.data.local.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payIds",
    indices = [Index("connection_id", unique = true), Index("name", unique = true), Index("message_id", unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = Contact::class,
            parentColumns = ["connection_id"],
            childColumns = ["connection_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PayId(
    @ColumnInfo(name = "connection_id") val connectionId: String,
    @ColumnInfo(name = "name") var name: String?,
    @ColumnInfo(name = "message_id") val messageId: String?, // request message id
    @ColumnInfo(name = "status") var status: Status = Status.WaitingForResponse
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    enum class Status(val value: Int) {
        WaitingForResponse(0),
        Registered(1),
    }
}
