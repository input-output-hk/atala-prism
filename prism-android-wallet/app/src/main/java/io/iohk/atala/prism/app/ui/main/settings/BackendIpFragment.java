package io.iohk.atala.prism.app.ui.main.settings;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dagger.android.support.DaggerFragment;
import io.iohk.cvp.R;
import io.iohk.atala.prism.app.data.local.preferences.Preferences;

public class BackendIpFragment extends DaggerFragment {

    @BindView(R.id.backend_ip)
    EditText editTextBackendIp;

    @BindView(R.id.backend_port)
    EditText editTextBackendPort;

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_backend_ip_config, container, false);
        ButterKnife.bind(this, view);

        editTextBackendIp.setText(new Preferences(getContext()).getString(Preferences.BACKEND_IP));
        Preferences pref = new Preferences(getContext());
        Integer port = pref.getInt(Preferences.BACKEND_PORT);

        editTextBackendPort.setText(port.equals(0) ? "" : port.toString());
        return view;
    }

    @OnClick(R.id.save_ip)
    void onSaveClick() {
        Preferences pref = new Preferences(getContext());
        pref.saveBackendData(editTextBackendIp.getText().toString(),
                editTextBackendPort.getText().toString());

        Toast.makeText(getContext(), "Saved successfully", Toast.LENGTH_SHORT).show();
    }

}