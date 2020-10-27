package io.iohk.atala.prism.app.dagger.modules;

import dagger.Module;
import dagger.Provides;
import io.iohk.atala.prism.app.data.DataManager;
import io.iohk.atala.prism.app.viewmodel.ContactDetailViewModelFactory;

@Module
public class ContactDetailFragmentModule {
    @Provides
    ContactDetailViewModelFactory provideFactory(DataManager dataManager) {
        return new ContactDetailViewModelFactory(dataManager);
    }
}
