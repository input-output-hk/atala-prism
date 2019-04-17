package obft.mempool

import obft.logging._

class MemPool[Tx: Loggable](u: Int) extends AtalaLogging {

  require(u > 0)

  private val data: Array[List[Tx]] = Array.fill(u)(Nil)
  private var current: Int = 0

  /** Signals the mempool that the clock has advanced one time slot */
  def advance(): Unit = {
    current = current.next
    logger.trace("Advancing the slot in the mempool", "NewCurrentSlot" -> current)
    data(current) = Nil
  }

  /** Adds one transaction into the mempool */
  def add(tx: Tx): Unit = {
    logger.trace("Adding transaction to the MemPool", "tx" -> tx)
    data(current) = tx :: data(current)
  }

  /** Retrives all the transactions currently stored in the mempool */
  def collect(): List[Tx] = {
    logger.debug("Collecting all the transactions in the mempool")
    var r = List.empty[Tx]
    var i = current
    do {
      i = i.next
      r = data(i).foldRight(r)((tx, a) => tx :: a)
    } while (i != current)

    r.reverse // `reverse` puts the older Tx at index 0 and the newer at index r.length - 1
  }

  private implicit class IntOps(i: Int) {
    def next: Int = (i + 1) % u
  }
}

object MemPool {

  def apply[Tx](u: Int)(implicit loggable: Loggable[Tx] = defaultLoggable[Tx]): MemPool[Tx] =
    new MemPool[Tx](u)

  private def defaultLoggable[T]: Loggable[T] =
    new Loggable[T] {
      def log(t: T): String = t.toString
    }

}
