package io.iohk.atala.prism.app.core;

import androidx.lifecycle.ProcessLifecycleOwner;

import dagger.android.AndroidInjector;
import dagger.android.support.DaggerApplication;
import io.iohk.atala.prism.app.neo.di.modules.ApplicationModule;
import io.iohk.atala.prism.app.neo.di.modules.RoomDataBaseModule;
import io.iohk.atala.prism.app.dagger.components.DaggerCvpComponent;
import io.iohk.atala.prism.app.neo.di.ApplicationComponent;
import io.iohk.atala.prism.app.neo.di.DaggerApplicationComponent;
import io.iohk.atala.prism.app.views.utils.ForegroundBackgroundListener;

public class PrismApplication extends DaggerApplication {

    private static PrismApplication sInstance;

    public static synchronized PrismApplication getInstance() {
        return sInstance;
    }

    @Override
    public final void onCreate() {
        super.onCreate();
        sInstance = this;
        // TODO this observer is is used to handle the security pin view, but I think we have to find a way to handle this better
        ProcessLifecycleOwner.get()
                .getLifecycle()
                .addObserver(
                        new ForegroundBackgroundListener(getApplicationContext()));
        buildApplicationComponent();
    }

    @Override
    protected AndroidInjector<? extends DaggerApplication> applicationInjector() {
        return DaggerCvpComponent.builder().create(this);
    }

    /*
     *  Handle new Application components for a Code Refactor
     * */
    public static ApplicationComponent applicationComponent;

    private void buildApplicationComponent() {
        applicationComponent = DaggerApplicationComponent
                .builder()
                .applicationModule(new ApplicationModule(this))
                .roomDataBaseModule(new RoomDataBaseModule(this))
                .build();
        applicationComponent.inject(this);
    }
}