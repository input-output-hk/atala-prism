package io.iohk.atala.prism.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import io.iohk.atala.prism.app.grpc.AsyncTaskResult;
import io.iohk.atala.prism.app.grpc.GetBraintreePaymentsConfigRunnable;
import io.iohk.atala.prism.app.grpc.GrpcTask;

import javax.inject.Inject;

public class AcceptConnectionViewModel extends CvpViewModel {

    private MutableLiveData<AsyncTaskResult<String>> tokenizationKey = new MutableLiveData<>();

    @Inject
    public AcceptConnectionViewModel() {
    }

    public LiveData<AsyncTaskResult<String>> getTokenizationKey() {
        new GrpcTask<>(new GetBraintreePaymentsConfigRunnable(tokenizationKey), context).execute();
        return tokenizationKey;
    }
}
