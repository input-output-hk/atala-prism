package io.iohk.cvp.viewmodel;

import androidx.lifecycle.ViewModel;
import io.iohk.cvp.grpc.GrpcTask;
import java.util.ArrayList;
import java.util.List;

public class CvpViewModel extends ViewModel {

  protected List<GrpcTask> runningTasks = new ArrayList<>();

  public void stopTasks() {
    runningTasks.forEach(task -> task.cancel(true));
  }
}
