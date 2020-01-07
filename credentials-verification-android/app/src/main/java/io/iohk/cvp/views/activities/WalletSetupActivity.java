package io.iohk.cvp.views.activities;

import static org.bitcoinj.crypto.MnemonicCode.BIP39_ENGLISH_SHA256;

import android.os.Bundle;
import android.widget.Button;
import android.widget.GridView;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.core.exception.MnemonicException.MnemonicLengthException;
import org.bitcoinj.crypto.MnemonicCode;
import io.iohk.cvp.viewmodel.WalletSetupViewModel;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.utils.adapters.SeedPhraseAdapter;
import io.iohk.cvp.views.utils.components.CheckboxWithDescription;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import javax.inject.Inject;

public class WalletSetupActivity extends CvpActivity<WalletSetupViewModel> implements
    CheckboxWithDescription.CheckboxStateListener {

  private static final Integer[] seedPhraseIndexes = new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

  @Inject
  Navigator navigator;

  @Inject
  ViewModelProvider.Factory factory;

  @BindView(R.id.acceptance_checkbox)
  public CheckboxWithDescription checkbox;

  @BindView(R.id.accept_button)
  public Button acceptButton;

  @BindView(R.id.grid_view_seed_phrase)
  public GridView gridView;

  private SeedPhraseAdapter adapter;

  private Integer firstWordIndexToCheck;

  private Integer secondWordIndexToCheck;

  protected int getView() {
    return R.layout.activity_wallet_setup;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Objects.requireNonNull(getSupportActionBar()).hide();

    checkbox.setListeners(this, null);
    adapter = new SeedPhraseAdapter();
    gridView.setAdapter(adapter);

    InputStream inputStream = getResources().openRawResource(R.raw.word_list);
    try {
      MnemonicCode mnemonic = new MnemonicCode(inputStream, BIP39_ENGLISH_SHA256);

      byte[] entropy = mnemonic.getSeedBytes();
      new SecureRandom().nextBytes(entropy);
      adapter.setSeedPhrase(mnemonic.toMnemonic(entropy));
      adapter.notifyDataSetChanged();
    } catch (IOException | MnemonicLengthException e) {
      e.printStackTrace();
    }

    List<Integer> indexesList = new LinkedList<>(Arrays.asList(seedPhraseIndexes));
    Random rand = new Random();
    firstWordIndexToCheck = indexesList.get(rand.nextInt(indexesList.size()));
    indexesList.remove(firstWordIndexToCheck);
    secondWordIndexToCheck = indexesList.get(rand.nextInt(indexesList.size()));
  }

  @Override
  public void stateChanged(Boolean isClicked) {
    acceptButton.setEnabled(isClicked);
  }

  @Override
  protected int getTitleValue() {
    return R.string.wallet_setup_activity_title;
  }

  @Override
  public WalletSetupViewModel getViewModel() {
    return ViewModelProviders.of(this, factory).get(WalletSetupViewModel.class);
  }

  @Override
  protected Navigator getNavigator() {
    return navigator;
  }

  @OnClick(R.id.accept_button)
  public void onContinueClick() {
    navigator.showSeedPhraseVerification(this, adapter.getSeedPhrase(), firstWordIndexToCheck,
        secondWordIndexToCheck);
  }

}