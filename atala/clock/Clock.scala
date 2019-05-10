package atala.clock

import scala.concurrent.duration._

case class TimeSlot(index: Int) extends Ordered[TimeSlot] {

  override def compare(that: TimeSlot) = this.index - that.index

  override def toString(): String =
    s"#$index"

  def leader(numServers: Int): Int =
    ((index - 1) % numServers) + 1

  def -(that: Int): TimeSlot =
    TimeSlot(index - that)

}

object TimeSlot {
  val zero: TimeSlot = TimeSlot(0)
}

object Clock {

  /**
    * NOTE: This allows all the servers to have the same initial time but leads to the first slot to be N instead of 0.
    *       Which shouldn't be a problem at all.
    */
  private val initialTime: Long = 0L

  def currentSlot(slotDuration: Long): TimeSlot = {
    val current = System.currentTimeMillis()
    val delta = current - initialTime
    TimeSlot((delta / slotDuration).toInt)
  }

  def currentSlot(slotDuration: Duration): TimeSlot =
    currentSlot(slotDuration.toMillis)
}
