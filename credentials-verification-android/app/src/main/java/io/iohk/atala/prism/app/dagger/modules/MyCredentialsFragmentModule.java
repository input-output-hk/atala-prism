package io.iohk.atala.prism.app.dagger.modules;

import dagger.Module;
import dagger.Provides;
import io.iohk.atala.prism.app.data.DataManager;
import io.iohk.atala.prism.app.viewmodel.MyCredentialsViewModelFactory;

@Module
public class MyCredentialsFragmentModule {

    @Provides
    MyCredentialsViewModelFactory provideViewModelProvider(DataManager dataManager) {
        return new MyCredentialsViewModelFactory(dataManager);
    }
}