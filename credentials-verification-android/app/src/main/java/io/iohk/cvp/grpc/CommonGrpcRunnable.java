package io.iohk.cvp.grpc;

import androidx.lifecycle.MutableLiveData;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public abstract class CommonGrpcRunnable<A> implements GrpcRunnable<A> {

  protected final MutableLiveData<A> liveData;

  @Override
  public void onPostExecute(final A result) {
    liveData.postValue(result);
  }
}
