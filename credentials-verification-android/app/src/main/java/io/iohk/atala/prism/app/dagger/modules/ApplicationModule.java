package io.iohk.atala.prism.app.dagger.modules;

import android.content.res.Resources;

import androidx.room.Room;

import io.iohk.atala.prism.app.core.PrismApplication;
import io.iohk.atala.prism.app.data.DataManager;
import io.iohk.atala.prism.app.data.local.AppDataManager;

import io.iohk.atala.prism.app.data.local.db.AppDatabase;
import io.iohk.atala.prism.app.data.local.db.AppDbHelper;
import io.iohk.atala.prism.app.data.local.db.DbHelper;
import io.iohk.atala.prism.app.data.local.db.MigrationsKt;
import io.iohk.atala.prism.app.data.local.preferences.AppPreferencesHelper;
import io.iohk.atala.prism.app.data.local.preferences.PreferencesHelper;
import io.iohk.atala.prism.app.data.local.remote.ApiHelper;
import io.iohk.atala.prism.app.data.local.remote.AppApiHelper;
import io.iohk.atala.prism.app.utils.Constants;
import io.iohk.atala.prism.app.views.Navigator;
import dagger.Module;
import dagger.Provides;
import io.iohk.atala.prism.app.views.Preferences;

import javax.inject.Singleton;

@Module
public class ApplicationModule {

    @Provides
    @Singleton
    public Navigator provideNavigator() {
        return new Navigator();
    }

    @Provides
    @Singleton
    AppDatabase provideAppDatabase(PrismApplication prismApplication) {
        return Room.databaseBuilder(prismApplication.getApplicationContext(), AppDatabase.class, Constants.DB_NAME)
                .addMigrations(
                        MigrationsKt.getMIGRATION_1_2(),
                        MigrationsKt.getMIGRATION_2_3()
                )
                .build();
    }

    /*
     * TODO this is just to help handle legacy code which is used to locally compute the name of the credentials based on the type of credential
     *  when there is a proper way to get that data from the backend this should be removed, this is directly related to the dependencies that
     *  the [MyCredentialsViewModel] class needs
     * */
    @Provides
    Resources provideResources(PrismApplication prismApplication) {
        return prismApplication.getApplicationContext().getResources();
    }

    @Provides
    @Singleton
    DataManager provideDataManager(AppDataManager appDataManager) {
        return appDataManager;
    }

    @Provides
    @Singleton
    DbHelper provideDbHelper(AppDbHelper appDbHelper) {
        return appDbHelper;
    }

    @Provides
    @Singleton
    ApiHelper provideApiHelper(AppApiHelper appApiHelper) {
        return appApiHelper;
    }

    @Provides
    @Singleton
    PreferencesHelper provideAppPreferences(AppPreferencesHelper appPreferencesHelper) {
        return appPreferencesHelper;
    }

    @Provides
    @Singleton
    Preferences providePreferences(PrismApplication prismApplication) {
        return new Preferences(prismApplication);
    }

}