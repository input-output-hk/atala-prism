package io.iohk.cvp.views.activities;

import android.os.Bundle;
import android.widget.Button;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.OnClick;
import com.crashlytics.android.Crashlytics;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import io.iohk.cvp.R;
import io.iohk.cvp.core.exception.CryptoException;
import io.iohk.cvp.crypto.ECKeys;
import io.iohk.cvp.io.wallet.KeyPair;
import io.iohk.cvp.viewmodel.WalletSetupViewModel;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.utils.SimpleTextWatcher;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;

public class SeedPhraseVerificationActivity extends CvpActivity<WalletSetupViewModel> {

  public static final String SEED_PHRASE_KEY = "seed_phrase";
  public static final String FIRST_WORD_INDEX_KEY = "first_word_index";
  public static final String SECOND_WORD_INDEX_KEY = "second_word_index";

  @Inject
  Navigator navigator;

  @BindView(R.id.verify_button)
  public Button verifyButton;

  @BindView(R.id.edit_text_1)
  public TextInputEditText inputEditText1;

  @BindView(R.id.edit_text_2)
  public TextInputEditText inputEditText2;

  @BindView(R.id.text_input_layout_1)
  public TextInputLayout inputEditTextLayout1;

  @BindView(R.id.text_input_layout_2)
  public TextInputLayout inputEditTextLayout2;

  @Inject
  ViewModelProvider.Factory factory;

  private Boolean firstSeedValidated = false;
  private Boolean secondSeedValidated = false;
  private List<String> seedPhrase = new ArrayList<>();

  protected int getView() {
    return R.layout.activity_seed_phrase_verification;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Objects.requireNonNull(getSupportActionBar()).hide();

    Bundle bundle = getIntent().getExtras();

    Objects.requireNonNull(bundle);

    seedPhrase = Arrays.asList(
        Objects.requireNonNull(bundle.getStringArray(SEED_PHRASE_KEY)));

    int firstWordIndexToCheck = bundle.getInt(FIRST_WORD_INDEX_KEY);

    inputEditTextLayout1.setHint(
        getString(R.string.word_numeric, String.valueOf(firstWordIndexToCheck)));

    inputEditText1.addTextChangedListener(new SimpleTextWatcher() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        firstSeedValidated = validateSeed(s, firstWordIndexToCheck);
        updateButtonState();
      }
    });

    int secondWordIndexToCheck = bundle.getInt(SECOND_WORD_INDEX_KEY);

    inputEditTextLayout2.setHint(
        getString(R.string.word_numeric, String.valueOf(secondWordIndexToCheck)));

    inputEditText2.addTextChangedListener(new SimpleTextWatcher() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        secondSeedValidated = validateSeed(s, secondWordIndexToCheck);
        updateButtonState();
      }
    });
  }

  private void updateButtonState() {
    verifyButton.setEnabled(firstSeedValidated && secondSeedValidated);
  }

  @Override
  protected int getTitleValue() {
    return R.string.seed_phrase_verification_activity_title;
  }

  @Override
  public WalletSetupViewModel getViewModel() {
    return ViewModelProviders.of(this, factory).get(WalletSetupViewModel.class);
  }

  @Override
  protected Navigator getNavigator() {
    return navigator;
  }

  @OnClick(R.id.verify_button)
  public void onContinueClick() {
    try {
      ECKeys crypto = new ECKeys();
      KeyPair keyPair = crypto.getKeyPair(seedPhrase);
      Preferences prefs = new Preferences(this);
      prefs.savePrivateKey(keyPair.getPrivateKey().toByteArray());
      navigator.showAccountCreated(this);
    } catch (CryptoException | InvalidKeySpecException e) {
      Crashlytics.logException(e);
      // TODO show error message
    }
  }

  private boolean validateSeed(CharSequence toCompare, int seedNumber) {
    return seedPhrase != null && seedPhrase.get(seedNumber - 1).contentEquals(toCompare);
  }
}