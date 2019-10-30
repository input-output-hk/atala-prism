package io.iohk.cvp.views.activities;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.fragment.app.FragmentTransaction;

import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.support.DaggerAppCompatActivity;
import io.iohk.cvp.R;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.fragments.ConnectionsFragment;
import io.iohk.cvp.views.fragments.CvpFragment;
import io.iohk.cvp.views.fragments.FirstConnectionFragment;
import io.iohk.cvp.views.fragments.HomeFragment;
import io.iohk.cvp.views.utils.components.bottomAppBar.BottomAppBar;
import io.iohk.cvp.views.utils.components.bottomAppBar.BottomAppBarListener;
import io.iohk.cvp.views.utils.components.bottomAppBar.BottomAppBarOption;
import lombok.Getter;

public class MainActivity extends DaggerAppCompatActivity implements BottomAppBarListener {

  @Inject
  @Getter
  Navigator navigator;

  @BindView(R.id.bottom_appbar)
  public BottomAppBar bottomAppBar;

  @BindView(R.id.fragment_layout)
  public FrameLayout frameLayout;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Objects.requireNonNull(getSupportActionBar()).hide();

    if (findViewById(this.getView()) != null && savedInstanceState != null) {
      return;
    }

    setContentView(this.getView());

    ButterKnife.bind(this);

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
  public void onNavigation(BottomAppBarOption option) {
    getFragmentToRender(option)
      .ifPresent(cvpFragment -> {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_layout, cvpFragment);
        ft.commit();
      });
  }

  private Optional<CvpFragment> getFragmentToRender(BottomAppBarOption option) {
    switch (option) {
      case CONNECTIONS:
        return Optional.of(new ConnectionsFragment());
      case HOME:
        return Optional.of(new HomeFragment());
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
}