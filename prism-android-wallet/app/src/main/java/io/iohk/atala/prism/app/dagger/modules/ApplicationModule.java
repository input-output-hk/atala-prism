package io.iohk.atala.prism.app.dagger.modules;

import android.content.Context;
import android.content.res.Resources;

import io.iohk.atala.prism.app.core.PrismApplication;
import io.iohk.atala.prism.app.data.local.db.AppDatabase;
import io.iohk.atala.prism.app.data.local.db.dao.ContactDao;
import io.iohk.atala.prism.app.data.local.db.dao.CredentialDao;
import io.iohk.atala.prism.app.data.local.db.dao.PayIdDao;
import io.iohk.atala.prism.app.data.local.db.dao.KycRequestDao;
import io.iohk.atala.prism.app.data.local.db.dao.ProofRequestDao;
import io.iohk.atala.prism.app.neo.data.AccountRecoveryRepository;
import io.iohk.atala.prism.app.neo.data.ActivityHistoriesRepository;
import io.iohk.atala.prism.app.neo.data.ContactsRepository;
import io.iohk.atala.prism.app.neo.data.CredentialsRepository;
import io.iohk.atala.prism.app.neo.data.DashboardRepository;
import io.iohk.atala.prism.app.neo.data.MirrorMessageHandler;
import io.iohk.atala.prism.app.neo.data.PayIdRepository;
import io.iohk.atala.prism.app.neo.data.KycRepository;
import io.iohk.atala.prism.app.neo.data.PreferencesRepository;
import io.iohk.atala.prism.app.neo.data.ProofRequestRepository;
import io.iohk.atala.prism.app.neo.data.ConnectorListenerRepository;
import io.iohk.atala.prism.app.neo.data.SessionRepository;
import io.iohk.atala.prism.app.neo.data.local.ActivityHistoriesLocalDataSource;
import io.iohk.atala.prism.app.neo.data.local.ActivityHistoriesLocalDataSourceInterface;
import io.iohk.atala.prism.app.neo.data.local.ContactsLocalDataSource;
import io.iohk.atala.prism.app.neo.data.local.ContactsLocalDataSourceInterface;
import io.iohk.atala.prism.app.neo.data.local.CredentialsLocalDataSource;
import io.iohk.atala.prism.app.neo.data.local.CredentialsLocalDataSourceInterface;
import io.iohk.atala.prism.app.neo.data.local.PayIdLocalDataSource;
import io.iohk.atala.prism.app.neo.data.local.PayIdLocalDataSourceInterface;
import io.iohk.atala.prism.app.neo.data.local.KycLocalDataSource;
import io.iohk.atala.prism.app.neo.data.local.KycLocalDataSourceInterface;
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSource;
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSourceInterface;
import io.iohk.atala.prism.app.neo.data.local.ProofRequestLocalDataSource;
import io.iohk.atala.prism.app.neo.data.local.ProofRequestLocalDataSourceInterface;
import io.iohk.atala.prism.app.neo.data.local.ConnectorListenerLocalDataSource;
import io.iohk.atala.prism.app.neo.data.local.ConnectorListenerLocalDataSourceInterface;
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

    @Provides
    PayIdDao providePayIdDao(AppDatabase appDatabase) {
        return appDatabase.payIdDao();
    }

    @Provides
    KycRequestDao provideKycRequestDao(AppDatabase appDatabase) {
        return appDatabase.kycRequestDao();
    }
    
    @Provides
    Context provideContext(PrismApplication prismApplication) {
        return prismApplication.getApplicationContext();
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
    @Singleton
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
     * MirrorMessageHandler provider
     * */

    @Provides
    public MirrorMessageHandler provideMirrorMessageHandler(ConnectorRemoteDataSource remoteDataSource){
        return MirrorMessageHandler.Companion.getInstance(remoteDataSource);
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
                                                                            PayIdLocalDataSourceInterface payIdLocalDataSource,
                                                                            ContactsLocalDataSourceInterface contactsLocalDataSource,
                                                                            ConnectorRemoteDataSource connectorRemoteDataSource) {
        return new AccountRecoveryRepository(sessionLocalDataSource, preferencesLocalDataSource, payIdLocalDataSource, contactsLocalDataSource, connectorRemoteDataSource);
    }

    /*
     * [ProofRequestRepository] providers
     * */

    @Provides
    public ProofRequestLocalDataSourceInterface provideProofRequestLocalDataSource(ProofRequestDao proofRequestDao, CredentialDao credentialDao){
        return new ProofRequestLocalDataSource(proofRequestDao, credentialDao);
    }

    @Provides
    public ProofRequestRepository provideProofRequestRepository(
            ProofRequestLocalDataSourceInterface proofRequestLocalDataSource,
            ConnectorRemoteDataSource connectorRemoteDataSource,
            SessionLocalDataSourceInterface sessionLocalDataSource,
            PreferencesLocalDataSourceInterface preferencesLocalDataSource
    ){
        return new ProofRequestRepository(proofRequestLocalDataSource, connectorRemoteDataSource, sessionLocalDataSource, preferencesLocalDataSource);
    }

    /*
     * [ConnectorListenerRepository] providers
     * */

    @Provides
    public ConnectorListenerLocalDataSourceInterface provideConnectorListenerLocalDataSource(ProofRequestDao proofRequestDao, ContactDao contactDao, CredentialDao credentialDao,PayIdDao payIdDao, KycRequestDao kycRequestDao) {
        return new ConnectorListenerLocalDataSource(proofRequestDao, contactDao,credentialDao, payIdDao, kycRequestDao);
    }

    @Provides
    public ConnectorListenerRepository provideConnectorListenerRepository(ConnectorListenerLocalDataSourceInterface localDataSource,
                                                                          ConnectorRemoteDataSource connectorRemoteDataSource,
                                                                          MirrorMessageHandler mirrorMessageHandler,
                                                                          SessionLocalDataSourceInterface sessionLocalDataSource,
                                                                          PreferencesLocalDataSourceInterface preferencesLocalDataSource) {
        return new ConnectorListenerRepository(localDataSource, connectorRemoteDataSource, mirrorMessageHandler, sessionLocalDataSource, preferencesLocalDataSource);
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
    public ActivityHistoriesLocalDataSourceInterface provideActivityHistoriesLocalDataSource(ContactDao contactDao) {
        return new ActivityHistoriesLocalDataSource(contactDao);
    }

    @Provides
    public ActivityHistoriesRepository provideActivityHistoriesRepository(ActivityHistoriesLocalDataSourceInterface localDataSource,
                                                                          SessionLocalDataSourceInterface sessionLocalDataSource,
                                                                          PreferencesLocalDataSourceInterface preferencesLocalDataSource) {
        return new ActivityHistoriesRepository(localDataSource, sessionLocalDataSource, preferencesLocalDataSource);
    }


    /*
     * [PreferencesRepository] providers
     * */
    @Provides
    public PreferencesRepository providePreferencesRepository(ContactsLocalDataSourceInterface localDataSource,
                                                                    PayIdLocalDataSourceInterface payIdLocalDataSource,
                                                                    SessionLocalDataSourceInterface sessionLocalDataSource,
                                                                    PreferencesLocalDataSourceInterface preferencesLocalDataSource){
        return new PreferencesRepository(payIdLocalDataSource, localDataSource,sessionLocalDataSource,preferencesLocalDataSource);
    }

    /*
     * [KycRepository] providers
     * */

    @Provides
    public KycLocalDataSourceInterface provideKycLocalDataSource(KycRequestDao kycRequestDao,
                                                                 ContactDao contactDao,
                                                                 PrismApplication prismApplication) {
        return new KycLocalDataSource(kycRequestDao, contactDao, prismApplication.getApplicationContext());
    }

    @Singleton
    @Provides
    public KycRepository provideIdentityVerificationRepository(KycLocalDataSourceInterface kycLocalDataSource,
                                                               ConnectorRemoteDataSource remoteDataSource,
                                                               Context context,
                                                               SessionLocalDataSourceInterface sessionLocalDataSource,
                                                               PreferencesLocalDataSourceInterface preferencesLocalDataSource) {
        return new KycRepository(kycLocalDataSource, remoteDataSource, context, sessionLocalDataSource, preferencesLocalDataSource);
    }

    /*
     * [DashboardRepository] providers
     * */

    @Provides
    public DashboardRepository provideDashboardRepository(
            PayIdLocalDataSourceInterface payIdLocalDataSource,
            ActivityHistoriesLocalDataSourceInterface activityHistoriesLocalDataSource,
            SessionLocalDataSourceInterface sessionLocalDataSource,
            PreferencesLocalDataSourceInterface preferencesLocalDataSource
    ) {
        return new DashboardRepository(payIdLocalDataSource, activityHistoriesLocalDataSource, sessionLocalDataSource, preferencesLocalDataSource);
    }

    /*
     * [PayIdRepository] providers
     * */

    @Provides
    public PayIdLocalDataSourceInterface providePayIdLocalDataSource(
            CredentialDao credentialDao,
            ContactDao contactDao,
            PayIdDao payIdDao,
            PrismApplication prismApplication
    ){
        return new PayIdLocalDataSource(credentialDao, contactDao, payIdDao, prismApplication.getApplicationContext());
    }
    @Provides
    public PayIdRepository providePayIdRepository(
            MirrorMessageHandler mirrorMessageHandler,
            PayIdLocalDataSourceInterface payIdLocalDataSource,
            ConnectorRemoteDataSource remoteDataSource,
            SessionLocalDataSourceInterface sessionLocalDataSource,
            PreferencesLocalDataSourceInterface preferencesLocalDataSource
    ){
        return new PayIdRepository(payIdLocalDataSource, mirrorMessageHandler, remoteDataSource, sessionLocalDataSource, preferencesLocalDataSource);
    }
}