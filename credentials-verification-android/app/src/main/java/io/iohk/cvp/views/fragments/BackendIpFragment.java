package io.iohk.cvp.views.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.lifecycle.ViewModel;
import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.RootAppBar;
import javax.inject.Inject;

public class BackendIpFragment extends CvpFragment {

  @BindView(R.id.backend_ip)
  EditText editTextBackendIp;

  @BindView(R.id.backend_port)
  EditText editTextBackendPort;

  @Inject
  public BackendIpFragment() {
  }

  @SuppressLint("SetTextI18n")
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);

    Preferences prefs = new Preferences(getContext());

    editTextBackendIp.setText(prefs.getString(Preferences.BACKEND_IP));

    Integer port = prefs.getInt(Preferences.BACKEND_PORT);

    editTextBackendPort.setText(port.equals(0) ? "" : port.toString());

    return view;
  }

  @Override
  protected int getViewId() {
    return R.layout.fragment_backend_ip_config;
  }

  @Override
  public ViewModel getViewModel() {
    return null;
  }

  @Override
  protected AppBarConfigurator getAppBarConfigurator() {
    return new RootAppBar(R.string.settings);
  }

  @OnClick(R.id.save_ip)
  void onSaveClick() {
    Preferences prefs = new Preferences(getContext());
    prefs.saveBackendData(editTextBackendIp.getText().toString(),
        editTextBackendPort.getText().toString());

    Toast.makeText(getContext(), "Saved successfully", Toast.LENGTH_SHORT).show();
  }

}
