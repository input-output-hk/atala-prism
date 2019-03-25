package obft.clock

import scala.concurrent.duration._

case class TimeSlot(private val index: Int) extends Ordered[TimeSlot] {

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
  private val initialTime: Long =
    Clock.synchronized {
      System.currentTimeMillis()
    }

  def currentSlot(slotDuration: Long): TimeSlot =
    Clock.synchronized {
      val current = System.currentTimeMillis()
      val delta = current - initialTime
      TimeSlot((delta / slotDuration).toInt)
    }

  def currentSlot(slotDuration: Duration): TimeSlot =
    currentSlot(slotDuration.toMillis)
}
