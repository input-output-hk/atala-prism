package io.iohk.cvp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import io.iohk.cvp.grpc.AsyncTaskResult;
import io.iohk.cvp.grpc.GetConnectionTokenInfoRunnable;
import io.iohk.cvp.grpc.GetMessagesRunnable;
import io.iohk.cvp.grpc.GrpcTask;
import io.iohk.prism.protos.ParticipantInfo;
import io.iohk.prism.protos.ReceivedMessage;

public class NewConnectionsViewModel extends CvpViewModel {

    private MutableLiveData<AsyncTaskResult<ParticipantInfo>> issuerInfo = new MutableLiveData<>();


    @Inject
    public NewConnectionsViewModel() {
    }

    public LiveData<AsyncTaskResult<ParticipantInfo>> getConnectionTokenInfo(String token) {
        new GrpcTask<>(new GetConnectionTokenInfoRunnable(issuerInfo), context).execute(null, token);
        return issuerInfo;
    }
}
