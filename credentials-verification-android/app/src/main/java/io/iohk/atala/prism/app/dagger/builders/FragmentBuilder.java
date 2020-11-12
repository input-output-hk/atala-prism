package io.iohk.atala.prism.app.dagger.builders;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import io.iohk.atala.prism.app.dagger.modules.AcceptConnectionDialogModule;
import io.iohk.atala.prism.app.dagger.modules.ConnectionsListableModule;
import io.iohk.atala.prism.app.dagger.modules.ContactDetailFragmentModule;
import io.iohk.atala.prism.app.dagger.modules.ContactsFragmentModule;
import io.iohk.atala.prism.app.dagger.modules.CredentialHistoryFragmentModule;
import io.iohk.atala.prism.app.dagger.modules.CredentialsFragmentModule;
import io.iohk.atala.prism.app.dagger.modules.NotificationsFragmentModule;
import io.iohk.atala.prism.app.dagger.modules.DeleteContactAlertDialogModule;
import io.iohk.atala.prism.app.dagger.modules.MyCredentialsFragmentModule;
import io.iohk.atala.prism.app.dagger.modules.PaymentsModule;
import io.iohk.atala.prism.app.dagger.modules.SettingsFragmentModule;
import io.iohk.atala.prism.app.dagger.modules.ShareCredentialDialogModule;
import io.iohk.atala.prism.app.views.activities.UnlockActivity;
import io.iohk.atala.prism.app.views.fragments.AboutFragment;
import io.iohk.atala.prism.app.views.fragments.AcceptConnectionDialogFragment;
import io.iohk.atala.prism.app.views.fragments.AddQrCodeDialogFragment;
import io.iohk.atala.prism.app.views.fragments.AlreadyConnectedDialogFragment;
import io.iohk.atala.prism.app.views.fragments.BackendIpFragment;
import io.iohk.atala.prism.app.views.fragments.ContactDetailFragment;
import io.iohk.atala.prism.app.views.fragments.ContactsFragment;
import io.iohk.atala.prism.app.views.fragments.CredentialDetailFragment;
import io.iohk.atala.prism.app.views.fragments.CredentialHistoryFragment;
import io.iohk.atala.prism.app.views.fragments.DeleteAllConnectionsDialogFragment;
import io.iohk.atala.prism.app.views.fragments.DeleteContactAlertDialogFragment;
import io.iohk.atala.prism.app.views.fragments.DeleteCredentialDialogFragment;
import io.iohk.atala.prism.app.views.fragments.LargeDescriptionDialogFragment;
import io.iohk.atala.prism.app.views.fragments.MyCredentialsFragment;
import io.iohk.atala.prism.app.views.fragments.NotificationsFragment;
import io.iohk.atala.prism.app.views.fragments.PaymentCongratsFragment;
import io.iohk.atala.prism.app.views.fragments.PaymentFragment;
import io.iohk.atala.prism.app.views.fragments.PaymentHistoryFragment;
import io.iohk.atala.prism.app.views.fragments.ProfileFragment;
import io.iohk.atala.prism.app.views.fragments.SecurityChangePinFragment;
import io.iohk.atala.prism.app.views.fragments.SecurityFragment;
import io.iohk.atala.prism.app.views.fragments.SecuritySettingsStep1Fragment;
import io.iohk.atala.prism.app.views.fragments.SecuritySettingsStep2Fragment;
import io.iohk.atala.prism.app.views.fragments.SettingsFragment;
import io.iohk.atala.prism.app.views.fragments.ShareCredentialDialogFragment;
import io.iohk.atala.prism.app.views.fragments.ShareProofRequestDialogFragment;
import io.iohk.atala.prism.app.views.fragments.WalletFragment;
import io.iohk.atala.prism.app.views.utils.ForegroundBackgroundListener;

@Module
public abstract class FragmentBuilder {

    @ContributesAndroidInjector
    abstract LargeDescriptionDialogFragment contributeLargeDescription();

    @ContributesAndroidInjector(modules = MyCredentialsFragmentModule.class)
    abstract MyCredentialsFragment contributeMyCredentialsFragment();

    @ContributesAndroidInjector
    abstract ProfileFragment contributeProfileFragment();

    @ContributesAndroidInjector(modules = NotificationsFragmentModule.class)
    abstract NotificationsFragment contributeNotificationsFragment();

    @ContributesAndroidInjector(modules = CredentialsFragmentModule.class)
    abstract CredentialDetailFragment contributeCredentialFragment();

    @ContributesAndroidInjector(modules = ShareCredentialDialogModule.class)
    abstract ShareCredentialDialogFragment contributeShareCredentialDialogFragment();

    @ContributesAndroidInjector(modules = ContactsFragmentModule.class)
    abstract ContactsFragment contributeContactsFragment();

    @ContributesAndroidInjector(modules = SettingsFragmentModule.class)
    abstract SettingsFragment contributeSettingsFragment();

    @ContributesAndroidInjector(modules = PaymentsModule.class)
    abstract WalletFragment contributeWalletFragment();

    @ContributesAndroidInjector
    abstract PaymentFragment contributePaymenFragment();

    @ContributesAndroidInjector
    abstract PaymentCongratsFragment contributePaymenCongratsFragment();

    @ContributesAndroidInjector(modules = PaymentsModule.class)
    abstract PaymentHistoryFragment contributePaymentHistoryFragment();

    @ContributesAndroidInjector(modules = AcceptConnectionDialogModule.class)
    abstract AcceptConnectionDialogFragment contributeAcceptConnectionDialogFragmentRefactored();

    @ContributesAndroidInjector(modules = ConnectionsListableModule.class)
    abstract ShareProofRequestDialogFragment contributeShareProofRequestDialogFragment();

    @ContributesAndroidInjector
    abstract BackendIpFragment contributeBackendIpFragment();

    @ContributesAndroidInjector
    abstract AboutFragment contributeAboutFragment();

    @ContributesAndroidInjector
    abstract SecurityFragment contributeSecurityFragment();

    @ContributesAndroidInjector
    abstract SecurityChangePinFragment contributeSecurityChangePinFragment();

    @ContributesAndroidInjector
    abstract SecuritySettingsStep1Fragment contributeSecuritySettingsStep1Fragment();

    @ContributesAndroidInjector
    abstract SecuritySettingsStep2Fragment contributeSecuritySettingsStep2Fragment();

    @ContributesAndroidInjector
    abstract UnlockActivity contributeUnlockFragment();

    @ContributesAndroidInjector
    abstract ForegroundBackgroundListener foregroundBackgroundListener();

    @ContributesAndroidInjector
    abstract DeleteAllConnectionsDialogFragment deleteAllConnectionsDialogFragment();

    @ContributesAndroidInjector
    abstract AlreadyConnectedDialogFragment alreadyConnectedDialogFragment();

    @ContributesAndroidInjector
    abstract AddQrCodeDialogFragment addQrCodeDialogFragment();

    @ContributesAndroidInjector(modules = CredentialsFragmentModule.class)
    abstract DeleteCredentialDialogFragment deleteCredentialDialogFragment();

    @ContributesAndroidInjector(modules = DeleteContactAlertDialogModule.class)
    abstract DeleteContactAlertDialogFragment deleteContactDialogFragment();

    @ContributesAndroidInjector(modules = ContactDetailFragmentModule.class)
    abstract ContactDetailFragment contributeContactDetailFragment();

    @ContributesAndroidInjector(modules = CredentialHistoryFragmentModule.class)
    abstract CredentialHistoryFragment contributeCredentialHistoryFragment();
}
