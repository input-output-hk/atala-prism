package io.iohk.cef.utils.concurrent

trait Cancellable {
  def cancel(): Unit
}
