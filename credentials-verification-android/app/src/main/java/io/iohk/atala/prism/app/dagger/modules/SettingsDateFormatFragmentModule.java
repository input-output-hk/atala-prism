package io.iohk.atala.prism.app.dagger.modules;

import dagger.Module;
import dagger.Provides;
import io.iohk.atala.prism.app.data.DataManager;
import io.iohk.atala.prism.app.viewmodel.SettingsDateFormatViewModelFactory;

@Module
public class SettingsDateFormatFragmentModule {
    @Provides
    SettingsDateFormatViewModelFactory provideSettingsDateFormatViewModelFactory(DataManager dataManager) {
        return new SettingsDateFormatViewModelFactory(dataManager);
    }
}