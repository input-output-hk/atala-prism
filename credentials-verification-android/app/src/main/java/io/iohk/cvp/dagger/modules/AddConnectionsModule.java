package io.iohk.cvp.dagger.modules;

import androidx.lifecycle.ViewModelProvider;
import dagger.Module;
import dagger.Provides;
import io.iohk.cvp.utils.ViewModelProviderFactory;
import io.iohk.cvp.viewmodel.AcceptConnectionViewModel;

@Module
public class AddConnectionsModule {

  @Provides
  ViewModelProvider.Factory provideViewModelProvider(AcceptConnectionViewModel viewModel) {
    return new ViewModelProviderFactory<>(viewModel);
  }

}