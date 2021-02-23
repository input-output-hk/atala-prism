package io.iohk.atala.prism.app.ui.main.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import androidx.navigation.Navigation;
import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.support.DaggerFragment;
import io.iohk.atala.prism.app.neo.common.extensions.FragmentExtensionsKt;
import io.iohk.cvp.R;
import io.iohk.atala.prism.app.data.local.preferences.Preferences;
import lombok.Setter;

@Setter
public class SecurityFragment extends DaggerFragment {

    @BindView(R.id.switchTouch)
    Switch switchTouch;

    @BindView(R.id.changePin)
    TextView changePin;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_security, container, false);
        ButterKnife.bind(this, view);
        if (new Preferences(requireContext()).getSecurityTouch()) {
            switchTouch.setChecked(true);
        }
        switchTouch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                new Preferences(requireContext()).saveSecurityTouch(isChecked);
            }
        });
        changePin.setOnClickListener(v -> {
            Navigation.findNavController(requireView()).navigate(R.id.action_securityFragment_to_securityChangePinFragment);
        });
        FragmentExtensionsKt.getSupportActionBar(this).hide();
        return view;
    }
}
