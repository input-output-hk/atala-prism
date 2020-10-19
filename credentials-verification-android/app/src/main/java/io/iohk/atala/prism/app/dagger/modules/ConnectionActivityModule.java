package io.iohk.atala.prism.app.dagger.modules;

import androidx.lifecycle.ViewModelProvider;

import io.iohk.atala.prism.app.data.DataManager;
import io.iohk.atala.prism.app.utils.ViewModelProviderFactory;
import io.iohk.atala.prism.app.viewmodel.ConnectionsActivityViewModel;
import dagger.Module;
import dagger.Provides;

@Module
public class ConnectionActivityModule {

    @Provides
    ConnectionsActivityViewModel provideConnectionsActivityViewModel(DataManager dataManager) {
        return new ConnectionsActivityViewModel(dataManager);
    }

    @Provides
    ViewModelProvider.Factory provideViewModelProvider(ConnectionsActivityViewModel viewModel) {
        return new ViewModelProviderFactory<>(viewModel);
    }
}
