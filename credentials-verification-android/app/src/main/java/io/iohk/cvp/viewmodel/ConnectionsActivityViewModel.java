package io.iohk.cvp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.iohk.cvp.grpc.GetConnectionTokenInfoRunnable;
import io.iohk.cvp.grpc.GetConnectionsInfoRunnable;
import io.iohk.cvp.grpc.GrpcTask;
import io.iohk.cvp.io.connector.ConnectionInfo;
import io.iohk.cvp.io.connector.ParticipantInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

public class ConnectionsActivityViewModel extends CvpViewModel {

  private MutableLiveData<List<ConnectionInfo>> connections = new MutableLiveData<>();
  private MutableLiveData<ParticipantInfo> issuerInfo = new MutableLiveData<>();

  @Inject
  public ConnectionsActivityViewModel() {
  }

  public LiveData<List<ConnectionInfo>> getConnections(Set<String> userIds) {
    userIds.forEach(userId -> {
      GrpcTask task = new GrpcTask<>(new GetConnectionsInfoRunnable(connections));
      task.execute(userId);
      runningTasks.add(task);
    });
    return connections;
  }

  public LiveData<ParticipantInfo> getConnectionTokenInfo(String token) {
    new GrpcTask<>(new GetConnectionTokenInfoRunnable(issuerInfo)).execute(null, token);
    return issuerInfo;
  }

  public void clearConnections() {
    connections.setValue(new ArrayList<>());
  }


}
