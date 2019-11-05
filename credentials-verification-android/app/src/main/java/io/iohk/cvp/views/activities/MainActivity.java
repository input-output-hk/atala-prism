package io.iohk.cvp.views.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.FrameLayout;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModel;
import butterknife.BindView;
import io.iohk.cvp.R;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.fragments.ConnectionsFragment;
import io.iohk.cvp.views.fragments.CvpFragment;
import io.iohk.cvp.views.fragments.FirstConnectionFragment;
import io.iohk.cvp.views.fragments.HomeFragment;
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
    ft.replace(R.id.fragment_layout, new FirstConnectionFragment());
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
        return Optional.of(new ConnectionsFragment());
      case HOME:
        return Optional.of(new HomeFragment());
      case SETTINGS:
        return Optional.of(new SettingsFragment());
      case WALLET:
        return Optional.of(new WalletFragment());
      default:
        // TODO: for now, every intention to go to an unimplemented screen result in no action.
        // TODO: when the rest of the screen are implemented, the default case should throw an Exception
        // Crashlytics.logException(
        //   new CaseNotFoundException("Couldn't find fragment for option " + option,
        //     ErrorCode.STEP_NOT_FOUND));
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