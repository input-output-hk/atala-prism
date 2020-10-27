package io.iohk.atala.prism.app.data.local.db.model

import androidx.room.*

@Entity(tableName = "activityHistories",
        indices = [Index("credential_id"), Index("connection_id")],
        foreignKeys = [
            ForeignKey(entity = Credential::class,
                    parentColumns = ["credential_id"],
                    childColumns = ["credential_id"],
                    onDelete = ForeignKey.CASCADE),
            ForeignKey(entity = Contact::class,
                    parentColumns = ["connection_id"],
                    childColumns = ["connection_id"],
                    onDelete = ForeignKey.CASCADE)
        ])
data class ActivityHistory(
        @ColumnInfo(name = "connection_id") val connectionId: String?,
        @ColumnInfo(name = "credential_id") val credentialId: String?,
        @ColumnInfo(name = "date") val date: Long,
        @ColumnInfo(name = "type") val type: Type
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    enum class Type(val value: Int) {
        ContactAdded(0),
        ContactDeleted(1),
        CredentialIssued(2),
        CredentialShared(3),
        CredentialRequested(4),
        CredentialDeleted(5)
    }
}