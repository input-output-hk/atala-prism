package io.iohk.cvp.viewmodel;

import static io.iohk.cvp.core.exception.ErrorCode.ITEM_NOT_FOUND_IN_LIST;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.crashlytics.android.Crashlytics;
import com.google.protobuf.InvalidProtocolBufferException;
import io.iohk.cvp.core.exception.ItemNotFoundException;
import io.iohk.cvp.grpc.GetMessagesRunnable;
import io.iohk.cvp.grpc.GrpcTask;
import io.iohk.cvp.io.connector.Credential;
import io.iohk.cvp.io.connector.ReceivedMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.inject.Inject;

public class CredentialsViewModel extends ViewModel {

  private final MutableLiveData<List<ReceivedMessage>> messages = new MutableLiveData<>(
      new ArrayList<>());
  private final MutableLiveData<Credential> selectedCredential = new MutableLiveData<>();

  @Inject
  public CredentialsViewModel() {
    selectedCredential.setValue(Credential.getDefaultInstance());
  }

  public LiveData<List<ReceivedMessage>> getMessages(Set<String> userIds) {
    userIds.forEach(userId ->
        new GrpcTask<>(new GetMessagesRunnable(messages)).execute(userId));
    return messages;
  }


  public LiveData<Credential> getCredential(String userId, String messageId)
      throws InvalidProtocolBufferException, ExecutionException, InterruptedException {

    Optional<List<ReceivedMessage>> msgs = new GrpcTask<>(new GetMessagesRunnable(messages))
        .execute(userId).get();

    try {
      selectedCredential
          .setValue(findCredential(msgs.orElse(new ArrayList<>()), messageId)
              .orElseThrow(() -> new ItemNotFoundException("The selected credential id " + messageId
                  + " wasn't found on current messages list", ITEM_NOT_FOUND_IN_LIST)));
    } catch (ItemNotFoundException e) {
      Crashlytics.logException(e);
    }

    return selectedCredential;
  }

  private Optional<Credential> findCredential(List<ReceivedMessage> messages, String messageId)
      throws InvalidProtocolBufferException {
    if (messages.size() > 0) {
      return Optional.of(Credential.parseFrom(messages.stream()
          .filter(message -> message.getId().equals(messageId)).collect(Collectors.toList())
          .get(0).getMessage()));
    }
    return Optional.empty();

  }
}

