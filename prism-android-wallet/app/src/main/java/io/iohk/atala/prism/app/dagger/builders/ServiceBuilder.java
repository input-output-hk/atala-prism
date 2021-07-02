package io.iohk.atala.prism.app.dagger.builders;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import io.iohk.atala.prism.app.core.ConnectorListenerService;

@Module
public abstract class ServiceBuilder {
    @ContributesAndroidInjector
    abstract ConnectorListenerService contributeConnectorListenerService();
}
