package io.iohk.cvp.dagger.modules;

import androidx.lifecycle.ViewModelProvider;

import java.util.Date;

import dagger.Module;
import dagger.Provides;
import io.iohk.cvp.data.DataManager;
import io.iohk.cvp.utils.ViewModelProviderFactory;
import io.iohk.cvp.viewmodel.ShareCredentialDialogViewModel;
import io.iohk.cvp.viewmodel.ShareCredentialDialogViewModelFactory;

@Module
public class ShareCredentialDialogModule {
    @Provides
    ShareCredentialDialogViewModelFactory provideViewModelProvider(DataManager dataManager) {
        return new ShareCredentialDialogViewModelFactory(dataManager);
    }
}