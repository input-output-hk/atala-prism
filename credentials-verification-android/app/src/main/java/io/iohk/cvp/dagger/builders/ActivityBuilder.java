package io.iohk.cvp.dagger.builders;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import io.iohk.cvp.views.activities.MainActivity;
import io.iohk.cvp.views.activities.TermsAndConditionsActivity;
import io.iohk.cvp.views.activities.WelcomeActivity;

@Module
public abstract class ActivityBuilder {
  @ContributesAndroidInjector
  abstract WelcomeActivity contributeWelcomeActivity();

  @ContributesAndroidInjector
  abstract MainActivity contributeMainActivity();

  @ContributesAndroidInjector
  abstract TermsAndConditionsActivity contributeTermsAndConditionsActivity();
}
