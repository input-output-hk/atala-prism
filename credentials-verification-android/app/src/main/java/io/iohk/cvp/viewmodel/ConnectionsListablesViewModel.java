package io.iohk.cvp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.protobuf.ByteString;
import io.iohk.cvp.grpc.GetConnectionsListableRunnable;
import io.iohk.cvp.grpc.GrpcTask;
import io.iohk.cvp.grpc.SendMessageRunnable;
import io.iohk.cvp.viewmodel.dtos.ConnectionListable;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

public class ConnectionsListablesViewModel extends CvpViewModel {

  private MutableLiveData<List<ConnectionListable>> connections = new MutableLiveData<>();
  private MutableLiveData<Boolean> messageSentSuccessfully = new MutableLiveData<>();

  @Inject
  public ConnectionsListablesViewModel() {
  }

  public LiveData<List<ConnectionListable>> getConnections(Set<String> userIds) {
    userIds.forEach(userId ->
        new GrpcTask<>(new GetConnectionsListableRunnable(connections), context).execute(userId));

    return connections;
  }

  public LiveData<Boolean> sendMessage(String senderUserId, String connectionId,
      ByteString message) {
    new GrpcTask<>(new SendMessageRunnable(messageSentSuccessfully), context)
        .execute(senderUserId, connectionId, message);

    return messageSentSuccessfully;
  }
}
