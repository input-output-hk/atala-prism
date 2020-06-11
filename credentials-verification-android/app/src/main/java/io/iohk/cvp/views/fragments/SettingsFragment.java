package io.iohk.cvp.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.lifecycle.ViewModel;

import java.util.Objects;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.cvp.BuildConfig;
import io.iohk.cvp.R;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.RootAppBar;
import io.iohk.cvp.views.utils.components.OptionItem;
import lombok.Setter;

@Setter
public class SettingsFragment extends CvpFragment {

  @Inject
  public SettingsFragment() {
  }

  @Inject
  Navigator navigator;

  @BindView(R.id.backend_ip)
  OptionItem backendConfigItem;

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

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);

    if (!BuildConfig.DEBUG) {
      backendConfigItem.setVisibility(View.GONE);
    }

    return view;
  }

  @OnClick(R.id.support)
  void onSupportClick() {
    navigator.showWebView(Objects.requireNonNull(this.getActivity()));
  }

  @OnClick(R.id.backend_ip)
  void onBackendIpClick() {
    navigator.showFragmentOnTopOfMenu(getFragmentManager(), new BackendIpFragment());
  }

  @OnClick(R.id.security)
  void onSecurityClick() {
    if(!new Preferences(getContext()).isPinConfigured()){
      navigator.showFragmentOnTopOfMenu(getFragmentManager(), new SecuritySettingsStep1Fragment());
    }else{
      navigator.showFragmentOnTopOfMenu(getFragmentManager(), new SecurityFragment());
    }
  }

  // Disable for the time being
  @OnClick(R.id.about)
  void onAboutClick() {
//    navigator.showFragmentOnTopOfMenu(getFragmentManager(), new AboutFragment());
  }
}