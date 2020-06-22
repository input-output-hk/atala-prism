package io.iohk.cvp.views.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.SupportErrorDialogFragment;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.cvp.BuildConfig;
import io.iohk.cvp.R;
import io.iohk.cvp.grpc.AsyncTaskResult;
import io.iohk.cvp.viewmodel.ConnectionsActivityViewModel;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.activities.MainActivity;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.RootAppBar;
import io.iohk.cvp.views.utils.components.OptionItem;
import io.iohk.cvp.views.utils.dialogs.SuccessDialog;
import io.iohk.prism.protos.ConnectionInfo;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class SettingsFragment extends CvpFragment implements DeleteAllConnectionsDialogFragment.OnResetDataListener {

  // Request code is obligatory on setTargetFragment() even if is not used for transfer data back
  private static final int DELETE_ALL_CONNECTIONS_REQUEST_CODE = 22;
  private ViewModelProvider.Factory factory;
  private ConnectionsActivityViewModel connectionsActivityViewModel;
  private LiveData<AsyncTaskResult<List<ConnectionInfo>>> liveData;

  @Inject
  public SettingsFragment(ViewModelProvider.Factory factory) {
    this.factory = factory;
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

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    connectionsActivityViewModel = ViewModelProviders.of(this, factory)
            .get(ConnectionsActivityViewModel.class);
    connectionsActivityViewModel.setContext(getContext());
  }

  // Disable for the time being
  @OnClick(R.id.support)
  void onSupportClick() {
//    navigator.showWebView(Objects.requireNonNull(this.getActivity()));
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

  @OnClick(R.id.about)
  void onAboutClick() {
    navigator.showFragmentOnTopOfMenu(getFragmentManager(), new AboutFragment());
  }


  @OnClick(R.id.delete_credentials)
  void onDeleteCredentialsClicked() {

    DeleteAllConnectionsDialogFragment deleteAllConnectionsDialogFragment = DeleteAllConnectionsDialogFragment.newInstance();
    deleteAllConnectionsDialogFragment.setTargetFragment(this, DELETE_ALL_CONNECTIONS_REQUEST_CODE);
    getNavigator().showDialogFragment(getFragmentManager(),
            deleteAllConnectionsDialogFragment, "DELETE_CONNECTIONS_DIALOG_FRAGMENT");
  }

  @Override
  public void resetData() {
    Preferences prefs = new Preferences(getContext());
    liveData = connectionsActivityViewModel.getConnections(prefs.getUserIds());
    if (liveData.hasActiveObservers()) {
      return;
    }
    showLoading();
    liveData.observe(this, response -> {
      try {
        FragmentManager fm = getFragmentManager();
        if (response.getError() != null) {
          SupportErrorDialogFragment.newInstance(new Dialog(getContext()))
                  .show(fm, "");
          getNavigator().showPopUp(getFragmentManager(), getResources().getString(
                  R.string.server_error_message));
          return;
        }
        List<ConnectionInfo> connections = response.getResult();
        List<String> connectionIdList = connections.stream().map(ConnectionInfo::getConnectionId).collect(Collectors.toList());
        new Preferences(getContext()).deleteUserConnections(connectionIdList);
        ((MainActivity)getActivity()).clearIssueConnections();
        SuccessDialog.newInstance(this, R.string.connections_remove_success, true)
                .show(getFragmentManager(), "dialog");
      } catch (Exception ex) {
        Crashlytics.logException(ex);
      } finally {
        hideLoading();
      }
    });
  }
}