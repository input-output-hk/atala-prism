package io.iohk.atala.prism.app.dagger.modules;

import dagger.Module;
import dagger.Provides;
import io.iohk.atala.prism.app.data.DataManager;
import io.iohk.atala.prism.app.viewmodel.NotificationsViewModelFactory;

@Module
public class NotificationsFragmentModule {

    @Provides
    NotificationsViewModelFactory provideNotificationsViewModelFactory(DataManager dataManager) {
        return new NotificationsViewModelFactory(dataManager);
    }
}
