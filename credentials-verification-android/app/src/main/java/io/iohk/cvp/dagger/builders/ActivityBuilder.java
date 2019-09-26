package io.iohk.cvp.dagger.builders;

import io.iohk.cvp.dagger.modules.ConnectionActivityModule;
import io.iohk.cvp.views.activities.ConnectionActivity;
import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class ActivityBuilder {

  @ContributesAndroidInjector(modules = ConnectionActivityModule.class)
  abstract ConnectionActivity contributeConnectionActivity();

}
