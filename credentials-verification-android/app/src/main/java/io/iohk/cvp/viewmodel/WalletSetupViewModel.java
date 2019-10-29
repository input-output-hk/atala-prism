package io.iohk.cvp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

public class WalletSetupViewModel extends ViewModel {

  // TODO: hardcode seed phrase
  private final String[] hardcodeSeedPhrase = {"fan", "enter", "win", "brick", "sniff", "act", "doll", "batman", "until", "test", "comic", "disney"};

  private MutableLiveData<List<String>> seedPhrase = new MutableLiveData<>();

  @Inject
  public WalletSetupViewModel() {
    seedPhrase.setValue(Arrays.asList(hardcodeSeedPhrase));
  }

  public LiveData<List<String>> getSeedPhrase() {
    return seedPhrase;
  }

  public boolean validateSeed(CharSequence toCompare, int seedNumber) {
    List<String> seedPhraseValue = seedPhrase.getValue();
    return seedPhraseValue != null && seedPhraseValue.get(seedNumber - 1).contentEquals(toCompare);
  }
}
