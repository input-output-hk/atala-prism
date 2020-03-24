package io.iohk.cvp.core;

import dagger.android.AndroidInjector;
import dagger.android.support.DaggerApplication;
import io.iohk.cvp.dagger.components.DaggerCvpComponent;

public class CvpApplication extends DaggerApplication {

  private static CvpApplication sInstance;

  public static synchronized CvpApplication getInstance() {
    return sInstance;
  }

  @Override
  public final void onCreate() {
    super.onCreate();
  }

  @Override
  protected AndroidInjector<? extends DaggerApplication> applicationInjector() {
    return DaggerCvpComponent.builder().create(this);
  }
}
