package io.iohk.cvp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.iohk.cvp.grpc.GetConnectionsListableRunnable;
import io.iohk.cvp.grpc.GrpcTask;
import io.iohk.cvp.viewmodel.dtos.ConnectionListable;
import java.util.List;
import javax.inject.Inject;

public class ConnectionsListablesViewModel extends ViewModel {

  private MutableLiveData<List<ConnectionListable>> connections = new MutableLiveData<>();

  @Inject
  public ConnectionsListablesViewModel() {
  }

  public LiveData<List<ConnectionListable>> getConnections(String userId) {
    new GrpcTask<>(new GetConnectionsListableRunnable(connections)).execute();
    return connections;
  }
}
