package io.iohk.atala.prism.app.ui.utils;

import android.view.View;
import android.widget.RelativeLayout;

import java.util.List;
import java.util.stream.Collectors;

import io.iohk.atala.prism.app.ui.utils.components.PinEditText;

public class SecurityPinHelper {

    public static void changePinsState(List<PinEditText> pinList, boolean isPinHidden) {
        pinList.forEach(pinEditText -> {
            if (isPinHidden) {
                pinEditText.showText();
            } else {
                pinEditText.hideText();
            }
        });
    }

    public static String getCurrentTextFromPinList(List<PinEditText> pinList) {
        return pinList
                .stream()
                .filter(pinEditText -> pinEditText.getPinCharacter() != null)
                .map(PinEditText::getPinCharacter)
                .collect(Collectors.joining());
    }

    public static void checkPins(String pin, String repeatedPin, RelativeLayout confirmationMessage, RelativeLayout errorMessage) {
        if (pin.equals(repeatedPin) && !pin.isEmpty()) {
            confirmationMessage.setVisibility(View.VISIBLE);
            errorMessage.setVisibility(View.GONE);
        } else {
            errorMessage.setVisibility(View.VISIBLE);
            confirmationMessage.setVisibility(View.GONE);
        }
    }
}
