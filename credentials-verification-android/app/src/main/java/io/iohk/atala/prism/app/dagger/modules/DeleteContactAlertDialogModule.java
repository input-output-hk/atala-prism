package io.iohk.atala.prism.app.dagger.modules;

import dagger.Module;
import dagger.Provides;
import io.iohk.atala.prism.app.data.DataManager;
import io.iohk.atala.prism.app.viewmodel.DeleteContactAlertDialogViewModelFactory;

@Module
public class DeleteContactAlertDialogModule {
    @Provides
    DeleteContactAlertDialogViewModelFactory provideViewModelProvider(DataManager dataManager) {
        return new DeleteContactAlertDialogViewModelFactory(dataManager);
    }
}
