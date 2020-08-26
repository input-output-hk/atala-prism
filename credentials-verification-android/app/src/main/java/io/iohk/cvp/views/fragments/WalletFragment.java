package io.iohk.cvp.views.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import io.iohk.cvp.R;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.RootAppBar;
import java.util.Objects;
import javax.inject.Inject;

import lombok.Setter;

@Setter
public class WalletFragment extends CvpFragment {

  @Inject
  public WalletFragment() {
  }

  @Inject
  Navigator navigator;

  @Inject
  PaymentHistoryFragment paymentHistoryFragment;

  @Override
  protected int getViewId() {
    return R.layout.fragment_wallet;
  }

  @Override
  public ViewModel getViewModel() {
    return null;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    MenuItem paymentHistoryMenuItem;
    paymentHistoryMenuItem = menu.findItem(R.id.action_history);
    paymentHistoryMenuItem.setVisible(true);
  }

  @Override
  protected AppBarConfigurator getAppBarConfigurator() {
    return new RootAppBar(R.string.wallet_title, Color.WHITE);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_history) {
      navigator.showFragmentOnTop(
          requireActivity().getSupportFragmentManager(),
          paymentHistoryFragment);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
}
