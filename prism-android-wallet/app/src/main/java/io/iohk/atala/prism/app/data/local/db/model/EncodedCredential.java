package io.iohk.atala.prism.app.data.local.db.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.protobuf.ByteString;

@Entity(tableName = "encodedCredentials",
        indices = {@Index(value = "credential_id", unique = true)},
        foreignKeys = {
                @ForeignKey(
                        entity = Credential.class,
                        parentColumns = {"credential_id"},
                        childColumns = {"credential_id"},
                        onDelete = ForeignKey.CASCADE
                )
        }
)
public class EncodedCredential {
    @PrimaryKey(autoGenerate = true)
    public Long id;

    @ColumnInfo(name = "credential_id")
    public String credentialId;
    /**
     * This ByteString can represent 2 different data models, if it is a Demo credential this will
     * be the bytes of an object [io.iohk.atala.prism.protos.Credential].
     * if it is a PlainTextCredential it means that they must be the bytes corresponding
     * to an [io.iohk.atala.prism.protos.AtalaMessage] object.
     */
    @ColumnInfo(name = "credential_encoded", typeAffinity = ColumnInfo.BLOB)
    public ByteString credentialEncoded;
}
