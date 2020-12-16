package io.iohk.atala.prism.app.data.local.db;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import io.iohk.atala.prism.app.data.local.db.converters.ByteStringConverter;
import io.iohk.atala.prism.app.data.local.db.converters.ActivityHistoryTypeConverter;
import io.iohk.atala.prism.app.data.local.db.dao.ContactDao;
import io.iohk.atala.prism.app.data.local.db.dao.CredentialDao;
import io.iohk.atala.prism.app.data.local.db.dao.ProofRequestDao;
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistory;
import io.iohk.atala.prism.app.data.local.db.model.Contact;
import io.iohk.atala.prism.app.data.local.db.model.Credential;
import io.iohk.atala.prism.app.data.local.db.model.ProofRequest;
import io.iohk.atala.prism.app.data.local.db.model.ProofRequestCredential;
import io.iohk.atala.prism.app.utils.Constants;
import kotlin.jvm.Volatile;

@Database(entities = {Credential.class, Contact.class, ActivityHistory.class, ProofRequest.class, ProofRequestCredential.class}, version = 5)
@TypeConverters({ByteStringConverter.class, ActivityHistoryTypeConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract CredentialDao credentialDao();

    public abstract ContactDao contactDao();

    public abstract ProofRequestDao proofRequestDao();

    public static class Builder {

        private static Object lockObject = new Object();

        @Volatile
        private static AppDatabase instance;

        public static AppDatabase build(Context context) {
            if (instance == null) {
                synchronized (lockObject) {
                    instance = Room.databaseBuilder(context, AppDatabase.class, Constants.DB_NAME)
                            .addMigrations(
                                    MigrationsKt.getMIGRATION_1_2(),
                                    MigrationsKt.getMIGRATION_2_3(),
                                    MigrationsKt.getMIGRATION_3_4(),
                                    MigrationsKt.getMIGRATION_4_5()
                            )
                            .build();
                }
            }
            return instance;
        }
    }
}