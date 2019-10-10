package io.iohk.cvp.dagger.builders;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import io.iohk.cvp.dagger.modules.ConnectionActivityModule;
import io.iohk.cvp.dagger.modules.ConnectionsListFragmentModule;
import io.iohk.cvp.views.activities.ConnectionActivity;
import io.iohk.cvp.views.activities.ConnectionsListActivity;
import io.iohk.cvp.views.activities.WelcomeActivity;

@Module
public abstract class ActivityBuilder {

  @ContributesAndroidInjector(modules = ConnectionActivityModule.class)
  abstract ConnectionActivity contributeConnectionActivity();

  @ContributesAndroidInjector
  abstract WelcomeActivity contributeWelcomeActivity();

  @ContributesAndroidInjector(modules = {ConnectionsListFragmentModule.class})
  abstract ConnectionsListActivity contributeConnectionsListActivity();

}
