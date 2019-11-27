package io.iohk.cvp.views.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.FrameLayout;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModel;
import butterknife.BindView;
import com.crashlytics.android.Crashlytics;
import io.iohk.cvp.R;
import io.iohk.cvp.core.exception.CaseNotFoundException;
import io.iohk.cvp.core.exception.ErrorCode;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.fragments.ConnectionsFragment;
import io.iohk.cvp.views.fragments.CvpFragment;
import io.iohk.cvp.views.fragments.FirstConnectionFragment;
import io.iohk.cvp.views.fragments.HomeFragment;
import io.iohk.cvp.views.fragments.ProfileFragment;
import io.iohk.cvp.views.fragments.SettingsFragment;
import io.iohk.cvp.views.fragments.WalletFragment;
import io.iohk.cvp.views.utils.components.bottomAppBar.BottomAppBar;
import io.iohk.cvp.views.utils.components.bottomAppBar.BottomAppBarListener;
import io.iohk.cvp.views.utils.components.bottomAppBar.BottomAppBarOption;
import java.util.Optional;
import javax.inject.Inject;
import lombok.Getter;

public class MainActivity extends CvpActivity implements BottomAppBarListener {

  @Inject
  ConnectionsFragment connectionsFragment;

  @Inject
  HomeFragment homeFragment;

  @Inject
  SettingsFragment settingsFragment;

  @Inject
  WalletFragment walletFragment;

  @Inject
  ProfileFragment profileFragment;

  @Inject
  @Getter
  Navigator navigator;

  @BindView(R.id.bottom_appbar)
  public BottomAppBar bottomAppBar;

  @BindView(R.id.fragment_layout)
  public FrameLayout frameLayout;

  MenuItem paymentHistoryMenuItem;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
        WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
    super.onCreate(savedInstanceState);

    bottomAppBar.setListener(this);

    // TODO: for now, in every start of the main screen, the FirstConnectionFragment is showed, because other way this screen is only showed ones.
    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

    Preferences prefs = new Preferences(getApplicationContext());
    if (prefs.getUserIds().size() > 0) {
      ft.replace(R.id.fragment_layout, connectionsFragment);
    } else {
      ft.replace(R.id.fragment_layout, new FirstConnectionFragment());
    }
    ft.commit();
  }

  protected int getView() {
    return R.layout.activity_main;
  }

  protected int getTitleValue() {
    return R.string.connections_activity_title;
  }

  @Override
  public ViewModel getViewModel() {
    return null;
  }

  @Override
  public void onNavigation(BottomAppBarOption option) {
    getFragmentToRender(option)
        .ifPresent(cvpFragment -> navigator.showFragment(getSupportFragmentManager(), cvpFragment));
  }

  private Optional<CvpFragment> getFragmentToRender(BottomAppBarOption option) {
    switch (option) {
      case CONNECTIONS:
        return Optional.of(connectionsFragment);
      case HOME:
        return Optional.of(homeFragment);
      case SETTINGS:
        return Optional.of(settingsFragment);
      case WALLET:
        return Optional.of(walletFragment);
      case PROFILE:
        return Optional.of(profileFragment);
      default:
        Crashlytics.logException(
            new CaseNotFoundException("Couldn't find fragment for option " + option,
                ErrorCode.STEP_NOT_FOUND));
        return Optional.empty();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (getSupportActionBar() != null) {
      getSupportActionBar().setTitle(this.getTitleValue());
    }
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    paymentHistoryMenuItem = menu.findItem(R.id.action_payment_history);
    return true;
  }
}