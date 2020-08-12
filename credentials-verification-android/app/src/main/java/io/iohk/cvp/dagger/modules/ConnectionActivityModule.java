package io.iohk.cvp.dagger.modules;

import androidx.lifecycle.ViewModelProvider;

import io.iohk.cvp.data.DataManager;
import io.iohk.cvp.utils.ViewModelProviderFactory;
import io.iohk.cvp.viewmodel.ConnectionsActivityViewModel;
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
