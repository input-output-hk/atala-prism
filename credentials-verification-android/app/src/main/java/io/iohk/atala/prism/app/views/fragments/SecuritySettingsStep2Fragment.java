package io.iohk.atala.prism.app.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.lifecycle.ViewModel;

import com.google.firebase.analytics.FirebaseAnalytics;

import javax.inject.Inject;

import butterknife.BindView;
import io.iohk.cvp.R;
import io.iohk.atala.prism.app.utils.FirebaseAnalyticsEvents;
import io.iohk.atala.prism.app.views.Navigator;
import io.iohk.atala.prism.app.views.Preferences;
import io.iohk.atala.prism.app.views.fragments.utils.AppBarConfigurator;
import io.iohk.atala.prism.app.views.fragments.utils.NoAppBar;
import lombok.Setter;

@Setter
public class SecuritySettingsStep2Fragment extends CvpFragment {

    private Preferences prefs;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Inject
    public SecuritySettingsStep2Fragment() {
    }

    @Inject
    Navigator navigator;

    @BindView(R.id.launchAuthentication)
    Button launchAuthentication;

    @BindView(R.id.cancel_button)
    TextView cancelButton;

    @Override
    protected int getViewId() {
        return R.layout.fragment_security_step2;
    }

    @Override
    public ViewModel getViewModel() {
        return null;
    }

    @Override
    protected AppBarConfigurator getAppBarConfigurator() {
        return new NoAppBar();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        prefs = new Preferences(getContext());
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(getContext());

        launchAuthentication.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.saveSecurityTouch(true);
                mFirebaseAnalytics.logEvent(FirebaseAnalyticsEvents.SECURE_APP_FINGERPRINT, null);
                navigator.showFragmentOnTopOfMenuNoBackstack(getFragmentManager(), new SecurityFragment());
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.saveSecurityTouch(false);
                navigator.showFragmentOnTopOfMenuNoBackstack(getFragmentManager(), new SecurityFragment());
            }
        });

        return view;
    }
}
