package io.iohk.cvp.dagger.modules;

import dagger.Module;
import dagger.Provides;
import io.iohk.cvp.data.DataManager;
import io.iohk.cvp.viewmodel.AcceptConnectionDialogViewModelFactory;
import io.iohk.cvp.viewmodel.ShareCredentialDialogViewModelFactory;

@Module
public class AcceptConnectionDialogModule {
    @Provides
    AcceptConnectionDialogViewModelFactory provideViewModelProvider(DataManager dataManager) {
        return new AcceptConnectionDialogViewModelFactory(dataManager);
    }
}
