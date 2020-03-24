package io.iohk.cvp.dagger.modules;

import io.iohk.cvp.viewmodel.ConnectionsActivityViewModel;
import dagger.Module;
import dagger.Provides;

@Module
public class ConnectionActivityModule {

  @Provides
  ConnectionsActivityViewModel provideConnectionsActivityViewModel() {
    return new ConnectionsActivityViewModel();
  }
}