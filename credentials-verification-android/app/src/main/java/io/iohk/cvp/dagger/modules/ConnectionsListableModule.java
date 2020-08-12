package io.iohk.cvp.dagger.modules;

import androidx.lifecycle.ViewModelProvider;
import dagger.Module;
import dagger.Provides;
import io.iohk.cvp.data.DataManager;
import io.iohk.cvp.utils.ViewModelProviderFactory;
import io.iohk.cvp.viewmodel.ConnectionsListablesViewModel;

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
