package io.iohk.cvp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.iohk.cvp.grpc.GetBraintreePaymentsConfigRunnable;
import io.iohk.cvp.grpc.GrpcTask;
import javax.inject.Inject;

public class AcceptConnectionViewModel extends ViewModel {

  private MutableLiveData<String> tokenizationKey = new MutableLiveData<>();

  @Inject
  public AcceptConnectionViewModel() {
  }

  public LiveData<String> getTokenizationKey() {
    new GrpcTask<>(new GetBraintreePaymentsConfigRunnable(tokenizationKey)).execute();
    return tokenizationKey;
  }
}
