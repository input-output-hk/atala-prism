package io.iohk.cvp.views.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.lifecycle.ViewModel;
import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.views.Navigator;
import java.util.Objects;
import javax.inject.Inject;

public class AccountCreatedActivity extends CvpActivity {

  @Inject
  Navigator navigator;

  @BindView(R.id.button)
  public Button continueButton;

  @BindView(R.id.text_view_title)
  public TextView textViewTitle;

  @BindView(R.id.text_view_description)
  public TextView textViewDescription;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Objects.requireNonNull(getSupportActionBar()).hide();
    
    continueButton.setText(R.string.continue_string);
    textViewTitle.setText(R.string.your_account_has_been_created);
    textViewDescription.setText(R.string.continue_connectiong);
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

  @Override
  protected int getView() {
    return R.layout.layout_congratulations;
  }

  @OnClick(R.id.button)
  public void onContinueClick() {
    navigator.showConnections(this);
  }

}