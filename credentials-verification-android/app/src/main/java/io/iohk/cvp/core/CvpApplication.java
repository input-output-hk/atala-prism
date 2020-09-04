package io.iohk.cvp.core;

import androidx.lifecycle.ProcessLifecycleOwner;
import dagger.android.AndroidInjector;
import dagger.android.support.DaggerApplication;
import io.iohk.cvp.dagger.components.DaggerCvpComponent;
import io.iohk.cvp.neo.di.ApplicationComponent;
import io.iohk.cvp.neo.di.DaggerApplicationComponent;
import io.iohk.cvp.neo.di.modules.ApplicationModule;
import io.iohk.cvp.views.utils.ForegroundBackgroundListener;

public class CvpApplication extends DaggerApplication {

    private static CvpApplication sInstance;

    public static synchronized CvpApplication getInstance() {
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
                .build();
        applicationComponent.inject(this);
    }
}