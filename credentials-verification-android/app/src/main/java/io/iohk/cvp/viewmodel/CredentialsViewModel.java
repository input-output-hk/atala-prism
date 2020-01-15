package io.iohk.cvp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.iohk.cvp.grpc.GetMessagesRunnable;
import io.iohk.cvp.grpc.GrpcTask;
import io.iohk.cvp.io.connector.ReceivedMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

public class CredentialsViewModel extends CvpViewModel {

  private final MutableLiveData<List<ReceivedMessage>> messages = new MutableLiveData<>(
      new ArrayList<>());

  @Inject
  public CredentialsViewModel() {
  }

  public void clearMessages() {
    this.messages.setValue(new ArrayList<>());
  }

  public LiveData<List<ReceivedMessage>> getMessages(Set<String> userIds) {
    userIds.forEach(userId -> {
      GrpcTask task = new GrpcTask<>(new GetMessagesRunnable(messages), context);
      task.execute(userId);
      runningTasks.add(task);
    });
    return messages;
  }

}

