package io.iohk.cvp.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.biometric.BiometricManager;
import androidx.lifecycle.ViewModel;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.core.exception.WrongPinLengthException;
import io.iohk.cvp.data.preferences.SecurityPin;
import io.iohk.cvp.utils.FirebaseAnalyticsEvents;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.NoAppBar;
import io.iohk.cvp.views.fragments.utils.SecurityPinHelper;
import io.iohk.cvp.views.utils.components.PinEditText;
import io.iohk.cvp.views.utils.interfaces.PinEditTextListener;
import lombok.Setter;

@Setter
public class SecuritySettingsStep1Fragment extends CvpFragment implements PinEditTextListener {

    private boolean pinShowChar = false;
    private boolean repeatPinShowChar = false;

    private List<PinEditText> pinEditTexts = new ArrayList<>();
    private List<PinEditText> pinRepeatEditTexts = new ArrayList<>();
    private FirebaseAnalytics mFirebaseAnalytics;

    @Inject
    public SecuritySettingsStep1Fragment() {
    }

    @Inject
    Navigator navigator;

    @BindView(R.id.savePin)
    Button savePin;

    @BindView(R.id.textSteps)
    TextView textSteps;

    @BindView(R.id.pinCharacter1)
    PinEditText pinCharacter1;

    @BindView(R.id.pinCharacter2)
    PinEditText pinCharacter2;

    @BindView(R.id.pinCharacter3)
    PinEditText pinCharacter3;

    @BindView(R.id.pinCharacter4)
    PinEditText pinCharacter4;

    @BindView(R.id.pinRepeatCharacter1)
    PinEditText pinRepeatCharacter1;

    @BindView(R.id.pinRepeatCharacter2)
    PinEditText pinRepeatCharacter2;

    @BindView(R.id.pinRepeatCharacter3)
    PinEditText pinRepeatCharacter3;

    @BindView(R.id.pinRepeatCharacter4)
    PinEditText pinRepeatCharacter4;

    @BindView(R.id.confirmatioMessage)
    RelativeLayout confirmationMessage;

    @BindView(R.id.errorMessage)
    RelativeLayout errorMessage;

    @Override
    protected int getViewId() {
        return R.layout.fragment_security_step1;
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

        if(BiometricManager.from(getActivity()).canAuthenticate() == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
            textSteps.setVisibility(View.GONE);
        }
        pinEditTexts.addAll(Arrays.asList(pinCharacter1, pinCharacter2, pinCharacter3, pinCharacter4));
        pinRepeatEditTexts.addAll(Arrays.asList(pinRepeatCharacter1, pinRepeatCharacter2, pinRepeatCharacter3, pinRepeatCharacter4));
        initPinEditTexts();

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(getContext());
        return view;
    }

    private void initPinEditTexts(){
        List<PinEditText> combinedLists = Stream.of(pinEditTexts, pinRepeatEditTexts)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        IntStream.range(0, combinedLists.size()).forEach(value -> {
            if(value < combinedLists.size() - 1) {
                combinedLists.get(value).setTextChangedListener(this, combinedLists.get(value + 1));
            } else {
                combinedLists.get(value).setTextChangedListener(this, null);
            }
        });
    }

    public void checkPins(){
        SecurityPinHelper.checkPins(getPin(), getRepeatedPin(), confirmationMessage, errorMessage);
    }

    @OnClick(R.id.savePin)
    public void onSavePin() {
        Preferences prefs = new Preferences(getContext());
        try {
            SecurityPin securityPin = new SecurityPin(getPin());
            SecurityPin securityPinConfirm = new SecurityPin(getRepeatedPin());
            if(securityPin.equals(securityPinConfirm)) {
                prefs.saveSecurityPin(securityPin);
                mFirebaseAnalytics.logEvent(FirebaseAnalyticsEvents.SECURE_APP_FINGERPRINT_PASSCODE,null);
                if(BiometricManager.from(getActivity()).canAuthenticate() == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
                    navigator.showFragmentOnTopOfMenuNoBackstack(getFragmentManager(), new SecurityFragment());
                } else {
                    navigator.showFragmentOnTopOfMenuNoBackstack(getFragmentManager(), new SecuritySettingsStep2Fragment());
                }
            } else {
                Toast.makeText(getActivity(), R.string.same_pin_required, Toast.LENGTH_SHORT).show();
            }
        } catch (WrongPinLengthException e) {
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.showPin)
    public void onShowPin() {
        pinShowChar = !pinShowChar;
        SecurityPinHelper.changePinsState(pinEditTexts, pinShowChar);
    }

    @OnClick(R.id.showRepeatPin)
    public void onShowRepeatPin() {
        repeatPinShowChar = !repeatPinShowChar;
        SecurityPinHelper.changePinsState(pinRepeatEditTexts, repeatPinShowChar);
    }

    private String getPin() {
        return SecurityPinHelper.getCurrentTextFromPinList(pinEditTexts);
    }

    private String getRepeatedPin() {
        return SecurityPinHelper.getCurrentTextFromPinList(pinRepeatEditTexts);
    }
}
