package io.iohk.cvp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.iohk.cvp.grpc.AsyncTaskResult;
import io.iohk.cvp.grpc.GetBraintreePaymentsConfigRunnable;
import io.iohk.cvp.grpc.GrpcTask;
import javax.inject.Inject;

public class AcceptConnectionViewModel extends CvpViewModel {

  private MutableLiveData<AsyncTaskResult<String>> tokenizationKey = new MutableLiveData<>();

  @Inject
  public AcceptConnectionViewModel() {
  }

  public LiveData<AsyncTaskResult<String>> getTokenizationKey() {
    new GrpcTask<>(new GetBraintreePaymentsConfigRunnable(tokenizationKey), context).execute();
    return tokenizationKey;
  }
}
