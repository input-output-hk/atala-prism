package io.iohk.cvp.dagger.modules;

import dagger.Module;
import dagger.Provides;
import io.iohk.cvp.data.DataManager;
import io.iohk.cvp.viewmodel.ContactsViewModelFactory;

@Module
public class ContactsFragmentModule {
    @Provides
    ContactsViewModelFactory provideViewModelProvider(DataManager dataManager) {
        return new ContactsViewModelFactory(dataManager);
    }
}