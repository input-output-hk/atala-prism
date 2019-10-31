package io.iohk.cvp.views.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModel;

import java.util.Calendar;
import java.util.Objects;

import javax.inject.Inject;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.fragments.LargeDescriptionDialogFragment;
import io.iohk.cvp.views.utils.components.CheckboxWithDescription;

public class TermsAndConditionsActivity extends CvpActivity {

  @Inject
  Navigator navigator;

  @BindView(R.id.large_description_frame)
  public FrameLayout frameLayout;

  @BindView(R.id.terms_and_conditions_checkbox_layout)
  public CheckboxWithDescription firstCheckbox;

  @BindView(R.id.privacy_policy_checkbox_layout)
  public CheckboxWithDescription secondCheckbox;

  @BindView(R.id.continue_button)
  public Button continueButton;

  @BindString(R.string.terms_and_conditions_activity_title)
  public String termsAndConditionsTitle;

  @BindString(R.string.privacy_policies_agreement)
  public String policiesTitle;

  private Boolean termsAndConditionsChecked = false;
  private Boolean privacyPolicyChecked = false;

  protected int getView() {
    return R.layout.terms_and_conditions_activity;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Objects.requireNonNull(getSupportActionBar()).hide();
    firstCheckbox.setListeners(
      isClicked -> {
        termsAndConditionsChecked = isClicked;
        updateButtonState();
      },
      // TODO: get the last time the terms and conditions where updated
      () -> showLargeDescription(termsAndConditionsTitle, Calendar.getInstance(), R.string.terms_and_conditions_asset_name)
    );
    secondCheckbox.setListeners(
      isClicked -> {
        privacyPolicyChecked = isClicked;
        updateButtonState();
      },
      // TODO: get the last time the privacy policies and conditions where updated
      // TODO: import the privacy policies asset when needed
      () -> showLargeDescription(policiesTitle, Calendar.getInstance(), R.string.terms_and_conditions_asset_name)
    );
  }

  public void showLargeDescription(String title, Calendar lastUpdated, int assetResourceId) {
    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    ft.replace(R.id.large_description_frame, new LargeDescriptionDialogFragment(title, lastUpdated, assetResourceId));
    ft.addToBackStack(title);
    ft.commit();
  }

  private void updateButtonState() {
    continueButton.setEnabled(privacyPolicyChecked && termsAndConditionsChecked);
  }

  @Override
  protected int getTitleValue() {
    return R.string.terms_and_conditions_activity_title;
  }

  @Override
  public ViewModel getViewModel() {
    return null;
  }

  @Override
  protected Navigator getNavigator() {
    return navigator;
  }

  @OnClick(R.id.continue_button)
  public void onContinueClick() {
    navigator.showConnections(this);
  }

}