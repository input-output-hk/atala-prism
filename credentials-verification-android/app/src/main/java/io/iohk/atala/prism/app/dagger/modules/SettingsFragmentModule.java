package io.iohk.atala.prism.app.dagger.modules;

import androidx.lifecycle.ViewModelProvider;

import dagger.Module;
import dagger.Provides;
import io.iohk.atala.prism.app.utils.ViewModelProviderFactory;
import io.iohk.atala.prism.app.viewmodel.ConnectionsActivityViewModel;

@Module
public class SettingsFragmentModule {

    @Provides
    ViewModelProvider.Factory provideViewModelProvider(ConnectionsActivityViewModel viewModel) {
        return new ViewModelProviderFactory<>(viewModel);
    }
}
