package io.iohk.cvp.dagger.modules;

import io.iohk.cvp.core.CvpApplication;
import io.iohk.cvp.views.Navigator;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

@Module
public class ApplicationModule {

  @Provides
  @Singleton
  public CvpApplication provideMobilityApplication(CvpApplication application) {
    return application;
  }

  @Provides
  @Singleton
  public Navigator provideNavigator() {
    return new Navigator();
  }
}
