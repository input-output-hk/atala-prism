package io.iohk.atala.prism.app.dagger.modules;

import dagger.Module;
import dagger.Provides;
import io.iohk.atala.prism.app.data.DataManager;
import io.iohk.atala.prism.app.viewmodel.AcceptConnectionDialogViewModelFactory;

@Module
public class AcceptConnectionDialogModule {
    @Provides
    AcceptConnectionDialogViewModelFactory provideViewModelProvider(DataManager dataManager) {
        return new AcceptConnectionDialogViewModelFactory(dataManager);
    }
}
