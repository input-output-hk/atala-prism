package io.iohk.cvp.dagger.builders;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import io.iohk.cvp.dagger.modules.MainActivityModule;
import io.iohk.cvp.views.activities.AccountCreatedActivity;
import io.iohk.cvp.views.activities.MainActivity;
import io.iohk.cvp.views.activities.RestoreAccountSuccessActivity;

@Module
public abstract class ActivityBuilder {

  @ContributesAndroidInjector(modules = MainActivityModule.class)
  abstract MainActivity contributeMainActivity();

  @ContributesAndroidInjector
  abstract AccountCreatedActivity accountCreatedActivity();

  @ContributesAndroidInjector
  abstract RestoreAccountSuccessActivity restoreAccountSuccessActivity();

}
