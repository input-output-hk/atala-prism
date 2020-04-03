package io.iohk.cvp.dagger.components;


import javax.inject.Singleton;

import dagger.Component;
import dagger.android.AndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;
import io.iohk.cvp.core.CvpApplication;
import io.iohk.cvp.dagger.builders.ActivityBuilder;
import io.iohk.cvp.dagger.builders.FragmentBuilder;
import io.iohk.cvp.dagger.modules.ApplicationModule;

@Singleton
@Component(
        modules = {
                AndroidSupportInjectionModule.class,
                ApplicationModule.class,
                ActivityBuilder.class,
                FragmentBuilder.class
        }
)
public interface CvpComponent extends AndroidInjector<CvpApplication> {

    @Component.Builder
    abstract class Builder extends AndroidInjector.Builder<CvpApplication> {

    }
}
