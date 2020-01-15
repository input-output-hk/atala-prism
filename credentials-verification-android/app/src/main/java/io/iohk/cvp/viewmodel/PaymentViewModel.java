package io.iohk.cvp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.iohk.cvp.grpc.GetPaymentsRunnable;
import io.iohk.cvp.grpc.GrpcTask;
import io.iohk.cvp.io.connector.Payment;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

public class PaymentViewModel extends CvpViewModel {

  private MutableLiveData<List<Payment>> payments = new MutableLiveData<>();

  @Inject
  public PaymentViewModel() {

  }

  public LiveData<List<Payment>> getPayments(Set<String> userIds) {
    userIds.forEach(userId -> {
      GrpcTask task = new GrpcTask<>(new GetPaymentsRunnable(payments), context);
      task.execute(userId);
      runningTasks.add(task);
    });
    return payments;
  }

  public void clearPayments() {
    payments.setValue(new ArrayList<>());
  }

  public enum PaymentState {
    CHARGED,
    FAILED
  }
}

