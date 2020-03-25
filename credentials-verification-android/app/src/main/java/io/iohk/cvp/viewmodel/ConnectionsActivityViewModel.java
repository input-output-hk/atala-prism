package io.iohk.cvp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import io.iohk.cvp.grpc.AsyncTaskResult;
import io.iohk.cvp.grpc.GetConnectionTokenInfoRunnable;
import io.iohk.cvp.grpc.GetConnectionsInfoRunnable;
import io.iohk.cvp.grpc.GrpcTask;
import io.iohk.prism.protos.ConnectionInfo;
import io.iohk.prism.protos.ParticipantInfo;

public class ConnectionsActivityViewModel extends CvpViewModel {

  private MutableLiveData<AsyncTaskResult<List<ConnectionInfo>>> connections = new MutableLiveData<>();
  private MutableLiveData<AsyncTaskResult<ParticipantInfo>> issuerInfo = new MutableLiveData<>();

  @Inject
  public ConnectionsActivityViewModel() {
  }

  public LiveData<AsyncTaskResult<List<ConnectionInfo>>> getConnections(Set<String> userIds) {
    userIds.forEach(userId -> {
      GrpcTask task = new GrpcTask<>(new GetConnectionsInfoRunnable(connections), context);
      task.execute(userId);
      runningTasks.add(task);
    });
    return connections;
  }

  public LiveData<AsyncTaskResult<ParticipantInfo>> getConnectionTokenInfo(String token) {
    new GrpcTask<>(new GetConnectionTokenInfoRunnable(issuerInfo), context).execute(null, token);
    return issuerInfo;
  }

  public void clearConnections() {
    connections.setValue(new AsyncTaskResult<>(new ArrayList<>()));
  }
}
