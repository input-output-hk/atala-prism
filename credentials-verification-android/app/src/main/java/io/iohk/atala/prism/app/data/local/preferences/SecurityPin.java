package io.iohk.atala.prism.app.data.local.preferences;

import androidx.annotation.Nullable;

import io.iohk.atala.prism.app.core.PrismApplication;
import io.iohk.cvp.R;
import io.iohk.atala.prism.app.core.exception.WrongPinLengthException;

public class SecurityPin {
    private static final int PIN_SIZE = 4;
    private final String pinString;

    public SecurityPin(String pinString) throws WrongPinLengthException {
        if (pinString.length() < 4)
            throw new WrongPinLengthException(PrismApplication.getInstance().getString(R.string.wrong_pin_size, PIN_SIZE));
        this.pinString = pinString;
    }

    public String getPinString() {
        return pinString;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof SecurityPin) {
            return ((SecurityPin) obj).getPinString().equals(pinString);
        }
        return super.equals(obj);
    }
}
