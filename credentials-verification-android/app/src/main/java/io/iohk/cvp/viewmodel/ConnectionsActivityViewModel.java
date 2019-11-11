package io.iohk.cvp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.iohk.cvp.grpc.GetConnectionTokenInfoRunnable;
import io.iohk.cvp.grpc.GetConnectionsInfoRunnable;
import io.iohk.cvp.grpc.GrpcTask;
import io.iohk.cvp.io.connector.ConnectionInfo;
import io.iohk.cvp.io.connector.ParticipantInfo;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;

public class ConnectionsActivityViewModel extends ViewModel {

  private MutableLiveData<List<ConnectionInfo>> connections = new MutableLiveData<>();
  private MutableLiveData<ParticipantInfo> issuerInfo = new MutableLiveData<>();

  @Inject
  public ConnectionsActivityViewModel() {
  }

  public LiveData<List<ConnectionInfo>> getConnections() {
    Optional<List<ConnectionInfo>> result = new GrpcTask<>(
        new GetConnectionsInfoRunnable()).doInBackground();
    result.ifPresent(info -> connections.setValue(info));

    return connections;
  }

  public LiveData<ParticipantInfo> getConnectionTokenInfo(String token) {
    Optional<ParticipantInfo> result = new GrpcTask<>(
        new GetConnectionTokenInfoRunnable()).doInBackground(
        "3l-uWR__jJLUAqaEEkEHbg==");
    result.ifPresent(info -> issuerInfo.setValue(info));

    return issuerInfo;
  }
}
