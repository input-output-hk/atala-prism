package io.iohk.cvp.views.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.lifecycle.ViewModel;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.core.exception.WrongPinLengthException;
import io.iohk.cvp.data.local.preferences.SecurityPin;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.Preferences;
import lombok.NonNull;
import lombok.Setter;

@Setter
public class UnlockActivity extends CvpActivity {

    private Preferences prefs;

    private BiometricPrompt myBiometricPrompt;
    private String pin = "";

    @Inject
    Navigator navigator;

    private List<TextView> pinEditTexts = new ArrayList<>();

    @BindView(R.id.pinCharacter1)
    TextView pinCharacter1;

    @BindView(R.id.pinCharacter2)
    TextView pinCharacter2;

    @BindView(R.id.pinCharacter3)
    TextView pinCharacter3;

    @BindView(R.id.pinCharacter4)
    TextView pinCharacter4;

    @BindView(R.id.biometrics)
    ImageButton biometrics;

    @Override
    protected Navigator getNavigator() {
        return navigator;
    }

    @Override
    protected int getTitleValue() {
        return R.string.empty_title;
    }

    @Override
    public ViewModel getViewModel() {
        return null;
    }

    protected int getView() {
        return R.layout.fragment_unlock;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getView());
        ButterKnife.bind(this);
        Objects.requireNonNull(getSupportActionBar()).hide();
        prefs = new Preferences(this);
        initBiometrics();
        pinEditTexts.addAll(Arrays.asList(pinCharacter1, pinCharacter2, pinCharacter3, pinCharacter4));
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (BiometricManager.from(getBaseContext()).canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS && prefs.getSecurityTouch()) {
            biometrics.setVisibility(View.VISIBLE);
        } else {
            biometrics.setVisibility(View.INVISIBLE);
        }
    }

    private void initBiometrics() {
        Executor newExecutor = Executors.newSingleThreadExecutor();
        //Start listening for authentication events//
        myBiometricPrompt = new BiometricPrompt(this, newExecutor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            //onAuthenticationError is called when a fatal error occurs//
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED)
                    biometrics.setVisibility(View.INVISIBLE);
            }

            //onAuthenticationSucceeded is called when a fingerprint is matched successfully//
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                UnlockActivity.this.finish();
            }

            //onAuthenticationFailed is called when the fingerprint doesn’t match//
            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }
        });
    }

    private void addPinNumber(String number) {
        if (pin.length() > 3) {
            return;
        }
        pin = pin.concat(number);
        updatePinViews();
        try {
            if (pin.length() == 4) {
                if (prefs.getSecurityPin().equals(new SecurityPin(pin))) {
                    UnlockActivity.this.finish();
                } else {
                    Toast.makeText(UnlockActivity.this, R.string.incorrect_pin_code, Toast.LENGTH_SHORT).show();
                }
            }
        } catch (WrongPinLengthException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private void removePinNumber() {
        if (pin.length() == 0) {
            return;
        }
        pin = StringUtils.substring(pin, 0, pin.length() - 1);
        updatePinViews();
    }

    private void updatePinViews() {
        IntStream.range(0, pinEditTexts.size()).forEach(value -> {
            String characterTochange = pin.length() > value ? "*" : "";
            pinEditTexts.get(value).setText(characterTochange);
        });
    }

    public void authenticateFingerprint() {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Touch ID for ATALA")
                .setSubtitle("Unlock you app")
                .setDescription("")
                .setNegativeButtonText("Cancel")
                .build();
        myBiometricPrompt.authenticate(promptInfo);
    }

    @OnClick(R.id.number0)
    void onNumber0Click() {
        addPinNumber("0");
    }

    @OnClick(R.id.number1)
    void onNumber1Click() {
        addPinNumber("1");
    }

    @OnClick(R.id.number2)
    void onNumber2Click() {
        addPinNumber("2");
    }

    @OnClick(R.id.number3)
    void onNumber3Click() {
        addPinNumber("3");
    }

    @OnClick(R.id.number4)
    void onNumber4Click() {
        addPinNumber("4");
    }

    @OnClick(R.id.number5)
    void onNumber5Click() {
        addPinNumber("5");
    }

    @OnClick(R.id.number6)
    void onNumber6Click() {
        addPinNumber("6");
    }

    @OnClick(R.id.number7)
    void onNumber7Click() {
        addPinNumber("7");
    }

    @OnClick(R.id.number8)
    void onNumber8Click() {
        addPinNumber("8");
    }

    @OnClick(R.id.number9)
    void onNumber9Click() {
        addPinNumber("9");
    }

    @OnClick(R.id.biometrics)
    void onBiometricsClick() {
        authenticateFingerprint();
    }

    @OnClick(R.id.delete)
    void onDeleteClick() {
        removePinNumber();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
