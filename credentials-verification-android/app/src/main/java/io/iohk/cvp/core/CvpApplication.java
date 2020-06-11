package io.iohk.cvp.core;


import androidx.lifecycle.ProcessLifecycleOwner;

import dagger.android.AndroidInjector;
import dagger.android.support.DaggerApplication;
import io.iohk.cvp.dagger.components.DaggerCvpComponent;
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
    ProcessLifecycleOwner.get()
            .getLifecycle()
            .addObserver(
                    new ForegroundBackgroundListener(getApplicationContext()));

  }

  @Override
  protected AndroidInjector<? extends DaggerApplication> applicationInjector() {
    return DaggerCvpComponent.builder().create(this);
  }
}
