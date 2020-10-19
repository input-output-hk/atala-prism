package io.iohk.atala.prism.app.dagger.modules;

import dagger.Module;
import dagger.Provides;
import io.iohk.atala.prism.app.data.DataManager;
import io.iohk.atala.prism.app.viewmodel.ShareCredentialDialogViewModelFactory;

@Module
public class ShareCredentialDialogModule {
    @Provides
    ShareCredentialDialogViewModelFactory provideViewModelProvider(DataManager dataManager) {
        return new ShareCredentialDialogViewModelFactory(dataManager);
    }
}