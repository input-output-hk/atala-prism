package io.iohk.cvp.dagger.builders;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import io.iohk.cvp.dagger.modules.AddConnectionsModule;
import io.iohk.cvp.dagger.modules.ConnectionsListFragmentModule;
import io.iohk.cvp.dagger.modules.ConnectionsListableModule;
import io.iohk.cvp.dagger.modules.ContactsFragmentModule;
import io.iohk.cvp.dagger.modules.CredentialsFragmentModule;
import io.iohk.cvp.dagger.modules.HomeFragmentModule;
import io.iohk.cvp.dagger.modules.PaymentsModule;
import io.iohk.cvp.views.activities.UnlockActivity;
import io.iohk.cvp.views.fragments.AboutFragment;
import io.iohk.cvp.views.fragments.AcceptConnectionDialogFragment;
import io.iohk.cvp.views.fragments.BackendIpFragment;
import io.iohk.cvp.views.fragments.ConnectionsListFragment;
import io.iohk.cvp.views.fragments.ContactsFragment;
import io.iohk.cvp.views.fragments.CredentialDetailFragment;
import io.iohk.cvp.views.fragments.FirstConnectionFragment;
import io.iohk.cvp.views.fragments.HomeFragment;
import io.iohk.cvp.views.fragments.LargeDescriptionDialogFragment;
import io.iohk.cvp.views.fragments.MyCredentials;
import io.iohk.cvp.views.fragments.PaymentCongratsFragment;
import io.iohk.cvp.views.fragments.PaymentFragment;
import io.iohk.cvp.views.fragments.PaymentHistoryFragment;
import io.iohk.cvp.views.fragments.ProfileFragment;
import io.iohk.cvp.views.fragments.SecurityChangePinFragment;
import io.iohk.cvp.views.fragments.SecurityFragment;
import io.iohk.cvp.views.fragments.SecuritySettingsStep1Fragment;
import io.iohk.cvp.views.fragments.SecuritySettingsStep2Fragment;
import io.iohk.cvp.views.fragments.SettingsFragment;
import io.iohk.cvp.views.fragments.ShareCredentialDialogFragment;
import io.iohk.cvp.views.fragments.ShareProofRequestDialogFragment;
import io.iohk.cvp.views.fragments.WalletFragment;
import io.iohk.cvp.views.utils.ForegroundBackgroundListener;

@Module
public abstract class FragmentBuilder {

  @ContributesAndroidInjector
  abstract LargeDescriptionDialogFragment contributeLargeDescription();

  @ContributesAndroidInjector(modules = HomeFragmentModule.class)
  abstract MyCredentials contributeHomeFragment();

  @ContributesAndroidInjector(modules = ContactsFragmentModule.class)
  abstract ContactsFragment contributeContactsFragment();

  @ContributesAndroidInjector
  abstract ProfileFragment contributeProfileFragment();

  @ContributesAndroidInjector
  abstract FirstConnectionFragment contributeConnectionFragment();

  @ContributesAndroidInjector(modules = ConnectionsListFragmentModule.class)
  abstract ConnectionsListFragment contributeConnectionsListFragment();

  @ContributesAndroidInjector(modules = ConnectionsListFragmentModule.class)
  abstract HomeFragment contributeConnectionsFragment();

  @ContributesAndroidInjector(modules = CredentialsFragmentModule.class)
  abstract CredentialDetailFragment contributeCredentialFragment();

  @ContributesAndroidInjector(modules = ConnectionsListableModule.class)
  abstract ShareCredentialDialogFragment contributeShareCredentialDialogFragment();

  @ContributesAndroidInjector
  abstract SettingsFragment contributeSettingsFragment();

  @ContributesAndroidInjector(modules = PaymentsModule.class)
  abstract WalletFragment contributeWalletFragment();

  @ContributesAndroidInjector
  abstract PaymentFragment contributePaymenFragment();

  @ContributesAndroidInjector
  abstract PaymentCongratsFragment contributePaymenCongratsFragment();

  @ContributesAndroidInjector(modules = PaymentsModule.class)
  abstract PaymentHistoryFragment contributePaymentHistoryFragment();

  @ContributesAndroidInjector(modules = AddConnectionsModule.class)
  abstract AcceptConnectionDialogFragment contributeAcceptConnectionDialogFragment();

  @ContributesAndroidInjector(modules = AddConnectionsModule.class)
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
}
