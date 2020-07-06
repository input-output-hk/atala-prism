package io.iohk.cvp.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModel;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Calendar;
import java.util.Objects;

import javax.inject.Inject;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.utils.Constants;
import io.iohk.cvp.utils.FirebaseAnalyticsEvents;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.fragments.LargeDescriptionDialogFragment;
import io.iohk.cvp.views.fragments.utils.TermsAndConditionHelper;
import io.iohk.cvp.views.utils.components.CheckboxWithDescription;

public class TermsAndConditionsActivity extends CvpActivity {

  @Inject
  Navigator navigator;

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

  private FirebaseAnalytics mFirebaseAnalytics;

  protected int getView() {
    return R.layout.terms_and_conditions_activity;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Objects.requireNonNull(getSupportActionBar()).hide();
    mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

    firstCheckbox.setListeners(
            isClicked -> {
              termsAndConditionsChecked = isClicked;
              updateButtonState();
            },
            () -> TermsAndConditionHelper.showTermsAndConditions(this)
    );
    secondCheckbox.setListeners(
            isClicked -> {
              privacyPolicyChecked = isClicked;
              updateButtonState();
            },
            () -> TermsAndConditionHelper.showPrivacyPolicy(this)
    );
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
    mFirebaseAnalytics.logEvent(FirebaseAnalyticsEvents.CONTINUE_AFTER_TC_PP, null);
    navigator.showWalletSetup(this);
  }

}