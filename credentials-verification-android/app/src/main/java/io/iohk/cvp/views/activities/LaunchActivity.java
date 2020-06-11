package io.iohk.cvp.views.activities;

import android.os.Bundle;
import android.os.Handler;
import androidx.lifecycle.ViewModel;
import io.iohk.cvp.R;
import io.iohk.cvp.core.exception.SharedPrefencesDataNotFoundException;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.Preferences;
import java.util.ArrayList;
import java.util.Objects;
import javax.inject.Inject;

public class LaunchActivity extends CvpActivity {

  private static final long DELAY_IN_MILLISECONDS = 1500L;  // 1.5 seconds

  @Inject
  Navigator navigator;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    Preferences prefs = new Preferences(this);
    prefs.setIsFirstLaunch(true);
    Objects.requireNonNull(getSupportActionBar()).hide();

    new Handler().postDelayed(
        () -> {
          if(prefs.isPrivateKeyStored()) {
            navigator.showConnections(this, new ArrayList<>());
          } else {
            navigator.showWelcomeActivity(this);
          }
        },
        DELAY_IN_MILLISECONDS);
  }

  @Override
  protected Navigator getNavigator() {
    return navigator;
  }

  @Override
  protected int getView() {
    return R.layout.launch_activity;
  }

  @Override
  protected int getTitleValue() {
    return R.string.empty_title;
  }

  @Override
  public ViewModel getViewModel() {
    return null;
  }
}
