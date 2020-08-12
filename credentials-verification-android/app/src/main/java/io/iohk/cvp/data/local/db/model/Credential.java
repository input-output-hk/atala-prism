package io.iohk.cvp.data.local.db.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.protobuf.ByteString;

@Entity(tableName = "credential")
public class Credential{

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
    public String connectionId;

    public Boolean viewed = false;

    @ColumnInfo(name = "credentials_document")
    public String credentialDocument;

}
