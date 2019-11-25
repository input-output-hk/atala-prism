package io.iohk.cvp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.iohk.cvp.grpc.AddConnectionFromTokenRunnable;
import io.iohk.cvp.grpc.GetConnectionTokenInfoRunnable;
import io.iohk.cvp.grpc.GetConnectionsInfoRunnable;
import io.iohk.cvp.grpc.GrpcTask;
import io.iohk.cvp.io.connector.ConnectionInfo;
import io.iohk.cvp.io.connector.ParticipantInfo;
import io.iohk.cvp.viewmodel.dtos.ConnectionListable;
import java.util.List;
import javax.inject.Inject;

public class ConnectionsActivityViewModel extends ViewModel {

  private MutableLiveData<List<ConnectionInfo>> connections = new MutableLiveData<>();
  private MutableLiveData<ParticipantInfo> issuerInfo = new MutableLiveData<>();
  private MutableLiveData<ConnectionInfo> newConnectionInfo = new MutableLiveData<>();

  @Inject
  public ConnectionsActivityViewModel() {
  }

  public LiveData<List<ConnectionInfo>> getConnections() {
    new GrpcTask<>(new GetConnectionsInfoRunnable(connections)).execute();
    return connections;
  }

  public LiveData<ParticipantInfo> getConnectionTokenInfo(String token) {
    new GrpcTask<>(new GetConnectionTokenInfoRunnable(issuerInfo)).execute(token);
    return issuerInfo;
  }

  public LiveData<ConnectionInfo> addConnectionFromToken(String token) {
    new GrpcTask<>(new AddConnectionFromTokenRunnable(newConnectionInfo)).execute(token);
    return newConnectionInfo;
  }
}
