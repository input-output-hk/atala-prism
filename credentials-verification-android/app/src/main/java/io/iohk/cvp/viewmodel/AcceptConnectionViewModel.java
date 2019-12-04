package io.iohk.cvp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.iohk.cvp.grpc.AddConnectionFromTokenRunnable;
import io.iohk.cvp.grpc.GrpcTask;
import io.iohk.cvp.io.connector.AddConnectionFromTokenResponse;
import io.iohk.cvp.io.connector.PublicKey;
import javax.inject.Inject;

public class AcceptConnectionViewModel extends ViewModel {

  private MutableLiveData<AddConnectionFromTokenResponse> newConnectionInfo = new MutableLiveData<>();

  @Inject
  public AcceptConnectionViewModel() {
  }

  public LiveData<AddConnectionFromTokenResponse> addConnectionFromToken(
      String token,
      PublicKey publicKey) {
    new GrpcTask<>(new AddConnectionFromTokenRunnable(newConnectionInfo))
        .execute(null, token, publicKey);
    return newConnectionInfo;
  }
}
