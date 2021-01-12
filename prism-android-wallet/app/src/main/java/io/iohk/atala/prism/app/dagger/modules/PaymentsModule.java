package io.iohk.atala.prism.app.dagger.modules;

import androidx.lifecycle.ViewModelProvider;

import dagger.Module;
import dagger.Provides;
import io.iohk.atala.prism.app.utils.ViewModelProviderFactory;
import io.iohk.atala.prism.app.viewmodel.PaymentViewModel;

@Module
public class PaymentsModule {

    @Provides
    ViewModelProvider.Factory provideViewModelProvider(PaymentViewModel viewModel) {
        return new ViewModelProviderFactory<>(viewModel);
    }
}