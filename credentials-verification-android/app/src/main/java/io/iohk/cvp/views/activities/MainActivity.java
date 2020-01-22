package io.iohk.cvp.views.activities;

import static io.iohk.cvp.utils.ActivitiesRequestCodes.BRAINTREE_REQUEST_ACTIVITY;
import static io.iohk.cvp.views.Preferences.CONNECTION_TOKEN_TO_ACCEPT;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.FrameLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindColor;
import butterknife.BindView;
import butterknife.OnClick;
import com.braintreepayments.api.dropin.DropInActivity;
import com.braintreepayments.api.dropin.DropInResult;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.crashlytics.android.Crashlytics;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import io.iohk.cvp.R;
import io.iohk.cvp.core.exception.CaseNotFoundException;
import io.iohk.cvp.core.exception.CryptoException;
import io.iohk.cvp.core.exception.ErrorCode;
import io.iohk.cvp.core.exception.SharedPrefencesDataNotFoundException;
import io.iohk.cvp.utils.CryptoUtils;
import io.iohk.cvp.viewmodel.MainViewModel;
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
import java.math.BigDecimal;
import java.math.MathContext;
import java.security.spec.InvalidKeySpecException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import lombok.Getter;

public class MainActivity extends CvpActivity<MainViewModel> implements BottomAppBarListener {

  public static final String MAIN_FRAGMENT_TAG = "MAIN_FRAGMENT";

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

  @BindView(R.id.fab)
  FloatingActionButton fab;

  @BindView(R.id.bottom_appbar)
  public BottomAppBar bottomAppBar;

  @BindView(R.id.fragment_layout)
  public FrameLayout frameLayout;

  @BindColor(R.color.colorPrimary)
  ColorStateList colorRed;

  @BindColor(R.color.black)
  ColorStateList colorBlack;

  MenuItem paymentHistoryMenuItem;

  @Inject
  ViewModelProvider.Factory factory;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
        WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
    super.onCreate(savedInstanceState);

    bottomAppBar.setListener(this);
    fab.setBackgroundTintList(colorRed);

    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

    Preferences prefs = new Preferences(getApplicationContext());
    if (prefs.getUserIds().size() > 0) {
      ft.replace(R.id.fragment_layout, connectionsFragment, MAIN_FRAGMENT_TAG);
    } else {
      ft.replace(R.id.fragment_layout, new FirstConnectionFragment(), MAIN_FRAGMENT_TAG);
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
  public MainViewModel getViewModel() {
    MainViewModel viewModel = ViewModelProviders.of(this, factory).get(MainViewModel.class);
    viewModel.setContext(getApplicationContext());
    return viewModel;
  }

  @Override
  public void onNavigation(BottomAppBarOption option, String userId) {
    if (BottomAppBarOption.CONNECTIONS.equals(option)) {
      fab.setBackgroundTintList(colorRed);
      bottomAppBar.setItemColors(option);
    } else {
      fab.setBackgroundTintList(colorBlack);
    }

    getFragmentToRender(option)
        .ifPresent(cvpFragment -> {
          Fragment currentFragment = this.getSupportFragmentManager()
              .findFragmentByTag(MAIN_FRAGMENT_TAG);
          if (currentFragment instanceof ConnectionsFragment
              && cvpFragment instanceof ConnectionsFragment) {
            Set<String> userIds = new HashSet<>();
            userIds.add(userId);
            ((ConnectionsFragment) currentFragment).listConnections(userIds);
          } else {
            navigator.showFragment(getSupportFragmentManager(), cvpFragment, MAIN_FRAGMENT_TAG);
          }
        });
  }

  private Optional<CvpFragment> getFragmentToRender(BottomAppBarOption option) {
    switch (option) {
      case CONNECTIONS:
        Preferences prefs = new Preferences(getApplicationContext());
        if (prefs.getUserIds().size() > 0) {
          return Optional.of(connectionsFragment);
        }
        return Optional.of(new FirstConnectionFragment());
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

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == BRAINTREE_REQUEST_ACTIVITY) {
      if (resultCode == RESULT_OK) {
        DropInResult result = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT);
        PaymentMethodNonce nonce = result.getPaymentMethodNonce();
        String strNonce = nonce.getNonce();

        Preferences prefs = new Preferences(this);

        // TODO send real amount
        sendPayments(new BigDecimal(150.1, MathContext.DECIMAL32), strNonce,
            prefs.getString(CONNECTION_TOKEN_TO_ACCEPT),
            prefs);
      } else {
        if (resultCode != RESULT_CANCELED) {
          Exception error = (Exception) data.getSerializableExtra(DropInActivity.EXTRA_ERROR);
          Crashlytics.logException(error);
          // TODO show error message
        }
      }
    }
  }

  @OnClick(R.id.fab)
  public void onFabClick() {
    onNavigation(BottomAppBarOption.CONNECTIONS, null);
  }

  private void sendPayments(BigDecimal amount, String nonce, String connectionToken,
      Preferences prefs) {
    try {
      viewModel
          .addConnectionFromToken(connectionToken, CryptoUtils.getPublicKey(prefs), nonce)
          .observe(this, connectionInfo -> {
            prefs.addConnection(connectionInfo);
            onNavigation(BottomAppBarOption.CONNECTIONS, connectionInfo.getUserId());
          });
    } catch (SharedPrefencesDataNotFoundException | InvalidKeySpecException | CryptoException e) {
      Crashlytics.logException(e);
      // TODO show error message
    }
  }
}