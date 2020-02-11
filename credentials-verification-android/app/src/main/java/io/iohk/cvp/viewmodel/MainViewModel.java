package io.iohk.cvp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.iohk.cvp.grpc.AddConnectionFromTokenRunnable;
import io.iohk.cvp.grpc.AsyncTaskResult;
import io.iohk.cvp.grpc.GrpcTask;
import io.iohk.cvp.io.connector.AddConnectionFromTokenResponse;
import io.iohk.cvp.io.connector.PublicKey;
import javax.inject.Inject;

public class MainViewModel extends CvpViewModel {

  private MutableLiveData<AsyncTaskResult<AddConnectionFromTokenResponse>> newConnectionInfo = new MutableLiveData<>();

  @Inject
  public MainViewModel() {
  }

  public LiveData<AsyncTaskResult<AddConnectionFromTokenResponse>> addConnectionFromToken(
      String token,
      PublicKey publicKey, String nonce) {
    new GrpcTask<>(new AddConnectionFromTokenRunnable(newConnectionInfo), context)
        .execute(null, token, publicKey, nonce);
    return newConnectionInfo;
  }

}
