package io.iohk.cvp.data.local.db.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.crashlytics.android.Crashlytics;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Date;

import io.iohk.prism.protos.AtalaMessage;
import io.iohk.prism.protos.ReceivedMessage;

@Entity(tableName = "credential")
public class Credential {

    @PrimaryKey(autoGenerate = true)
    public Long id;

    @ColumnInfo(name = "credential_id")
    public String credentialId;

    @ColumnInfo(name = "date_received")
    public Long dateReceived;

    @ColumnInfo(name = "credentials_encoded")
    public byte[] credential_encoded;

    @ColumnInfo(name = "html_view")
    public String htmlView;

    @ColumnInfo(name = "issuer_id")
    public String issuerId;

    @ColumnInfo(name = "issuer_name")
    public String issuerName;

    @ColumnInfo(name = "credential_type")
    public String credentialType;

    public Boolean viewed = false;

}
