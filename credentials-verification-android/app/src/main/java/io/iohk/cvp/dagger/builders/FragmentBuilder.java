package io.iohk.cvp.dagger.builders;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import io.iohk.cvp.dagger.modules.ConnectionsListFragmentModule;
import io.iohk.cvp.dagger.modules.CredentialsFragmentModule;
import io.iohk.cvp.dagger.modules.HomeFragmentModule;
import io.iohk.cvp.dagger.modules.PaymentsModule;
import io.iohk.cvp.views.fragments.ConnectionsFragment;
import io.iohk.cvp.views.fragments.ConnectionsListFragment;
import io.iohk.cvp.views.fragments.CredentialDetailFragment;
import io.iohk.cvp.views.fragments.FirstConnectionFragment;
import io.iohk.cvp.views.fragments.HomeFragment;
import io.iohk.cvp.views.fragments.LargeDescriptionDialogFragment;
import io.iohk.cvp.views.fragments.PaymentFragment;
import io.iohk.cvp.views.fragments.PaymentHistoryFragment;
import io.iohk.cvp.views.fragments.ProfileFragment;
import io.iohk.cvp.views.fragments.SettingsFragment;
import io.iohk.cvp.views.fragments.WalletFragment;

@Module
public abstract class FragmentBuilder {

  @ContributesAndroidInjector
  abstract LargeDescriptionDialogFragment contributeLargeDescription();

  @ContributesAndroidInjector(modules = HomeFragmentModule.class)
  abstract HomeFragment contributeHomeFragment();

  @ContributesAndroidInjector
  abstract ProfileFragment contributeProfileFragment();

  @ContributesAndroidInjector
  abstract FirstConnectionFragment contributeConnectionFragment();

  @ContributesAndroidInjector(modules = ConnectionsListFragmentModule.class)
  abstract ConnectionsListFragment contributeConnectionsListFragment();

  @ContributesAndroidInjector(modules = ConnectionsListFragmentModule.class)
  abstract ConnectionsFragment contributeConnectionsFragment();

  @ContributesAndroidInjector(modules = CredentialsFragmentModule.class)
  abstract CredentialDetailFragment contributeCredentialFragment();

  @ContributesAndroidInjector
  abstract SettingsFragment contributeSettingsFragment();

  @ContributesAndroidInjector(modules = PaymentsModule.class)
  abstract WalletFragment contributeWalletFragment();

  @ContributesAndroidInjector
  abstract PaymentFragment contributePaymenFragment();

  @ContributesAndroidInjector(modules = PaymentsModule.class)
  abstract PaymentHistoryFragment contributePaymentHistoryFragment();
}
