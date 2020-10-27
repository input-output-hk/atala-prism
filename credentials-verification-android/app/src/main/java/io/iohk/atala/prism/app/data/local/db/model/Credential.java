package io.iohk.atala.prism.app.data.local.db.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.protobuf.ByteString;

@Entity(tableName = "credentials",
        indices = {@Index("connection_id"), @Index(value = "credential_id", unique = true)},
        foreignKeys = {
                @ForeignKey(
                        entity = Contact.class,
                        parentColumns = {"connection_id"},
                        childColumns = {"connection_id"},
                        onDelete = ForeignKey.CASCADE
                )
        }
)
public class Credential {

    @PrimaryKey(autoGenerate = true)
    public Long id;

    @ColumnInfo(name = "credential_id")
    public String credentialId;

    @ColumnInfo(name = "date_received")
    public Long dateReceived;

    @ColumnInfo(name = "credential_encoded", typeAffinity = ColumnInfo.BLOB)
    public ByteString credentialEncoded;

    @ColumnInfo(name = "html_view")
    public String htmlView;

    @ColumnInfo(name = "issuer_id")
    public String issuerId;

    @ColumnInfo(name = "issuer_name")
    public String issuerName;

    @ColumnInfo(name = "credential_type")
    public String credentialType;

    @ColumnInfo(name = "connection_id")
    @NonNull
    public String connectionId;

    @ColumnInfo(name = "credentials_document")
    public String credentialDocument;

    @ColumnInfo(name = "deleted", defaultValue = "false")
    public Boolean deleted = false;
}