package io.iohk.atala.prism.app.dagger.builders;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import io.iohk.atala.prism.app.neo.ui.launch.LaunchActivity;
import io.iohk.atala.prism.app.ui.main.MainActivity;

@Module
public abstract class ActivityBuilder {

    @ContributesAndroidInjector
    abstract MainActivity contributeMainActivity();

    @ContributesAndroidInjector
    abstract LaunchActivity launchActivity();
}