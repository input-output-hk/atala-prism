package io.iohk.atala.crypto.japi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MnemonicCode {
    private final List<String> words;

    public MnemonicCode(List<String> words) {
        this.words = Collections.unmodifiableList(new ArrayList<>(words));
    }

    public List<String> getWords() {
        return words;
    }

}
