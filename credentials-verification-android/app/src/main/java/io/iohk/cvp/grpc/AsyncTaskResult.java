package io.iohk.cvp.grpc;

public class AsyncTaskResult<A> {

  private A result;
  private Exception error;

  public A getResult() {
    return result;
  }

  public Exception getError() {
    return error;
  }

  public AsyncTaskResult(A result) {
    this.result = result;
  }

  public AsyncTaskResult(Exception error) {
    this.error = error;
  }
}
