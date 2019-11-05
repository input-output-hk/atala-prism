package io.iohk.cvp.views.fragments;

import androidx.lifecycle.ViewModel;
import io.iohk.cvp.R;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.RootAppBar;
import javax.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class WalletFragment extends CvpFragment {

  @Inject
  Navigator navigator;

  @Override
  protected int getViewId() {
    return R.layout.fragment_wallet;
  }

  @Override
  public ViewModel getViewModel() {
    return null;
  }

  @Override
  protected AppBarConfigurator getAppBarConfigurator() {
    return new RootAppBar(R.string.wallet_title);
  }
}
