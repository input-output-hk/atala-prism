package atala.obft.blockchain.models

/** Encapsulates the height of a block/blockchain
  *
  * @param int height represented
  * @note the internal representation may need to change to Long/BigInt
  */
class Height private (val int: Int) extends AnyVal {
  def toInt: Int = int
  def above: Height = new Height(int + 1)
  def below: Option[Height] = Height.from(int - 1)
}

object Height {
  val Zero = new Height(0)
  def from(int: Int): Option[Height] = {
    if (int < 0) None
    else Some(new Height(int))
  }
}
