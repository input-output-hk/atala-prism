package io.iohk.atala.prism.app.dagger.modules;

import androidx.lifecycle.ViewModelProvider;

import dagger.Module;
import dagger.Provides;
import io.iohk.atala.prism.app.utils.ViewModelProviderFactory;
import io.iohk.atala.prism.app.viewmodel.MainViewModel;
import io.iohk.atala.prism.app.views.fragments.ContactsFragment;
import io.iohk.atala.prism.app.views.fragments.MyCredentialsFragment;
import io.iohk.atala.prism.app.views.fragments.NotificationsFragment;
import io.iohk.atala.prism.app.views.fragments.ProfileFragment;
import io.iohk.atala.prism.app.views.fragments.SettingsFragment;
import io.iohk.atala.prism.app.views.fragments.WalletFragment;

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
    WalletFragment provideWalletFragment() {
        return new WalletFragment();
    }

    @Provides
    ProfileFragment provideProfileFragment() {
        return new ProfileFragment();
    }

    @Provides
    ContactsFragment provideContactsFragment() {
        return new ContactsFragment();
    }

    @Provides
    ViewModelProvider.Factory provideViewModelProvider(MainViewModel viewModel) {
        return new ViewModelProviderFactory<>(viewModel);
    }
}