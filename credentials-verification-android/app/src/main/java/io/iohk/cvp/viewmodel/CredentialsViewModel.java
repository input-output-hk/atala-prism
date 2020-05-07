package io.iohk.cvp.viewmodel;

import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import io.iohk.cvp.grpc.AsyncTaskResult;
import io.iohk.cvp.grpc.GetMessagesRunnable;
import io.iohk.cvp.grpc.GrpcTask;
import io.iohk.prism.protos.ReceivedMessage;

public class CredentialsViewModel extends NewConnectionsViewModel {

    private final MutableLiveData<AsyncTaskResult<List<ReceivedMessage>>> messages = new MutableLiveData<>(
            new AsyncTaskResult<>());

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

}

