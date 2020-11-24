package io.iohk.atala.prism.app.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.lifecycle.ViewModel;

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
import io.iohk.atala.prism.app.core.exception.WrongPinLengthException;
import io.iohk.atala.prism.app.data.local.preferences.SecurityPin;
import io.iohk.atala.prism.app.views.Navigator;
import io.iohk.atala.prism.app.data.local.preferences.Preferences;
import io.iohk.atala.prism.app.views.fragments.utils.AppBarConfigurator;
import io.iohk.atala.prism.app.views.fragments.utils.NoAppBar;
import io.iohk.atala.prism.app.views.fragments.utils.SecurityPinHelper;
import io.iohk.atala.prism.app.views.utils.components.PinEditText;
import io.iohk.atala.prism.app.views.utils.interfaces.PinEditTextListener;
import lombok.Setter;

@Setter
public class SecurityChangePinFragment extends CvpFragment implements PinEditTextListener {

    private boolean currentPinShowChar = false;
    private boolean newPinShowChar = false;
    private boolean confirmPinShowChar = false;

    private List<PinEditText> currentPinEditTextList = new ArrayList<>();
    private List<PinEditText> newPinEditTextList = new ArrayList<>();
    private List<PinEditText> repeatPinEditTextList = new ArrayList<>();


    @Inject
    public SecurityChangePinFragment() {
    }

    @Inject
    Navigator navigator;

    @BindView(R.id.savePin)
    Button savePin;

    @BindView(R.id.pinCharacter1)
    PinEditText pinCharacter1;

    @BindView(R.id.pinCharacter2)
    PinEditText pinCharacter2;

    @BindView(R.id.pinCharacter3)
    PinEditText pinCharacter3;

    @BindView(R.id.pinCharacter4)
    PinEditText pinCharacter4;

    @BindView(R.id.newPinCharacter1)
    PinEditText newPinCharacter1;

    @BindView(R.id.newPinCharacter2)
    PinEditText newPinCharacter2;

    @BindView(R.id.newPinCharacter3)
    PinEditText newPinCharacter3;

    @BindView(R.id.newPinCharacter4)
    PinEditText newPinCharacter4;

    @BindView(R.id.pinRepeatCharacter1)
    PinEditText pinRepeatCharacter1;

    @BindView(R.id.pinRepeatCharacter2)
    PinEditText pinRepeatCharacter2;

    @BindView(R.id.pinRepeatCharacter3)
    PinEditText pinRepeatCharacter3;

    @BindView(R.id.pinRepeatCharacter4)
    PinEditText pinRepeatCharacter4;

    @BindView(R.id.confirmationMessage)
    RelativeLayout confirmationMessage;

    @BindView(R.id.errorMessage)
    RelativeLayout errorMessage;

    @Override
    protected int getViewId() {
        return R.layout.fragment_change_pin;
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

        currentPinEditTextList.addAll(Arrays.asList(pinCharacter1, pinCharacter2, pinCharacter3, pinCharacter4));
        newPinEditTextList.addAll(Arrays.asList(newPinCharacter1, newPinCharacter2, newPinCharacter3, newPinCharacter4));
        repeatPinEditTextList.addAll(Arrays.asList(pinRepeatCharacter1, pinRepeatCharacter2, pinRepeatCharacter3, pinRepeatCharacter4));
        initPinEditTexts();

        return view;
    }

    private void initPinEditTexts() {
        List<PinEditText> combinedLists = Stream.of(currentPinEditTextList, newPinEditTextList, repeatPinEditTextList)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        IntStream.range(0, combinedLists.size()).forEach(value -> {
            if (value < combinedLists.size() - 1) {
                combinedLists.get(value).setTextChangedListener(this, combinedLists.get(value + 1));
            } else {
                combinedLists.get(value).setTextChangedListener(this, null);
            }
        });
    }

    public void checkPins() {
        SecurityPinHelper.checkPins(getNewPin(), getRepeatPin(), confirmationMessage, errorMessage);
    }

    @OnClick(R.id.savePin)
    public void onSavePin() {
        Preferences prefs = new Preferences(getContext());
        try {
            SecurityPin securityCurrentPin = new SecurityPin(getCurrentPin());
            SecurityPin securityPinNew = new SecurityPin(getNewPin());
            SecurityPin securityPinConfirm = new SecurityPin(getRepeatPin());
            if (securityPinNew.equals(securityPinConfirm)) {
                if (prefs.getSecurityPin().equals(securityCurrentPin)) {
                    prefs.saveSecurityPin(securityPinNew);
                    getFragmentManager().popBackStack();
                } else {
                    Toast.makeText(getActivity(), R.string.incorrect_current_pin, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getActivity(), R.string.same_pin_required, Toast.LENGTH_SHORT).show();
            }
        } catch (WrongPinLengthException e) {
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.showCurrentPin)
    public void onShowCurrentPin() {
        currentPinShowChar = !currentPinShowChar;
        SecurityPinHelper.changePinsState(currentPinEditTextList, currentPinShowChar);
    }

    @OnClick(R.id.showNewPin)
    public void onShowNewPin() {
        newPinShowChar = !newPinShowChar;
        SecurityPinHelper.changePinsState(newPinEditTextList, newPinShowChar);
    }

    @OnClick(R.id.showConfirmPin)
    public void onShowConfirmPin() {
        confirmPinShowChar = !confirmPinShowChar;
        SecurityPinHelper.changePinsState(repeatPinEditTextList, confirmPinShowChar);
    }

    private String getCurrentPin() {
        return SecurityPinHelper.getCurrentTextFromPinList(currentPinEditTextList);
    }

    private String getNewPin() {
        return SecurityPinHelper.getCurrentTextFromPinList(newPinEditTextList);
    }

    private String getRepeatPin() {
        return SecurityPinHelper.getCurrentTextFromPinList(repeatPinEditTextList);
    }
}
