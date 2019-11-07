package io.iohk.cvp.dagger.modules;

import androidx.lifecycle.ViewModelProvider;
import dagger.Module;
import dagger.Provides;
import io.iohk.cvp.utils.ViewModelProviderFactory;
import io.iohk.cvp.viewmodel.PaymentViewModel;
import io.iohk.cvp.viewmodel.WalletSetupViewModel;

@Module
public class PaymentsModule {

  @Provides
  ViewModelProvider.Factory provideViewModelProvider(PaymentViewModel viewModel) {
    return new ViewModelProviderFactory<>(viewModel);
  }
}