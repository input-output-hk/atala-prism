package io.iohk.atala.prism.app.ui.main.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.navigation.Navigation;

import com.google.firebase.analytics.FirebaseAnalytics;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.support.DaggerFragment;
import io.iohk.atala.prism.app.neo.common.extensions.FragmentExtensionsKt;
import io.iohk.cvp.R;
import io.iohk.atala.prism.app.utils.FirebaseAnalyticsEvents;
import io.iohk.atala.prism.app.data.local.preferences.Preferences;
import lombok.Setter;

@Setter
public class SecuritySettingsStep2Fragment extends DaggerFragment {

    private Preferences prefs;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Inject
    public SecuritySettingsStep2Fragment() {
    }

    @BindView(R.id.launchAuthentication)
    Button launchAuthentication;

    @BindView(R.id.cancel_button)
    TextView cancelButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_security_step2, container, false);
        ButterKnife.bind(this, view);
        prefs = new Preferences(getContext());
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(getContext());
        launchAuthentication.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.saveSecurityTouch(true);
                mFirebaseAnalytics.logEvent(FirebaseAnalyticsEvents.SECURE_APP_FINGERPRINT, null);
                Navigation.findNavController(requireView()).navigate(R.id.action_securitySettingsStep2Fragment_to_securityFragment);
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.saveSecurityTouch(false);
                Navigation.findNavController(requireView()).navigate(R.id.action_securitySettingsStep2Fragment_to_securityFragment);
            }
        });
        FragmentExtensionsKt.getSupportActionBar(this).hide();
        return view;
    }
}