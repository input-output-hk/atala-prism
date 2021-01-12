package io.iohk.atala.prism.app.dagger.modules;

import androidx.lifecycle.ViewModelProvider;

import dagger.Module;
import dagger.Provides;
import io.iohk.atala.prism.app.utils.ViewModelProviderFactory;
import io.iohk.atala.prism.app.viewmodel.WalletSetupViewModel;

@Module
public class WalletSetupModule {

    @Provides
    ViewModelProvider.Factory provideViewModelProvider(WalletSetupViewModel viewModel) {
        return new ViewModelProviderFactory<>(viewModel);
    }
}