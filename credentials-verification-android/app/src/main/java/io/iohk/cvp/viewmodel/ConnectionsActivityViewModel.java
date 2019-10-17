package io.iohk.cvp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.iohk.cvp.grpc.GetConnectionTokenInfoRunnable;
import io.iohk.cvp.grpc.GrpcTask;
import io.iohk.cvp.io.connector.IssuerInfo;
import io.iohk.cvp.io.connector.ParticipantInfo;
import java.util.Optional;
import javax.inject.Inject;

public class ConnectionsActivityViewModel extends ViewModel {

  private MutableLiveData<IssuerInfo> issuerInfo;

  @Inject
  public ConnectionsActivityViewModel() {
  }

  public LiveData<IssuerInfo> getIssuerInfo(String token) {
    Optional<ParticipantInfo> result = (Optional<ParticipantInfo>) new GrpcTask(
        new GetConnectionTokenInfoRunnable()).doInBackground(
        token);
    result.ifPresent(info -> issuerInfo.setValue(info.getIssuer()));

    return issuerInfo;
  }
}
