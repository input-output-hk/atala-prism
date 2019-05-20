package atala.obft.blockchain.models

class ChainSegment[Tx] private (val blocks: List[Block[Tx]]) extends AnyVal {
  def mostRecentToOldest: List[Block[Tx]] = blocks.sortBy(-_.body.timeSlot.toInt)
  def oldestToMostRecent: List[Block[Tx]] = blocks.sortBy(_.body.timeSlot.toInt)
  def oldestBlock: Option[Block[Tx]] = if (blocks.isEmpty) None else Some(blocks.minBy(_.body.timeSlot.toInt))
  def mostRecentBlock: Option[Block[Tx]] = if (blocks.isEmpty) None else Some(blocks.maxBy(_.body.timeSlot.toInt))
  def length: Int = blocks.length
}

object ChainSegment {

  def empty[Tx]: ChainSegment[Tx] = ChainSegment[Tx](Nil)

  def apply[Tx](blocks: List[Block[Tx]]): ChainSegment[Tx] = new ChainSegment[Tx](
    blocks
  )

  def apply[Tx](blocks: Block[Tx]*): ChainSegment[Tx] = ChainSegment[Tx](
    blocks.toList
  )
}
