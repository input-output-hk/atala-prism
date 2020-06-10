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
import io.iohk.cvp.grpc.GetMessagesRunnable;
import io.iohk.cvp.grpc.GrpcTask;
import io.iohk.prism.protos.ConnectionInfo;
import io.iohk.prism.protos.ParticipantInfo;
import io.iohk.prism.protos.ReceivedMessage;

public class ConnectionsActivityViewModel extends NewConnectionsViewModel {

    private MutableLiveData<AsyncTaskResult<List<ConnectionInfo>>> connections = new MutableLiveData<>();
    private MutableLiveData<AsyncTaskResult<ParticipantInfo>> issuerInfo = new MutableLiveData<>();
    private MutableLiveData<AsyncTaskResult<List<ReceivedMessage>>> messages = new MutableLiveData<>();


    @Inject
    public ConnectionsActivityViewModel() {
    }

    public LiveData<AsyncTaskResult<List<ConnectionInfo>>> getConnections(Set<String> userIds) {
        GrpcTask task = new GrpcTask<>(new GetConnectionsInfoRunnable(connections), context);
        task.execute(userIds);
        runningTasks.add(task);
        return connections;
    }

    public MutableLiveData<AsyncTaskResult<List<ReceivedMessage>>> getMessages(Set<String> userIds) {
        GrpcTask task = new GrpcTask<>(new GetMessagesRunnable(messages), context);
        task.execute(userIds);
        runningTasks.add(task);
        return messages;
    }

    public void clearConnections() {
        connections.setValue(new AsyncTaskResult<>(new ArrayList<>()));
    }
}
