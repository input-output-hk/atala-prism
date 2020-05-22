package io.iohk.cvp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import io.iohk.cvp.grpc.AsyncTaskResult;
import io.iohk.cvp.grpc.GetConnectionsInfoRunnable;
import io.iohk.cvp.grpc.GetMessagesRunnable;
import io.iohk.cvp.grpc.GrpcTask;
import io.iohk.prism.protos.ConnectionInfo;
import io.iohk.prism.protos.ReceivedMessage;

public class CredentialsViewModel extends NewConnectionsViewModel {

    private final MutableLiveData<AsyncTaskResult<List<ReceivedMessage>>> messages = new MutableLiveData<>(
            new AsyncTaskResult<>());
    private MutableLiveData<AsyncTaskResult<List<ConnectionInfo>>> connections = new MutableLiveData<>();

    @Inject
    public CredentialsViewModel() {
    }

    public void clearMessages() {
        this.messages.setValue(
                new AsyncTaskResult<>(new ArrayList<>()));
    }

    public MutableLiveData<AsyncTaskResult<List<ReceivedMessage>>> getMessages(Set<String> userIds) {
        GrpcTask task = new GrpcTask<>(new GetMessagesRunnable(messages), context);
        task.execute(userIds);
        runningTasks.add(task);
        return messages;
    }

    public LiveData<AsyncTaskResult<List<ConnectionInfo>>> getConnections(Set<String> userIds) {
        GrpcTask task = new GrpcTask<>(new GetConnectionsInfoRunnable(connections), context);
        task.execute(userIds);
        runningTasks.add(task);
        return connections;
    }

}

