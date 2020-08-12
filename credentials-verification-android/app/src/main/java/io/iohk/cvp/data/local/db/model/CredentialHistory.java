package io.iohk.cvp.data.local.db.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(tableName = "credential_history", foreignKeys = @ForeignKey(entity = Credential.class,
        parentColumns = "id",
        childColumns = "credential_id",
        onDelete = CASCADE))
public class CredentialHistory {

    @PrimaryKey(autoGenerate = true)
    public Long id;

    @ColumnInfo(name = "date_shared")
    public Long dateShared;

    @ColumnInfo(name = "is_requested")
    public Boolean isRequested;

    @ColumnInfo(name = "credential_id")
    public Long credentialId;

    @ColumnInfo(name = "share_connection_id")
    public Long shareConnectionId;
}