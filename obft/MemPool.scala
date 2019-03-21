package obft

class MemPool[Tx](u: Int) {

  require(u > 0)

  private val data: Array[List[Tx]] = Array.fill(u)(Nil)
  private var current: Int = 0

  def advance(): Unit = {
    current = current.next
    data(current) = Nil
  }

  def add(tx: Tx): Unit =
    data(current) = tx :: data(current)

  def collect(): List[Tx] = {
    var r = List.empty[Tx]
    var i = current.next
    do {
      r = data(current).foldRight(r)((tx, a) => tx :: a)
      i = i.next
    } while (i != current)

    r.reverse // `reverse` puts the older Tx at index 0 and the newer at index r.length - 1
  }

  private implicit class IntOps(i: Int) {
    def next: Int = (i + 1) % u
  }
}

object MemPool {

  def apply[Tx](u: Int): MemPool[Tx] =
    new MemPool[Tx](u)

}
