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
import java.util.Set;
import javax.inject.Inject;

public class ConnectionsActivityViewModel extends ViewModel {

  private MutableLiveData<List<ConnectionInfo>> connections = new MutableLiveData<>();
  private MutableLiveData<ParticipantInfo> issuerInfo = new MutableLiveData<>();

  @Inject
  public ConnectionsActivityViewModel() {
  }

  public LiveData<List<ConnectionInfo>> getConnections(Set<String> userIds) {
    userIds.forEach(userId ->
        new GrpcTask<>(new GetConnectionsInfoRunnable(connections)).execute(userId));
    return connections;
  }

  public LiveData<ParticipantInfo> getConnectionTokenInfo(String token) {
    // FIXME this shouldn't be sending user id
    new GrpcTask<>(new GetConnectionTokenInfoRunnable(issuerInfo)).execute(null, token);
    return issuerInfo;
  }

}
