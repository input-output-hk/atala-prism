package io.iohk.atala.prism.app.dagger.modules;

import dagger.Module;
import dagger.Provides;
import io.iohk.atala.prism.app.data.DataManager;
import io.iohk.atala.prism.app.viewmodel.CredentialHistoryViewModelFactory;

@Module
public class CredentialHistoryFragmentModule {
    @Provides
    CredentialHistoryViewModelFactory provideViewModelProvider(DataManager dataManager) {
        return new CredentialHistoryViewModelFactory(dataManager);
    }
}