package io.iohk.cvp.viewmodel;

import android.content.Context;
import androidx.lifecycle.ViewModel;
import io.iohk.cvp.grpc.GrpcTask;
import java.util.ArrayList;
import java.util.List;
import lombok.Setter;

public class CvpViewModel extends ViewModel {

  @Setter
  protected Context context;

  protected List<GrpcTask> runningTasks = new ArrayList<>();

  public void stopTasks() {
    runningTasks.forEach(task -> task.cancel(true));
  }
}
