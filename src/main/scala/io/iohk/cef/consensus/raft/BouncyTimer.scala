package io.iohk.cef.consensus.raft
import java.util.{Timer, TimerTask}

import io.iohk.cef.consensus.raft.RaftConsensus.RaftTimer

import scala.concurrent.duration.Duration
import scala.util.Random

class BouncyTimer(minTimeout: Duration, maxTimeout: Duration)(timeoutFn: () => Unit) extends RaftTimer {

  val timer = new Timer()

  override def reset(): Unit = {
    timer.cancel()
    schedule()
  }

  private def schedule(): Unit = {
    timer.schedule(new TimerTask { override def run(): Unit = timeout() }, nextRandom())
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
