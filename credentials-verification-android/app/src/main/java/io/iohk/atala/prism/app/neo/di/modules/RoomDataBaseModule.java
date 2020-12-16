package io.iohk.atala.prism.app.neo.di.modules;

import android.content.Context;

import androidx.room.Room;

import dagger.Module;
import dagger.Provides;
import io.iohk.atala.prism.app.data.local.db.AppDatabase;
import io.iohk.atala.prism.app.data.local.db.MigrationsKt;
import io.iohk.atala.prism.app.data.local.db.dao.ContactDao;
import io.iohk.atala.prism.app.data.local.db.dao.CredentialDao;
import io.iohk.atala.prism.app.utils.Constants;

@Module
public class RoomDataBaseModule {
    private final Context context;

    public RoomDataBaseModule(Context context) {
        this.context = context;
    }

    @Provides
    public Context context() {
        return context;
    }

    @Provides
    public AppDatabase provideAppDataBase() {
        return AppDatabase.Builder.build(context);
    }

    @Provides
    public ContactDao provideContactDao(AppDatabase appDatabase) {
        return appDatabase.contactDao();
    }

    @Provides
    public CredentialDao provideCredentialDao(AppDatabase appDatabase) {
        return appDatabase.credentialDao();
    }
}