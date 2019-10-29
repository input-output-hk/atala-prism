package io.iohk.cvp.views.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.GridView;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import java.util.Objects;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.viewmodel.WalletSetupViewModel;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.utils.adapters.SeedPhraseAdapter;
import io.iohk.cvp.views.utils.components.CheckboxWithDescription;

public class WalletSetupActivity extends CvpActivity<WalletSetupViewModel> implements CheckboxWithDescription.CheckboxStateListener {

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

    viewModel.getSeedPhrase().observe(this, seedPhrase -> {
      adapter.setSeedPhrase(seedPhrase);
      adapter.notifyDataSetChanged();
    });

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
    navigator.showSeedPhraseVerification(this);
  }

}