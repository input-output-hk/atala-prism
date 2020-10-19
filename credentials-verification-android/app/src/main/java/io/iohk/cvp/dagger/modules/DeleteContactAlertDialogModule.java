package io.iohk.cvp.dagger.modules;

import dagger.Module;
import dagger.Provides;
import io.iohk.cvp.data.DataManager;
import io.iohk.cvp.viewmodel.DeleteContactAlertDialogViewModelFactory;

@Module
public class DeleteContactAlertDialogModule {
    @Provides
    DeleteContactAlertDialogViewModelFactory provideViewModelProvider(DataManager dataManager) {
        return new DeleteContactAlertDialogViewModelFactory(dataManager);
    }
}
