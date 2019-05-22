package atala.clock

import io.iohk.decco.Codec
import io.iohk.decco.auto._

import scala.concurrent.duration._

class TimeSlot private (val index: Long) extends AnyVal with Ordered[TimeSlot] {

  def toLong: Long = index

  def next: TimeSlot = new TimeSlot(index + 1)

  override def compare(that: TimeSlot) =
    if (this.index < that.index) -1
    else if (this.index > that.index) 1
    else 0

  override def toString(): String =
    s"#$index"

  def leader(numServers: Int): Int = {
    (((index - 1 + numServers) % numServers) + 1).toInt
  }

  def -(that: Long): Option[TimeSlot] =
    TimeSlot.from(index - that)

}

object TimeSlot {
  val zero = new TimeSlot(0)
  def from(x: Long): Option[TimeSlot] =
    if (x < 0) None else Some(new TimeSlot(x))

  implicit val timeSlotCodec: Codec[TimeSlot] = Codec[Long].map(x => TimeSlot.from(x).get, _.toLong)
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
    TimeSlot.from(delta / slotDuration).getOrElse {
      throw new RuntimeException(s"FATAL: Invalid time slot found for delta = $delta and slotDuration = $slotDuration")
    }
  }

  def currentSlot(slotDuration: Duration): TimeSlot =
    currentSlot(slotDuration.toMillis)
}
