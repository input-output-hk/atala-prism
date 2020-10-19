package io.iohk.atala.prism.app.neo.di.modules;

import android.content.Context;

import dagger.Module;
import dagger.Provides;
import io.iohk.atala.prism.app.data.local.db.dao.ContactDao;
import io.iohk.atala.prism.app.data.local.db.dao.CredentialDao;
import io.iohk.atala.prism.app.neo.data.AccountRecoveryRepository;
import io.iohk.atala.prism.app.neo.data.SessionRepository;
import io.iohk.atala.prism.app.neo.data.local.ContactsLocalDataSource;
import io.iohk.atala.prism.app.neo.data.local.ContactsLocalDataSourceInterface;
import io.iohk.atala.prism.app.neo.data.local.CredentialsLocalDataSource;
import io.iohk.atala.prism.app.neo.data.local.CredentialsLocalDataSourceInterface;
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSource;
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSourceInterface;
import io.iohk.atala.prism.app.neo.data.remote.ConnectorRemoteDataSource;

@Module(includes = RoomDataBaseModule.class)
public class ApplicationModule {

    private final Context context;

    public ApplicationModule(Context context) {
        this.context = context;
    }

    @Provides
    public Context context() {
        return context;
    }

    /*
     *  SessionRepository providers
     * */

    @Provides
    public SessionLocalDataSourceInterface provideSessionLocalDataSource() {
        return new SessionLocalDataSource(context);
    }

    @Provides
    public SessionRepository provideSessionRepository(SessionLocalDataSourceInterface sessionLocalDataSource) {
        return new SessionRepository(sessionLocalDataSource);
    }

    /*
     * API Providers
     * */

    @Provides
    public ConnectorRemoteDataSource provideConnectorRemoteDataSource(SessionLocalDataSourceInterface sessionLocalDataSource) {
        return new ConnectorRemoteDataSource(sessionLocalDataSource);
    }

    /*
     * AccountRecoveryRepository providers
     * */

    @Provides
    public CredentialsLocalDataSourceInterface provideCredentialsLocalDataSource(CredentialDao credentialDao) {
        return new CredentialsLocalDataSource(credentialDao);
    }

    @Provides
    public ContactsLocalDataSourceInterface provideContactsLocalDataSource(ContactDao contactDao) {
        return new ContactsLocalDataSource(contactDao);
    }

    @Provides
    public AccountRecoveryRepository provideSessionLocalDataSourceInterface(SessionLocalDataSourceInterface sessionLocalDataSource,
                                                                            CredentialsLocalDataSourceInterface credentialsLocalDataSource,
                                                                            ContactsLocalDataSourceInterface contactsLocalDataSource,
                                                                            ConnectorRemoteDataSource connectorRemoteDataSource) {
        return new AccountRecoveryRepository(sessionLocalDataSource, credentialsLocalDataSource, contactsLocalDataSource, connectorRemoteDataSource);
    }
}