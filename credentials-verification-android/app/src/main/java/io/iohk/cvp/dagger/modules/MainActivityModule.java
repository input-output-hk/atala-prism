package io.iohk.cvp.dagger.modules;

import androidx.lifecycle.ViewModelProvider;
import dagger.Module;
import dagger.Provides;
import io.iohk.cvp.utils.ViewModelProviderFactory;
import io.iohk.cvp.viewmodel.MainViewModel;
import io.iohk.cvp.views.fragments.HomeFragment;
import io.iohk.cvp.views.fragments.MyCredentialsFragment;
import io.iohk.cvp.views.fragments.ProfileFragment;
import io.iohk.cvp.views.fragments.SettingsFragment;
import io.iohk.cvp.views.fragments.WalletFragment;

@Module
public class MainActivityModule {

  @Provides
  HomeFragment provideConnectionsFragment() {
    return new HomeFragment();
  }

  @Provides
  MyCredentialsFragment provideMyCredentialsFragment() {
    return new MyCredentialsFragment();
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

  @Provides
  ViewModelProvider.Factory provideViewModelProvider(MainViewModel viewModel) {
    return new ViewModelProviderFactory<>(viewModel);
  }
}