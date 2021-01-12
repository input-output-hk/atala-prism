package io.iohk.atala.prism.app.dagger.modules;

import androidx.lifecycle.ViewModelProvider;

import dagger.Module;
import dagger.Provides;
import io.iohk.atala.prism.app.utils.ViewModelProviderFactory;
import io.iohk.atala.prism.app.viewmodel.CredentialsViewModel;

@Module
public class CredentialsFragmentModule {

    @Provides
    ViewModelProvider.Factory provideViewModelProvider(CredentialsViewModel viewModel) {
        return new ViewModelProviderFactory<>(viewModel);
    }
}