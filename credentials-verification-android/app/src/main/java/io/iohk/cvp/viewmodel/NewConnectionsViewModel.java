package io.iohk.cvp.viewmodel;

import android.os.AsyncTask;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import javax.inject.Inject;

import io.iohk.cvp.grpc.AsyncTaskResult;
import io.iohk.cvp.grpc.GetConnectionTokenInfoRunnable;
import io.iohk.cvp.grpc.GetMessagesRunnable;
import io.iohk.cvp.grpc.GrpcTask;
import io.iohk.cvp.grpc.ParticipantInfoResponse;
import io.iohk.prism.protos.ParticipantInfo;

public class NewConnectionsViewModel extends CvpViewModel {

    private MutableLiveData<AsyncTaskResult<ParticipantInfoResponse>> issuerInfo = new MutableLiveData<>();

    @Inject
    public NewConnectionsViewModel() {
    }

    public LiveData<AsyncTaskResult<ParticipantInfoResponse>> getConnectionTokenInfoLiveData() {
        return issuerInfo;
    }

    public void getConnectionTokenInfo(String token) {
        GrpcTask task = new GrpcTask<>(new GetConnectionTokenInfoRunnable(issuerInfo), context);
        task.execute(null,token);
        runningTasks.add(task);
    }
}
