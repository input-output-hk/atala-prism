package io.iohk.atala.prism.app.data.local.db.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "contact", indices = {@Index(value = "connection_id", unique = true)})
public class Contact {

    @PrimaryKey(autoGenerate = true)
    public Long id;

    @ColumnInfo(name = "connection_id")
    public String connectionId;

    @ColumnInfo(name = "date_created")
    public Long dateCreated;

    @ColumnInfo(name = "did")
    public String did;

    @ColumnInfo(name = "last_message_id")
    public String lastMessageId;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "token")
    public String token;

    @ColumnInfo(name = "key_derivation_path")
    public String keyDerivationPath;

    public byte[] logo;
}
