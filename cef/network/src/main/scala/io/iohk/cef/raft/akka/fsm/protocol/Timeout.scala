package io.iohk.cef.raft.akka.fsm.protocol



import scala.concurrent.duration.{DurationInt, FiniteDuration}

/**
  * Raft uses random election timeouts, this class help us to generate those
  * random timeouts based on a time interval.
  *
  * The timeouts are considered in milliseconds, hence, there should be at least
  * 1 ms of difference between minTimeout and maxTimeout, where minTimeout is
  * less than maxTimeout.
  *
  * TODO: Consider renaming to something like ElectionTimeoutGenerator.
  *
  * @param minTimeout the minimum allowed timeout
  * @param maxTimeout the maximun allowed timeout
  */
class Timeout private (minTimeout: FiniteDuration, maxTimeout: FiniteDuration) {

  /**
    * Generates a random timeout
    *
    * @return a random timeout in the [minTimeout, maxTimeout] interval
    */
  def randomTimeout(): FiniteDuration = {
    val diff = (maxTimeout.toMillis - minTimeout.toMillis) + 1

    // the difference of the timeouts should fit in an integer, it isn't practical to have a wide range.
    val shiftBy = scala.util.Random.nextInt(diff.toInt)

    minTimeout.plus(shiftBy.millis)
  }
}

object Timeout {
  //TODO  configurable
  val DefaultElectionTimeout = new Timeout(150.millis, 300.millis)

}