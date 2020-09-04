package io.iohk.cvp.neo.di.modules;

import android.content.Context;

import dagger.Module;
import dagger.Provides;
import io.iohk.cvp.neo.data.SessionRepository;
import io.iohk.cvp.neo.data.local.SessionLocalDataSource;
import io.iohk.cvp.neo.data.local.SessionLocalDataSourceInterface;

@Module
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
}