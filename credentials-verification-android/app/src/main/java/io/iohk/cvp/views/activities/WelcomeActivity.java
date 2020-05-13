package io.iohk.cvp.views.activities;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.lifecycle.ViewModel;
import butterknife.BindColor;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.OnClick;
import com.crashlytics.android.Crashlytics;
import com.google.firebase.analytics.FirebaseAnalytics;

import io.iohk.cvp.R;
import io.iohk.cvp.core.exception.CaseNotFoundException;
import io.iohk.cvp.core.exception.ErrorCode;
import io.iohk.cvp.utils.FirebaseAnalyticsEvents;
import io.iohk.cvp.views.Navigator;
import java.util.Objects;
import javax.inject.Inject;

public class WelcomeActivity extends CvpActivity {

  @Inject
  Navigator navigator;

  @BindView(R.id.welcome_view)
  View welcomeView;

  @BindView(R.id.tutorial_step1_view)
  View tutorialStep1View;

  @BindView(R.id.tutorial_step2_view)
  View tutorialStep2View;

  @BindView(R.id.tutorial_step3_view)
  View tutorialStep3View;

  @BindView(R.id.steps_counter)
  View stepsCounter;

  @BindView(R.id.step1_text)
  TextView step1Text;

  @BindView(R.id.step1_dot)
  View step1Dot;

  @BindView(R.id.step2_text)
  TextView step2Text;

  @BindView(R.id.step2_dot)
  View step2Dot;

  @BindView(R.id.step3_dot)
  View step3Dot;

  @BindView(R.id.get_started_button)
  Button getStartedBtn;

  @BindView(R.id.restore_account_btn)
  Button restoreAccountBtn;

  @BindView(R.id.create_account_btn)
  Button createAccountBtn;

  @BindColor(R.color.black)
  ColorStateList blackColor;

  @BindColor(R.color.grey_2)
  ColorStateList mediumGrayColor;

  @BindString(R.string.continue_string)
  String continueString;

  @BindString(R.string.get_started)
  String getStartedString;

  private int currentStep = 0;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    Objects.requireNonNull(getSupportActionBar()).hide();
  }

  @Override
  public void onBackPressed() {
    if (currentStep > 0) {
      currentStep--;
      changeViewsByStep(false);
    } else {
      super.onBackPressed();
    }
  }

  @OnClick(R.id.get_started_button)
  public void onClickGetStart() {
    changeViewsByStep(true);
    currentStep++;
  }

  @OnClick(R.id.create_account_btn)
  public void onClickCreateAccount() {
    // TODO this should take you to the wallet setup
    navigator.showTermsAndConditions(this);

    Bundle bundle = new Bundle();
    bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, FirebaseAnalyticsEvents.CREATE_ACCOUNT);
    FirebaseAnalytics.getInstance(this).logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
  }

  private void changeViewsByStep(boolean isAdvancing) {
    switch (currentStep) {
      case 0:
        prepareViewForWelcomeLayout(isAdvancing);
        break;
      case 1:
        prepareViewForTutorialsFirstStep(isAdvancing);
        break;
      case 2:
        prepareViewForTutorialsFinalsStep(isAdvancing);
        break;
      default:
        Crashlytics.logException(
            new CaseNotFoundException("Couldn't find view for step " + currentStep,
                ErrorCode.STEP_NOT_FOUND));
    }
  }

  private void prepareViewForTutorialsFinalsStep(boolean isAdvancing) {
    tutorialStep2View.setVisibility(isAdvancing ? View.GONE : View.VISIBLE);
    tutorialStep3View.setVisibility(isAdvancing ? View.VISIBLE : View.GONE);
    step2Text.setTextColor(isAdvancing ? mediumGrayColor : blackColor);
    step2Dot.setVisibility(isAdvancing ? View.GONE : View.VISIBLE);
    step3Dot.setVisibility(isAdvancing ? View.VISIBLE : View.GONE);
    getStartedBtn.setVisibility(isAdvancing ? View.GONE : View.VISIBLE);
//    restoreAccountBtn.setVisibility(isAdvancing ? View.VISIBLE : View.GONE);
    createAccountBtn.setVisibility(isAdvancing ? View.VISIBLE : View.GONE);
  }

  private void prepareViewForTutorialsFirstStep(boolean isAdvancing) {
    tutorialStep1View.setVisibility(isAdvancing ? View.GONE : View.VISIBLE);
    tutorialStep2View.setVisibility(isAdvancing ? View.VISIBLE : View.GONE);
    step1Text.setTextColor(isAdvancing ? mediumGrayColor : blackColor);
    step1Dot.setVisibility(isAdvancing ? View.GONE : View.VISIBLE);
    step2Dot.setVisibility(isAdvancing ? View.VISIBLE : View.GONE);
  }

  private void prepareViewForWelcomeLayout(boolean isAdvancing) {
    welcomeView.setVisibility(isAdvancing ? View.GONE : View.VISIBLE);
    stepsCounter.setVisibility(isAdvancing ? View.VISIBLE : View.GONE);
    tutorialStep1View.setVisibility(isAdvancing ? View.VISIBLE : View.GONE);
    getStartedBtn.setText(isAdvancing ? continueString : getStartedString);
  }


  @Override
  protected Navigator getNavigator() {
    return this.navigator;
  }

  @Override
  protected int getView() {
    return R.layout.welcome_activity;
  }

  @Override
  protected int getTitleValue() {
    return R.string.empty_title;
  }

  @Override
  public ViewModel getViewModel() {
    return null;
  }
}
