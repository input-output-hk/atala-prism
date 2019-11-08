package io.iohk.cvp.dagger.modules;

import dagger.Module;
import dagger.Provides;
import io.iohk.cvp.views.fragments.ConnectionsFragment;
import io.iohk.cvp.views.fragments.HomeFragment;
import io.iohk.cvp.views.fragments.ProfileFragment;
import io.iohk.cvp.views.fragments.SettingsFragment;
import io.iohk.cvp.views.fragments.WalletFragment;

@Module
public class MainActivityModule {

  @Provides
  ConnectionsFragment provideConnectionsFragment() {
    return new ConnectionsFragment();
  }

  @Provides
  HomeFragment provideHomeFragment() {
    return new HomeFragment();
  }

  @Provides
  SettingsFragment provideSettingsFragment() {
    return new SettingsFragment();
  }

  @Provides
  WalletFragment provideWalletFragment() {
    return new WalletFragment();
  }

  @Provides
  ProfileFragment provideProfileFragment() {
    return new ProfileFragment();
  }
}