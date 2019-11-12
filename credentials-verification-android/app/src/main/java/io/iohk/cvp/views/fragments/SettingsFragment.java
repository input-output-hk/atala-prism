package io.iohk.cvp.views.fragments;

import android.graphics.Color;
import androidx.lifecycle.ViewModel;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.RootAppBar;
import java.util.Objects;
import javax.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
public class SettingsFragment extends CvpFragment {

  @Inject
  public SettingsFragment() {
  }

  @Inject
  Navigator navigator;

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
  void onSupportClick() {
    navigator.showWebView(Objects.requireNonNull(this.getActivity()));
  }
}
