package io.iohk.cvp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.protobuf.ByteString;
import io.iohk.cvp.grpc.AsyncTaskResult;
import io.iohk.cvp.grpc.GetConnectionsListableRunnable;
import io.iohk.cvp.grpc.GrpcTask;
import io.iohk.cvp.grpc.SendMessageRunnable;
import io.iohk.cvp.viewmodel.dtos.ConnectionListable;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

public class ConnectionsListablesViewModel extends CvpViewModel {

  private MutableLiveData<AsyncTaskResult<List<ConnectionListable>>> connections = new MutableLiveData<>();
  private MutableLiveData<AsyncTaskResult<Boolean>> messageSentSuccessfully = new MutableLiveData<>();

  @Inject
  public ConnectionsListablesViewModel() {
  }

  public LiveData<AsyncTaskResult<List<ConnectionListable>>> getConnections(Set<String> userIds) {
    new GrpcTask<>(new GetConnectionsListableRunnable(connections), context).execute(userIds);

    return connections;
  }

  public LiveData<AsyncTaskResult<Boolean>> sendMessage(String senderUserId, String connectionId,
      ByteString message) {
    new GrpcTask<>(new SendMessageRunnable(messageSentSuccessfully), context)
        .execute(senderUserId, connectionId, message);

    return messageSentSuccessfully;
  }
}
