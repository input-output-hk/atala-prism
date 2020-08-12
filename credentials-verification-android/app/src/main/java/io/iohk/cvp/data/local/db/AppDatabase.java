package io.iohk.cvp.data.local.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import io.iohk.cvp.data.local.db.converters.ByteStringConverter;
import io.iohk.cvp.data.local.db.dao.ContactDao;
import io.iohk.cvp.data.local.db.dao.CredentialDao;
import io.iohk.cvp.data.local.db.dao.CredentialHistoryDao;
import io.iohk.cvp.data.local.db.model.Contact;
import io.iohk.cvp.data.local.db.model.Credential;
import io.iohk.cvp.data.local.db.model.CredentialHistory;

@Database(entities = {Credential.class, CredentialHistory.class, Contact.class}, version = 1)
@TypeConverters(ByteStringConverter.class)
public abstract class AppDatabase extends RoomDatabase {
    public abstract CredentialDao credentialDao();
    public abstract CredentialHistoryDao credentialHistoryDao();
    public abstract ContactDao contactDao();

}
