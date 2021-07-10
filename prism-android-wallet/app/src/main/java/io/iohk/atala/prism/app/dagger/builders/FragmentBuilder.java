package io.iohk.atala.prism.app.dagger.builders;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import io.iohk.atala.prism.app.neo.ui.onboarding.phraseverification.PhraseVerificationFragment;
import io.iohk.atala.prism.app.neo.ui.onboarding.restoreaccount.RestoreAccountFragment;
import io.iohk.atala.prism.app.neo.ui.onboarding.walletsetup.WalletSetupFragment;
import io.iohk.atala.prism.app.ui.UnlockActivity;
import io.iohk.atala.prism.app.ui.main.dashboard.DashboardFragment;
import io.iohk.atala.prism.app.ui.idverification.tutorial.IdVerificationTutorialFragment;
import io.iohk.atala.prism.app.ui.main.settings.AboutFragment;
import io.iohk.atala.prism.app.ui.commondialogs.AcceptConnectionDialogFragment;
import io.iohk.atala.prism.app.ui.main.dashboard.ActivityLogFragment;
import io.iohk.atala.prism.app.ui.commondialogs.AddQrCodeDialogFragment;
import io.iohk.atala.prism.app.ui.main.settings.BackendIpFragment;
import io.iohk.atala.prism.app.ui.main.contacts.ContactDetailFragment;
import io.iohk.atala.prism.app.ui.main.contacts.ContactsFragment;
import io.iohk.atala.prism.app.ui.main.credentials.CredentialDetailFragment;
import io.iohk.atala.prism.app.ui.main.credentials.CredentialHistoryFragment;
import io.iohk.atala.prism.app.ui.main.contacts.DeleteContactAlertDialogFragment;
import io.iohk.atala.prism.app.ui.main.credentials.DeleteCredentialDialogFragment;
import io.iohk.atala.prism.app.ui.main.credentials.MyCredentialsFragment;
import io.iohk.atala.prism.app.ui.main.dashboard.NotificationsFragment;
import io.iohk.atala.prism.app.ui.main.dashboard.ProfileFragment;
import io.iohk.atala.prism.app.ui.commondialogs.ProofRequestDialogFragment;
import io.iohk.atala.prism.app.ui.main.settings.SecurityChangePinFragment;
import io.iohk.atala.prism.app.ui.main.settings.SecurityFragment;
import io.iohk.atala.prism.app.ui.main.settings.SecuritySettingsStep1Fragment;
import io.iohk.atala.prism.app.ui.main.settings.SecuritySettingsStep2Fragment;
import io.iohk.atala.prism.app.ui.main.settings.SettingsDateFormatFragment;
import io.iohk.atala.prism.app.ui.main.settings.SettingsFragment;
import io.iohk.atala.prism.app.ui.main.credentials.ShareCredentialDialogFragment;
import io.iohk.atala.prism.app.ui.payid.addresslist.PayIdAddressListFragment;
import io.iohk.atala.prism.app.ui.payid.detail.PayIdDetailFragment;
import io.iohk.atala.prism.app.ui.payid.addaddressform.AddAddressFormDialogFragment;
import io.iohk.atala.prism.app.ui.payid.instructions.PayIdInstructionsFragment;
import io.iohk.atala.prism.app.ui.payid.step1.PayIdSelectIdentityCredentialFragment;
import io.iohk.atala.prism.app.ui.payid.step2.PayIdSetupFormFragment;
import io.iohk.atala.prism.app.ui.utils.ForegroundBackgroundListener;

@Module
public abstract class FragmentBuilder {

    @ContributesAndroidInjector
    abstract MyCredentialsFragment contributeMyCredentialsFragment();

    @ContributesAndroidInjector
    abstract ProfileFragment contributeProfileFragment();

    @ContributesAndroidInjector
    abstract NotificationsFragment contributeNotificationsFragment();

    @ContributesAndroidInjector
    abstract CredentialDetailFragment contributeCredentialFragment();

    @ContributesAndroidInjector
    abstract ShareCredentialDialogFragment contributeShareCredentialDialogFragment();

    @ContributesAndroidInjector
    abstract ContactsFragment contributeContactsFragment();

    @ContributesAndroidInjector
    abstract SettingsFragment contributeSettingsFragment();

    @ContributesAndroidInjector
    abstract AcceptConnectionDialogFragment contributeAcceptConnectionDialogFragmentRefactored();

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
    abstract AddQrCodeDialogFragment addQrCodeDialogFragment();

    @ContributesAndroidInjector
    abstract DeleteCredentialDialogFragment deleteCredentialDialogFragment();

    @ContributesAndroidInjector
    abstract DeleteContactAlertDialogFragment deleteContactDialogFragment();

    @ContributesAndroidInjector
    abstract ContactDetailFragment contributeContactDetailFragment();

    @ContributesAndroidInjector
    abstract CredentialHistoryFragment contributeCredentialHistoryFragment();

    @ContributesAndroidInjector
    abstract ActivityLogFragment contributeActivityLogFragment();

    @ContributesAndroidInjector
    abstract SettingsDateFormatFragment contributeSettingsDateFormatFragment();

    @ContributesAndroidInjector
    abstract ProofRequestDialogFragment contributeProofRequestDialogFragment();

    @ContributesAndroidInjector
    abstract WalletSetupFragment contributeWalletSetupFragment();

    @ContributesAndroidInjector
    abstract PhraseVerificationFragment contributePhraseVerificationFragment();

    @ContributesAndroidInjector
    abstract RestoreAccountFragment contributeRestoreAccountFragment();

    @ContributesAndroidInjector
    abstract DashboardFragment contributeDashboardFragment();

    @ContributesAndroidInjector
    abstract PayIdSelectIdentityCredentialFragment contributePayIdSelectIdentityCredentialFragment();

    @ContributesAndroidInjector
    abstract PayIdSetupFormFragment contributePayIdSetupFormFragment();

    @ContributesAndroidInjector
    abstract PayIdDetailFragment contributePayIdDetailFragment();

    @ContributesAndroidInjector
    abstract PayIdAddressListFragment contributePayIdAddressListFragment();

    @ContributesAndroidInjector
    abstract AddAddressFormDialogFragment contributeAddAddressFormDialogFragment();

    @ContributesAndroidInjector
    abstract PayIdInstructionsFragment contributePayIdInstructionsFragment();

    @ContributesAndroidInjector
    abstract IdVerificationTutorialFragment contributeIdVerificationTutorialFragment();
}