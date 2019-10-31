package io.iohk.cvp.dagger.modules;

import androidx.lifecycle.ViewModelProvider;

import dagger.Module;
import dagger.Provides;
import io.iohk.cvp.utils.ViewModelProviderFactory;
import io.iohk.cvp.viewmodel.ConnectionsActivityViewModel;
import io.iohk.cvp.viewmodel.CredentialsViewModel;

@Module
public class HomeFragmentModule {

  @Provides
  ViewModelProvider.Factory provideViewModelProvider(CredentialsViewModel viewModel) {
    return new ViewModelProviderFactory<>(viewModel);
  }

}