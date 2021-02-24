package io.iohk.atala.prism.app.dagger.modules;

import android.content.res.Resources;

import io.iohk.atala.prism.app.core.PrismApplication;
import io.iohk.atala.prism.app.data.local.db.AppDatabase;
import io.iohk.atala.prism.app.data.local.db.dao.ContactDao;
import io.iohk.atala.prism.app.data.local.db.dao.CredentialDao;
import io.iohk.atala.prism.app.data.local.db.dao.ProofRequestDao;
import io.iohk.atala.prism.app.neo.data.AccountRecoveryRepository;
import io.iohk.atala.prism.app.neo.data.ActivityHistoriesRepository;
import io.iohk.atala.prism.app.neo.data.ContactsRepository;
import io.iohk.atala.prism.app.neo.data.CredentialsRepository;
import io.iohk.atala.prism.app.neo.data.PreferencesRepository;
import io.iohk.atala.prism.app.neo.data.SyncRepository;
import io.iohk.atala.prism.app.neo.data.SessionRepository;
import io.iohk.atala.prism.app.neo.data.local.ActivityHistoriesLocalDataSource;
import io.iohk.atala.prism.app.neo.data.local.ActivityHistoriesLocalDataSourceInterface;
import io.iohk.atala.prism.app.neo.data.local.ContactsLocalDataSource;
import io.iohk.atala.prism.app.neo.data.local.ContactsLocalDataSourceInterface;
import io.iohk.atala.prism.app.neo.data.local.CredentialsLocalDataSource;
import io.iohk.atala.prism.app.neo.data.local.CredentialsLocalDataSourceInterface;
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSource;
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSourceInterface;
import io.iohk.atala.prism.app.neo.data.local.SyncLocalDataSource;
import io.iohk.atala.prism.app.neo.data.local.SyncLocalDataSourceInterface;
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSource;
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSourceInterface;
import io.iohk.atala.prism.app.neo.data.remote.ConnectorRemoteDataSource;
import dagger.Module;
import dagger.Provides;
import io.iohk.atala.prism.app.data.local.preferences.Preferences;

import javax.inject.Singleton;

@Module
public class ApplicationModule {

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

    @Provides
    ProofRequestDao provideProofRequestDao(AppDatabase appDatabase) {
        return appDatabase.proofRequestDao();
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

    /*
     * TODO this provider has to be deleted and replaced by PreferencesRepository this will be done
     *  when finishing refactoring some screens that currently do not have an appropriate viewmodel
     * */
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
    public ConnectorRemoteDataSource provideConnectorRemoteDataSource(PreferencesLocalDataSourceInterface preferencesLocalDataSource, SessionLocalDataSourceInterface sessionLocalDataSource) {
        return new ConnectorRemoteDataSource(preferencesLocalDataSource, sessionLocalDataSource);
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


    /*
     * [ProofRequestRepository] providers
     * */

    @Provides
    public SyncLocalDataSourceInterface provideProofRequestsLocalDataSource(ProofRequestDao proofRequestDao, ContactDao contactDao, CredentialDao credentialDao) {
        return new SyncLocalDataSource(proofRequestDao, contactDao,credentialDao);
    }

    @Provides
    public SyncRepository provideProofRequestRepository(SyncLocalDataSourceInterface localDataSource,
                                                        ConnectorRemoteDataSource connectorRemoteDataSource,
                                                        SessionLocalDataSourceInterface sessionLocalDataSource,
                                                        PreferencesLocalDataSourceInterface preferencesLocalDataSource) {
        return new SyncRepository(localDataSource, connectorRemoteDataSource, sessionLocalDataSource, preferencesLocalDataSource);
    }

    /*
     * [CredentialsRepository] providers
     * */

    @Provides
    public CredentialsLocalDataSourceInterface provideCredentialsLocalDataSource(CredentialDao credentialDao) {
        return new CredentialsLocalDataSource(credentialDao);
    }

    @Provides
    public CredentialsRepository provideCredentialsRepository(CredentialsLocalDataSourceInterface localDataSource,
                                                              ConnectorRemoteDataSource connectorRemoteDataSource,
                                                              SessionLocalDataSourceInterface sessionLocalDataSource,
                                                              PreferencesLocalDataSourceInterface preferencesLocalDataSource) {
        return new CredentialsRepository(localDataSource, connectorRemoteDataSource, sessionLocalDataSource, preferencesLocalDataSource);
    }

    /*
     * [ContactsRepository] providers
     * */

    @Provides
    public ContactsRepository provideContactsRepository(ContactsLocalDataSourceInterface localDataSource,
                                                        ConnectorRemoteDataSource connectorRemoteDataSource,
                                                        SessionLocalDataSourceInterface sessionLocalDataSource,
                                                        PreferencesLocalDataSourceInterface preferencesLocalDataSource) {
        return new ContactsRepository(localDataSource, connectorRemoteDataSource, sessionLocalDataSource, preferencesLocalDataSource);
    }

    /*
    * [ActivityHistoriesRepository] providers
    * */

    @Provides
    public ActivityHistoriesLocalDataSourceInterface provideActivityHistoriesLocalDataSource(ContactDao contactDao){
        return new ActivityHistoriesLocalDataSource(contactDao);
    }

    @Provides
    public ActivityHistoriesRepository provideActivityHistoriesRepository(ActivityHistoriesLocalDataSourceInterface localDataSource,
                                                                          SessionLocalDataSourceInterface sessionLocalDataSource,
                                                                          PreferencesLocalDataSourceInterface preferencesLocalDataSource){
        return new ActivityHistoriesRepository(localDataSource,sessionLocalDataSource,preferencesLocalDataSource);
    }


    /*
     * [PreferencesRepository] providers
     * */
    @Provides
    public PreferencesRepository providePreferencesRepository(ContactsLocalDataSourceInterface localDataSource,
                                                                    SessionLocalDataSourceInterface sessionLocalDataSource,
                                                                    PreferencesLocalDataSourceInterface preferencesLocalDataSource){
        return new PreferencesRepository(localDataSource,sessionLocalDataSource,preferencesLocalDataSource);
    }
}