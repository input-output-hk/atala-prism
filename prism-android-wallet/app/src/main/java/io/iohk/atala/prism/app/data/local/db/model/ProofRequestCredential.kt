package io.iohk.atala.prism.app.data.local.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "proofRequestCredential",
    indices = [Index("credential_id"), Index("proof_request_id")],
    foreignKeys = [
        ForeignKey(
            entity = Credential::class,
            parentColumns = ["credential_id"],
            childColumns = ["credential_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProofRequest::class,
            parentColumns = ["id"],
            childColumns = ["proof_request_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ProofRequestCredential(
    @ColumnInfo(name = "credential_id") val credentialId: String,
    @ColumnInfo(name = "proof_request_id") val proofRequestId: Long
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}
