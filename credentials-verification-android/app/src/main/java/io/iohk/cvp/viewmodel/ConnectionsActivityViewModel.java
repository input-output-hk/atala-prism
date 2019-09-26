package io.iohk.cvp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.grpc.ManagedChannel;
import io.iohk.cvp.io.connector.IssuerInfo;
import java.util.Optional;
import javax.inject.Inject;

public class ConnectionsActivityViewModel extends ViewModel {

  private MutableLiveData<IssuerInfo> issuerInfo;
  private ManagedChannel channel;

  @Inject
  public ConnectionsActivityViewModel() {
  }

  public LiveData<IssuerInfo> getIssuerInfo(String token) {
    if (issuerInfo == null) {
      issuerInfo = new MutableLiveData<>();
      loadUsers(token);
    }
    return issuerInfo;
  }

  private void loadUsers(String token) {
    // TODO mocking data with default instance until gRPC server is available
    //channel = ManagedChannelBuilder.forAddress("", 1).build()
    Optional<IssuerInfo> result = Optional.of(IssuerInfo.newBuilder()
        .getDefaultInstanceForType()); // new GrpcTask(new GetConnectionTokenInfoRunnable(), channel).doInBackground(token);
    result.ifPresent(info -> issuerInfo.setValue(info));
  }
}
