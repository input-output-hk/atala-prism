package io.iohk.cef.consensus.raft.node
import java.util.{Timer, TimerTask}

import scala.concurrent.duration.Duration
import scala.util.Random

private[raft] class RaftTimer(minTimeout: Duration, maxTimeout: Duration)(timeoutFn: () => Unit) {

  val timer = new Timer()
  var currentTask: TimerTask = _

  schedule()

  def reset(): Unit = {
    currentTask.cancel()
    schedule()
  }

  private def schedule(): Unit = this.synchronized {
    currentTask = new TimerTask { override def run(): Unit = timeout() }
    timer.schedule(currentTask, nextRandom())
  }

  private def timeout(): Unit = {
    timeoutFn()
    schedule()
  }

  private def nextRandom(): Int = {
    val minTimeoutMillis = minTimeout.toMillis.toInt
    val maxTimeoutMillis = maxTimeout.toMillis.toInt
    minTimeoutMillis + Random.nextInt((maxTimeoutMillis - minTimeoutMillis) + 1)
  }
}
