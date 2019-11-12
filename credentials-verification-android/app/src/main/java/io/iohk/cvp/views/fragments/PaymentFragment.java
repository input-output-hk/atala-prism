package io.iohk.cvp.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import butterknife.BindView;
import butterknife.OnClick;
import com.google.android.material.textfield.TextInputEditText;
import io.iohk.cvp.R;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.StackedAppBar;
import io.iohk.cvp.views.utils.SimpleTextWatcher.SimpleFormListener;
import io.iohk.cvp.views.utils.SimpleTextWatcher.SimpleFormWatcher;
import java.util.Objects;
import javax.inject.Inject;

public class PaymentFragment extends CvpFragment implements SimpleFormListener {

  @Inject
  Navigator navigator;

  @BindView(R.id.continue_button)
  public Button continueButton;

  @BindView(R.id.text_view_amount)
  public TextView textViewAmount;

  @BindView(R.id.edit_text_credit_card_number)
  public TextInputEditText inputEditTextCreditCardNumber;

  @BindView(R.id.edit_text_expiry_date)
  public TextInputEditText inputEditTextExpiryDate;

  @BindView(R.id.edit_text_cvv_number)
  public TextInputEditText inputEditTextCVVNumber;

  @BindView(R.id.edit_text_cardholder_name)
  public TextInputEditText inputEditTextCardholderName;


  @Override
  protected int getViewId() {
    return R.layout.fragment_payment;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View v = super.onCreateView(inflater, container, savedInstanceState);

    new SimpleFormWatcher.Builder()
        .addListener(this)
        .addInput(inputEditTextCreditCardNumber)
        .addInput(inputEditTextExpiryDate)
        .addInput(inputEditTextCVVNumber)
        .addInput(inputEditTextCardholderName)
        .build();
    return v;
  }

  @Override
  public ViewModel getViewModel() {
    return null;
  }

  @Override
  protected AppBarConfigurator getAppBarConfigurator() {
    setHasOptionsMenu(true);
    return new StackedAppBar(R.string.payment_activity_title);
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      Objects.requireNonNull(getActivity()).onBackPressed();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @OnClick(R.id.continue_button)
  public void onContinueClick() {
    navigator.showFragmentOnTop(
        Objects.requireNonNull(getActivity()).getSupportFragmentManager(),
        new PaymentCongratsFragment());

  }

  @Override
  public void stateChanged(Boolean isCompleted) {
    continueButton.setEnabled(isCompleted);
  }
}