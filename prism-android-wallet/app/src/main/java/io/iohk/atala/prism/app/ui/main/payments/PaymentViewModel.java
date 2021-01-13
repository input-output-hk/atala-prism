package io.iohk.atala.prism.app.ui.main.payments;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import io.iohk.atala.prism.app.grpc.AsyncTaskResult;
import io.iohk.atala.prism.app.grpc.GetPaymentsRunnable;
import io.iohk.atala.prism.app.grpc.GrpcTask;
import io.iohk.atala.prism.protos.Payment;
import lombok.Setter;

public class PaymentViewModel extends ViewModel {

    @Setter
    protected Context context;

    protected List<GrpcTask> runningTasks = new ArrayList<>();

    private MutableLiveData<AsyncTaskResult<List<Payment>>> payments = new MutableLiveData<>();

    @Inject
    public PaymentViewModel() {

    }

    public LiveData<AsyncTaskResult<List<Payment>>> getPayments(Set<String> userIds) {
        GrpcTask task = new GrpcTask<>(new GetPaymentsRunnable(payments), context);
        task.execute(userIds);
        runningTasks.add(task);
        return payments;
    }

    public void clearPayments() {
        payments.setValue(new AsyncTaskResult<>(new ArrayList<>()));
    }

    public enum PaymentState {
        CHARGED,
        FAILED
    }
}

