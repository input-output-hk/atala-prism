package io.iohk.atala.prism.app.ui.utils;

import android.text.Editable;
import android.text.TextWatcher;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class SimpleTextWatcher implements TextWatcher {

    private Boolean isCompleted = false;

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        setIsCompleted(s != null && s.length() > 0);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    public interface SimpleFormListener {

        void stateChanged(Boolean newState);
    }

    public static class SimpleFormWatcher {

        private final List<SimpleTextWatcher> watchers = new ArrayList<>();
        private final SimpleFormListener listener;
        private Boolean isCompleted = false;

        SimpleFormWatcher(SimpleFormListener listener,
                          List<TextInputEditText> inputs) {
            this.listener = listener;

            for (int i = 0; i < inputs.size(); i++) {
                watchers.add(
                        new SimpleTextWatcher() {
                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {
                                super.onTextChanged(s, start, before, count);
                                checkFormState();
                            }
                        }
                );
                inputs.get(i).addTextChangedListener(watchers.get(i));
            }
        }

        private void checkFormState() {
            Boolean newState = watchers.stream().allMatch(SimpleTextWatcher::getIsCompleted);

            if (newState != isCompleted) {
                listener.stateChanged(newState);
                this.isCompleted = newState;
            }
        }

        public static class Builder {

            private List<TextInputEditText> inputs = new ArrayList<>();
            private SimpleFormListener listener;

            public Builder addListener(SimpleFormListener listener) {
                this.listener = listener;
                return this;
            }

            public Builder addInput(TextInputEditText input) {
                inputs.add(input);
                return this;
            }

            public SimpleFormWatcher build() {
                return new SimpleFormWatcher(listener, inputs);
            }
        }
    }
}
