package io.iohk.atala.prism.app.dagger.components;

import javax.inject.Singleton;

import dagger.Component;
import dagger.android.AndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;
import io.iohk.atala.prism.app.core.PrismApplication;
import io.iohk.atala.prism.app.dagger.builders.ActivityBuilder;
import io.iohk.atala.prism.app.dagger.builders.FragmentBuilder;
import io.iohk.atala.prism.app.dagger.builders.ServiceBuilder;
import io.iohk.atala.prism.app.dagger.modules.ApplicationModule;
import io.iohk.atala.prism.app.dagger.modules.ViewModelModule;

@Singleton
@Component(
        modules = {
                AndroidSupportInjectionModule.class,
                ApplicationModule.class,
                ViewModelModule.class,
                ActivityBuilder.class,
                FragmentBuilder.class,
                ServiceBuilder.class
        }
)
public interface CvpComponent extends AndroidInjector<PrismApplication> {

    @Component.Builder
    abstract class Builder extends AndroidInjector.Builder<PrismApplication> {

    }
}