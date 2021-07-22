package io.iohk.atala.prism.app.data.local.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payIdPublicKeys",
    indices = [Index("pay_id_local_id"), Index("public_key", unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = PayId::class,
            parentColumns = ["id"],
            childColumns = ["pay_id_local_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PayIdPublicKey(
    @ColumnInfo(name = "pay_id_local_id") val payIdLocalId: Long,
    @ColumnInfo(name = "public_key") val publicKey: String,
    @ColumnInfo(name = "message_id") val messageId: String, // request message id
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}
