package io.iohk.cvp.viewmodel;

import android.os.AsyncTask;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.protobuf.AbstractMessageLite;
import com.google.protobuf.ByteString;
import io.iohk.cvp.grpc.AsyncTaskResult;
import io.iohk.cvp.grpc.GetConnectionsListableRunnable;
import io.iohk.cvp.grpc.GrpcTask;
import io.iohk.cvp.grpc.SendMessageRunnable;
import io.iohk.cvp.viewmodel.dtos.ConnectionListable;
import io.iohk.prism.protos.Credential;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

  public LiveData<AsyncTaskResult<Boolean>> sendMultipleMessage(String senderUserId, String connectionId,
                                                        List<Credential> messages) {
    new GrpcTask<>(new SendMessageRunnable(messageSentSuccessfully), context)
            .execute(senderUserId, connectionId, messages.stream()
                    .map(AbstractMessageLite::toByteString)
                    .collect(Collectors.toList()));
    return messageSentSuccessfully;
  }
}
