package io.iohk.atala.prism.app.dagger.modules;

import dagger.Module;
import dagger.Provides;
import io.iohk.atala.prism.app.data.DataManager;
import io.iohk.atala.prism.app.viewmodel.ActivityLogViewModelFactory;

@Module
public class ActivityLogFragmentModule {

    @Provides
    ActivityLogViewModelFactory provideNotificationsViewModelFactory(DataManager dataManager) {
        return new ActivityLogViewModelFactory(dataManager);
    }
}
