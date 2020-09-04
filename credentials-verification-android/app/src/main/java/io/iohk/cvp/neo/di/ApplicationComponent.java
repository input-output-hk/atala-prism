package io.iohk.cvp.neo.di;

import android.app.Application;

import javax.inject.Singleton;

import dagger.Component;
import io.iohk.cvp.neo.data.SessionRepository;
import io.iohk.cvp.neo.di.modules.ApplicationModule;

@Singleton
@Component(modules = {ApplicationModule.class})
public interface ApplicationComponent {

    void inject(Application app);

    SessionRepository sessionRepository();
}