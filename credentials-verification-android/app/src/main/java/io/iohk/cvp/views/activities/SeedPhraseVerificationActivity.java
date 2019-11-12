package io.iohk.cvp.views.activities;

import android.os.Bundle;
import android.widget.Button;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.OnClick;
import com.google.android.material.textfield.TextInputEditText;
import io.iohk.cvp.R;
import io.iohk.cvp.viewmodel.WalletSetupViewModel;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.utils.SimpleTextWatcher;
import java.util.Objects;
import javax.inject.Inject;

public class SeedPhraseVerificationActivity extends CvpActivity<WalletSetupViewModel> {

  @Inject
  Navigator navigator;

  @BindView(R.id.verify_button)
  public Button verifyButton;

  @BindView(R.id.edit_text_1)
  public TextInputEditText inputEditText1;

  @BindView(R.id.edit_text_2)
  public TextInputEditText inputEditText2;

  @Inject
  ViewModelProvider.Factory factory;

  private Boolean firstSeedValidated = false;
  private Boolean secondSeedValidated = false;

  protected int getView() {
    return R.layout.activity_seed_phrase_verification;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Objects.requireNonNull(getSupportActionBar()).hide();

    inputEditText1.addTextChangedListener(new SimpleTextWatcher() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        firstSeedValidated = viewModel.validateSeed(s, 5);
        updateButtonState();
      }
    });

    inputEditText2.addTextChangedListener(new SimpleTextWatcher() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        secondSeedValidated = viewModel.validateSeed(s, 10);
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
    // TODO: i created this stage for a simple simulation, it's not necesari to save the keys in this moment,
    // you can create and save the keys in other place and here only set a boolean "isWalletCreated"
    new Preferences(this).savePrivateKey("this is a mock pk");
    navigator.showConnections(this);
  }
}