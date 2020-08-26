package io.iohk.cvp.dagger.modules;

import androidx.lifecycle.ViewModelProvider;

import dagger.Module;
import dagger.Provides;
import io.iohk.cvp.utils.ViewModelProviderFactory;
import io.iohk.cvp.viewmodel.CredentialsViewModel;
import io.iohk.cvp.viewmodel.RestoreAccountViewModel;

@Module
public class RestoreAccountFragmentModule {

  @Provides
  ViewModelProvider.Factory provideViewModelProvider(RestoreAccountViewModel viewModel) {
    return new ViewModelProviderFactory<>(viewModel);
  }
}