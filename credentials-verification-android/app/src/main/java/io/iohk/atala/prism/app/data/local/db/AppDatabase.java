package io.iohk.atala.prism.app.data.local.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import io.iohk.atala.prism.app.data.local.db.converters.ByteStringConverter;
import io.iohk.atala.prism.app.data.local.db.converters.ActivityHistoryTypeConverter;
import io.iohk.atala.prism.app.data.local.db.dao.ContactDao;
import io.iohk.atala.prism.app.data.local.db.dao.CredentialDao;
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistory;
import io.iohk.atala.prism.app.data.local.db.model.Contact;
import io.iohk.atala.prism.app.data.local.db.model.Credential;

@Database(entities = {Credential.class, Contact.class, ActivityHistory.class}, version = 3)
@TypeConverters({ByteStringConverter.class, ActivityHistoryTypeConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract CredentialDao credentialDao();

    public abstract ContactDao contactDao();
}