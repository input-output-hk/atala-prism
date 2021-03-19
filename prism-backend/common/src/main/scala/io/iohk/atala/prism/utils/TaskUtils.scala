package io.iohk.atala.prism.utils

import monix.eval.Task

import scala.concurrent.duration.FiniteDuration

object TaskUtils {

  def retry[A](task: => Task[A], maxRetries: Int, retryWaitTime: FiniteDuration): Task[A] = {
    task.onErrorRestartLoop(maxRetries) { (err, retries, retry) =>
      if (retries > 0)
        retry(retries - 1).delayExecution(retryWaitTime)
      else
        Task.raiseError(new RuntimeException(s"Cannot complete task despite $maxRetries retries: " + err.getMessage))
    }
  }
}
