package io.iohk.atala.prism.app.dagger.modules;

import dagger.Module;
import dagger.Provides;
import io.iohk.atala.prism.app.data.DataManager;
import io.iohk.atala.prism.app.viewmodel.ContactsViewModelFactory;

@Module
public class ContactsFragmentModule {
    @Provides
    ContactsViewModelFactory provideViewModelProvider(DataManager dataManager) {
        return new ContactsViewModelFactory(dataManager);
    }
}