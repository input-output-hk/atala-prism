package io.iohk.cvp.dagger.modules;

import androidx.lifecycle.ViewModelProvider;

import dagger.Module;
import dagger.Provides;
import io.iohk.cvp.utils.ViewModelProviderFactory;
import io.iohk.cvp.viewmodel.ConnectionsActivityViewModel;
import io.iohk.cvp.viewmodel.WalletSetupViewModel;

@Module
public class WalletSetupModule {

  @Provides
  ViewModelProvider.Factory provideViewModelProvider(WalletSetupViewModel viewModel) {
    return new ViewModelProviderFactory<>(viewModel);
  }

}