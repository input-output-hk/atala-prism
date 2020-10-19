package io.iohk.atala.prism.app.dagger.modules;

import androidx.lifecycle.ViewModelProvider;

import dagger.Module;
import dagger.Provides;
import io.iohk.atala.prism.app.data.DataManager;
import io.iohk.atala.prism.app.utils.ViewModelProviderFactory;
import io.iohk.atala.prism.app.viewmodel.ConnectionsListablesViewModel;

@Module
public class ConnectionsListableModule {

    @Provides
    ViewModelProvider.Factory provideViewModelProvider(ConnectionsListablesViewModel viewModel) {
        return new ViewModelProviderFactory<>(viewModel);
    }

    @Provides
    ConnectionsListablesViewModel provideConnectionsListablesViewModel(DataManager dataManager) {
        return new ConnectionsListablesViewModel(dataManager);
    }

}
