package io.iohk.cvp.dagger.modules;

import dagger.Module;
import dagger.Provides;
import io.iohk.cvp.data.DataManager;
import io.iohk.cvp.viewmodel.MyCredentialsViewModelFactory;

@Module
public class MyCredentialsFragmentModule {

    @Provides
    MyCredentialsViewModelFactory provideViewModelProvider(DataManager dataManager) {
        return new MyCredentialsViewModelFactory(dataManager);
    }
}