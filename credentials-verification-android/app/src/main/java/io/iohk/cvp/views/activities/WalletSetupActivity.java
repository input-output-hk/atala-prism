package io.iohk.cvp.views.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.GridView;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.google.firebase.analytics.FirebaseAnalytics;

import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.utils.FirebaseAnalyticsEvents;
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
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException.MnemonicLengthException;

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

  private FirebaseAnalytics mFirebaseAnalytics;

  protected int getView() {
    return R.layout.activity_wallet_setup;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Objects.requireNonNull(getSupportActionBar()).hide();

    mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

    checkbox.setListeners(this, null);
    adapter = new SeedPhraseAdapter();
    gridView.setAdapter(adapter);

    InputStream inputStream = getResources().openRawResource(R.raw.word_list);
    try {
      byte[] entropy = new byte[128 / 8];
      MnemonicCode mnemonic = new MnemonicCode(inputStream,
          "ad90bf3beb7b0eb7e5acd74727dc0da96e0a280a258354e7293fb7e211ac03db");
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
    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, null);

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
    mFirebaseAnalytics.logEvent(FirebaseAnalyticsEvents.ACCEPT_RECOVERY_PHRASE_CONTINUE, null);

    navigator.showSeedPhraseVerification(this, adapter.getSeedPhrase(), firstWordIndexToCheck,
        secondWordIndexToCheck);
  }

}