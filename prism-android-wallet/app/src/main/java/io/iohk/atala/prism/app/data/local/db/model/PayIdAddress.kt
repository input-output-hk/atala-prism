package io.iohk.atala.prism.app.data.local.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payIdAddresses",
    indices = [Index("pay_id_local_id"), Index("address", unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = PayId::class,
            parentColumns = ["id"],
            childColumns = ["pay_id_local_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PayIdAddress(
    @ColumnInfo(name = "pay_id_local_id") val payIdLocalId: Long,
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "message_id") val messageId: String, // request message id
    @ColumnInfo(name = "status") var status: Status = Status.WaitingForResponse
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    enum class Status(val value: Int) {
        WaitingForResponse(0),
        Registered(1),
    }
}
