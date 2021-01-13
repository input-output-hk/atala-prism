package io.iohk.atala.prism.app.dagger.modules;

import dagger.Module;
import dagger.Provides;
import io.iohk.atala.prism.app.ui.main.contacts.ContactsFragment;
import io.iohk.atala.prism.app.ui.main.credentials.MyCredentialsFragment;
import io.iohk.atala.prism.app.ui.main.notifications.NotificationsFragment;
import io.iohk.atala.prism.app.ui.main.profile.ProfileFragment;
import io.iohk.atala.prism.app.ui.main.settings.SettingsFragment;

@Module
public class MainActivityModule {

    @Provides
    NotificationsFragment provideNotificationsFragment() {
        return new NotificationsFragment();
    }

    @Provides
    MyCredentialsFragment provideMyCredentialsFragment() {
        return new MyCredentialsFragment();
    }

    @Provides
    SettingsFragment provideSettingsFragment() {
        return new SettingsFragment();
    }

    @Provides
    ProfileFragment provideProfileFragment() {
        return new ProfileFragment();
    }

    @Provides
    ContactsFragment provideContactsFragment() {
        return new ContactsFragment();
    }
}