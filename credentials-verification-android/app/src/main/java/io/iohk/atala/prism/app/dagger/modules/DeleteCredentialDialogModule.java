package io.iohk.atala.prism.app.dagger.modules;

import dagger.Module;
import dagger.Provides;
import io.iohk.atala.prism.app.data.DataManager;
import io.iohk.atala.prism.app.viewmodel.DeleteCredentialDialogViewModelFactory;

@Module
public class DeleteCredentialDialogModule {
    @Provides
    DeleteCredentialDialogViewModelFactory provideDeleteCredentialDialogViewModelFactory(DataManager dataManager) {
        return new DeleteCredentialDialogViewModelFactory(dataManager);
    }
}