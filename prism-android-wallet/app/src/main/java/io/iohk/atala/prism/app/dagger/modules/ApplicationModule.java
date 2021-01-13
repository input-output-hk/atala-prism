package io.iohk.atala.prism.app.dagger.modules;

import android.content.res.Resources;

import io.iohk.atala.prism.app.core.PrismApplication;
import io.iohk.atala.prism.app.data.DataManager;
import io.iohk.atala.prism.app.data.local.AppDataManager;
import io.iohk.atala.prism.app.data.local.db.AppDatabase;
import io.iohk.atala.prism.app.data.local.db.AppDbHelper;
import io.iohk.atala.prism.app.data.local.db.DbHelper;
import io.iohk.atala.prism.app.data.local.db.dao.ContactDao;
import io.iohk.atala.prism.app.data.local.db.dao.CredentialDao;
import io.iohk.atala.prism.app.data.local.preferences.AppPreferencesHelper;
import io.iohk.atala.prism.app.data.local.preferences.PreferencesHelper;
import io.iohk.atala.prism.app.data.local.remote.ApiHelper;
import io.iohk.atala.prism.app.data.local.remote.AppApiHelper;
import io.iohk.atala.prism.app.neo.data.AccountRecoveryRepository;
import io.iohk.atala.prism.app.neo.data.SessionRepository;
import io.iohk.atala.prism.app.neo.data.local.ContactsLocalDataSource;
import io.iohk.atala.prism.app.neo.data.local.ContactsLocalDataSourceInterface;
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSource;
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSourceInterface;
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSource;
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSourceInterface;
import io.iohk.atala.prism.app.neo.data.remote.ConnectorRemoteDataSource;
import io.iohk.atala.prism.app.ui.Navigator;
import dagger.Module;
import dagger.Provides;
import io.iohk.atala.prism.app.data.local.preferences.Preferences;

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
        return AppDatabase.Builder.build(prismApplication.getApplicationContext());
    }

    @Provides
    ContactDao provideContactDao(AppDatabase appDatabase) {
        return appDatabase.contactDao();
    }

    @Provides
    CredentialDao provideCredentialDao(AppDatabase appDatabase) {
        return appDatabase.credentialDao();
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

    /*
     *  [PreferencesLocalDataSource] providers
     * */
    @Provides
    public PreferencesLocalDataSourceInterface providePreferencesLocalDataSource(PrismApplication prismApplication) {
        return new PreferencesLocalDataSource(prismApplication.getApplicationContext());
    }

    /*
     *  SessionRepository providers
     * */

    @Provides
    public SessionLocalDataSourceInterface provideSessionLocalDataSource(PrismApplication prismApplication) {
        return new SessionLocalDataSource(prismApplication.getApplicationContext());
    }

    @Provides
    public SessionRepository provideSessionRepository(SessionLocalDataSourceInterface sessionLocalDataSource, PreferencesLocalDataSourceInterface preferencesLocalDataSource) {
        return new SessionRepository(sessionLocalDataSource, preferencesLocalDataSource);
    }

    /*
     * API Providers
     * */

    @Provides
    public ConnectorRemoteDataSource provideConnectorRemoteDataSource(PreferencesLocalDataSourceInterface preferencesLocalDataSource) {
        return new ConnectorRemoteDataSource(preferencesLocalDataSource);
    }

    /*
     * AccountRecoveryRepository providers
     * */

    @Provides
    public ContactsLocalDataSourceInterface provideContactsLocalDataSource(ContactDao contactDao) {
        return new ContactsLocalDataSource(contactDao);
    }

    @Provides
    public AccountRecoveryRepository provideSessionLocalDataSourceInterface(SessionLocalDataSourceInterface sessionLocalDataSource,
                                                                            PreferencesLocalDataSourceInterface preferencesLocalDataSource,
                                                                            ContactsLocalDataSourceInterface contactsLocalDataSource,
                                                                            ConnectorRemoteDataSource connectorRemoteDataSource) {
        return new AccountRecoveryRepository(sessionLocalDataSource, preferencesLocalDataSource, contactsLocalDataSource, connectorRemoteDataSource);
    }
}