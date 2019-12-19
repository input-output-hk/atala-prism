package io.iohk.cvp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.iohk.cvp.grpc.AddConnectionFromTokenRunnable;
import io.iohk.cvp.grpc.GrpcTask;
import io.iohk.cvp.io.connector.AddConnectionFromTokenResponse;
import io.iohk.cvp.io.connector.ProcessPaymentResponse;
import io.iohk.cvp.io.connector.PublicKey;
import javax.inject.Inject;

public class MainViewModel extends ViewModel {

  private MutableLiveData<ProcessPaymentResponse> paymentResponse = new MutableLiveData<>();
  private MutableLiveData<AddConnectionFromTokenResponse> newConnectionInfo = new MutableLiveData<>();

  @Inject
  public MainViewModel() {
  }

  public LiveData<AddConnectionFromTokenResponse> addConnectionFromToken(
      String token,
      PublicKey publicKey, String nonce) {
    new GrpcTask<>(new AddConnectionFromTokenRunnable(newConnectionInfo))
        .execute(null, token, publicKey, nonce);
    return newConnectionInfo;
  }

}
