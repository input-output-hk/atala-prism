package io.iohk.atala.prism.app.ui.utils.components;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;

import io.iohk.atala.prism.app.ui.utils.interfaces.PinEditTextListener;

public class PinEditText extends androidx.appcompat.widget.AppCompatEditText {

    private String pinCharacter;
    private boolean pinShowChar;

    public PinEditText(Context context) {
        super(context);
    }

    public PinEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PinEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setTextChangedListener(PinEditTextListener listener, PinEditText nextPinCharacter) {

        this.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() == 0) {
                    pinCharacter = "";
                    return;
                }

                if (!getText().toString().equals("*")) {
                    pinCharacter = getText().toString();
                    clearFocus();

                    if (nextPinCharacter != null) {
                        nextPinCharacter.requestFocus();
                    } else {
                        listener.checkPins();
                    }

                    if (!pinShowChar)
                        setText("*");
                }
            }
        });
    }

    public String getPinCharacter() {
        return pinCharacter;
    }

    public void showText() {
        pinShowChar = true;
        setText(pinCharacter);
    }

    public void hideText() {
        pinShowChar = false;
        setText("*");
    }

}
