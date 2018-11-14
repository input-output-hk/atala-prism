package io.iohk.cef.consensus.raft.node

import io.iohk.cef.utils.concurrent.{CancellableFuture, Timer}

import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.util.Random

private[raft] class RaftTimer(minTimeout: Duration, maxTimeout: Duration)(timeoutFn: () => Unit) {

  private var currentTask: CancellableFuture[Unit] = _

  schedule()

  def reset(): Unit = {
    currentTask.cancel()
    schedule()
  }

  def schedule(): Unit = this.synchronized {
    currentTask = Timer.schedule(nextRandom() millis)(timeout())
  }

  def timeout(): Unit = {
    timeoutFn()
    schedule()
  }

  private def nextRandom(): Int = {
    val minTimeoutMillis = minTimeout.toMillis.toInt
    val maxTimeoutMillis = maxTimeout.toMillis.toInt
    minTimeoutMillis + Random.nextInt((maxTimeoutMillis - minTimeoutMillis) + 1)
  }
}
