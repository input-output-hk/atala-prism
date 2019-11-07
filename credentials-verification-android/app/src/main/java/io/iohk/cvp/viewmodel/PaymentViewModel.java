package io.iohk.cvp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import javax.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.Data;

public class PaymentViewModel extends ViewModel {


  private final Payment[] hardcodeList = {
      new Payment(Calendar.getInstance(), "Credential Issuance Free University Tblisi", 10f,
          "\u20BE", PaymentState.PENDING),
      new Payment(Calendar.getInstance(),
          "Credential Issuance Business and \nTechnical University (BTU)", 25f,
          "\u20BE", PaymentState.COMPLETED),
      new Payment(Calendar.getInstance(), "Credential Verification HR.GE", 6f,
          "\u20BE", PaymentState.FAILED)
  };
  private MutableLiveData<List<Payment>> payments = new MutableLiveData<>();

  @Inject
  public PaymentViewModel() {
    payments.setValue(Arrays.asList(hardcodeList));
  }

  public LiveData<List<Payment>> getPayments() {
    return payments;
  }


  @Data
  @AllArgsConstructor
  public static class Payment {

    private final Calendar date;
    private final String title;
    private final Float amount;
    private final String currencySymbol;
    private final PaymentState state;
  }

  public enum PaymentState {
    PENDING,
    COMPLETED,
    FAILED
  }
}

