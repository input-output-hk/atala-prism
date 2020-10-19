package io.iohk.atala.prism.app.dagger.modules;

import androidx.lifecycle.ViewModelProvider;

import dagger.Module;
import dagger.Provides;
import io.iohk.atala.prism.app.utils.ViewModelProviderFactory;
import io.iohk.atala.prism.app.viewmodel.RestoreAccountViewModel;

@Module
public class RestoreAccountFragmentModule {

    @Provides
    ViewModelProvider.Factory provideViewModelProvider(RestoreAccountViewModel viewModel) {
        return new ViewModelProviderFactory<>(viewModel);
    }
}