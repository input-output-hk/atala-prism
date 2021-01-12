package io.iohk.atala.prism.app.dagger.builders;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import io.iohk.atala.prism.app.dagger.modules.MainActivityModule;
import io.iohk.atala.prism.app.views.activities.AccountCreatedActivity;
import io.iohk.atala.prism.app.views.activities.MainActivity;

@Module
public abstract class ActivityBuilder {

    @ContributesAndroidInjector(modules = MainActivityModule.class)
    abstract MainActivity contributeMainActivity();

    @ContributesAndroidInjector
    abstract AccountCreatedActivity accountCreatedActivity();
}
