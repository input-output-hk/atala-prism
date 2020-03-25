package io.iohk.cvp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import javax.inject.Inject;

import io.iohk.cvp.grpc.AddConnectionFromTokenRunnable;
import io.iohk.cvp.grpc.AsyncTaskResult;
import io.iohk.cvp.grpc.GrpcTask;
import io.iohk.prism.protos.AddConnectionFromTokenResponse;
import io.iohk.prism.protos.ConnectorPublicKey;

public class MainViewModel extends CvpViewModel {

  private MutableLiveData<AsyncTaskResult<AddConnectionFromTokenResponse>> newConnectionInfo = new MutableLiveData<>();

  @Inject
  public MainViewModel() {
  }

  public LiveData<AsyncTaskResult<AddConnectionFromTokenResponse>> addConnectionFromToken(
          String token,
          ConnectorPublicKey publicKey, String nonce) {
    new GrpcTask<>(new AddConnectionFromTokenRunnable(newConnectionInfo), context)
        .execute(null, token, publicKey, nonce);
    return newConnectionInfo;
  }

}
