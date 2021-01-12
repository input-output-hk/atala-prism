package io.iohk.atala.prism.app.dagger.modules;

import dagger.Module;
import dagger.Provides;
import io.iohk.atala.prism.app.viewmodel.ProfileViewModelFactory;
import io.iohk.atala.prism.app.data.local.preferences.Preferences;

@Module
public class ProfileFragmentModule {
    @Provides
    ProfileViewModelFactory profileViewModelFactory(Preferences preferences) {
        return new ProfileViewModelFactory(preferences);
    }
}
