package io.iohk.cvp.views.fragments;

import androidx.lifecycle.ViewModel;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.RootAppBar;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class SettingsFragment extends CvpFragment {

  @Override
  protected int getViewId() {
    return R.layout.fragment_settings;
  }

  @Override
  public ViewModel getViewModel() {
    return null;
  }

  @Override
  protected AppBarConfigurator getAppBarConfigurator() {
    return new RootAppBar(R.string.settings);
  }

  @OnClick(R.id.support)
  public void onSupportClick() {
    // TODO open zenddesk web view
  }
}
