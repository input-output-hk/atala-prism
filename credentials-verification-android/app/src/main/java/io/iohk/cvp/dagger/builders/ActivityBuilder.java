package io.iohk.cvp.dagger.builders;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import io.iohk.cvp.dagger.modules.MainActivityModule;
import io.iohk.cvp.dagger.modules.WalletSetupModule;
import io.iohk.cvp.views.activities.AccountCreatedActivity;
import io.iohk.cvp.views.activities.MainActivity;
import io.iohk.cvp.views.activities.RestoreAccountSuccessActivity;
import io.iohk.cvp.views.activities.SeedPhraseVerificationActivity;
import io.iohk.cvp.views.activities.TermsAndConditionsActivity;
import io.iohk.cvp.views.activities.WalletSetupActivity;

@Module
public abstract class ActivityBuilder {

  @ContributesAndroidInjector(modules = MainActivityModule.class)
  abstract MainActivity contributeMainActivity();

  @ContributesAndroidInjector
  abstract TermsAndConditionsActivity contributeTermsAndConditionsActivity();

  @ContributesAndroidInjector(modules = WalletSetupModule.class)
  abstract WalletSetupActivity contributeWalletSetupActivity();

  @ContributesAndroidInjector(modules = WalletSetupModule.class)
  abstract SeedPhraseVerificationActivity contributeSeedPhraseVerificationActivity();

  @ContributesAndroidInjector
  abstract AccountCreatedActivity accountCreatedActivity();

  @ContributesAndroidInjector
  abstract RestoreAccountSuccessActivity restoreAccountSuccessActivity();

}
