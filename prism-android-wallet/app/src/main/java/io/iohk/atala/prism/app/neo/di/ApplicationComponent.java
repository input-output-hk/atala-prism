package io.iohk.atala.prism.app.neo.di;

import android.app.Application;

import javax.inject.Singleton;

import dagger.Component;
import io.iohk.atala.prism.app.neo.data.AccountRecoveryRepository;
import io.iohk.atala.prism.app.neo.data.SessionRepository;
import io.iohk.atala.prism.app.neo.di.modules.ApplicationModule;

@Singleton
@Component(modules = {ApplicationModule.class})
public interface ApplicationComponent {

    void inject(Application app);

    SessionRepository sessionRepository();

    AccountRecoveryRepository accountRecoveryRepository();
}