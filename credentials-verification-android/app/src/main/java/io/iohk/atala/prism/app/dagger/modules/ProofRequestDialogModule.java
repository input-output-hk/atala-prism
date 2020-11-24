package io.iohk.atala.prism.app.dagger.modules;

import dagger.Module;
import dagger.Provides;
import io.iohk.atala.prism.app.data.DataManager;
import io.iohk.atala.prism.app.viewmodel.ProofRequestDialogViewModelFactory;

@Module
public class ProofRequestDialogModule {
    @Provides
    ProofRequestDialogViewModelFactory provideViewModelFactory(DataManager dataManager) {
        return new ProofRequestDialogViewModelFactory(dataManager);
    }
}
