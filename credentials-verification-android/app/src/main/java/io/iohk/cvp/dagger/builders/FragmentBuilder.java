package io.iohk.cvp.dagger.builders;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import io.iohk.cvp.dagger.modules.ConnectionsListFragmentModule;
import io.iohk.cvp.views.fragments.ConnectionsFragment;
import io.iohk.cvp.views.fragments.FirstConnectionFragment;
import io.iohk.cvp.views.fragments.ConnectionsListFragment;
import io.iohk.cvp.views.fragments.HomeFragment;
import io.iohk.cvp.views.fragments.LargeDescriptionDialogFragment;

@Module
public abstract class FragmentBuilder {

  @ContributesAndroidInjector
  abstract LargeDescriptionDialogFragment contributeLargeDescription();

  @ContributesAndroidInjector
  abstract HomeFragment contributeHomeFragment();

  @ContributesAndroidInjector
  abstract FirstConnectionFragment contributeConnectionFragment();

  @ContributesAndroidInjector
  abstract ConnectionsListFragment contributeConnectionsListFragment();

  @ContributesAndroidInjector(modules = ConnectionsListFragmentModule.class)
  abstract ConnectionsFragment contributeConnectionsFragment();
}
