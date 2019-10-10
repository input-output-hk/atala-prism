package io.iohk.cvp.dagger.components;


import dagger.Component;
import dagger.android.AndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;
import io.iohk.cvp.core.CvpApplication;
import io.iohk.cvp.dagger.builders.ActivityBuilder;
import io.iohk.cvp.dagger.modules.ApplicationModule;
import javax.inject.Singleton;

@Singleton
@Component(
    modules = {
        AndroidSupportInjectionModule.class,
        ApplicationModule.class,
        ActivityBuilder.class
    }
)
public interface CvpComponent extends AndroidInjector<CvpApplication> {

  @Component.Builder
  abstract class Builder extends AndroidInjector.Builder<CvpApplication> {

  }

}
