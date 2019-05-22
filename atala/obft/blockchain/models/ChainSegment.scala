package atala.obft.blockchain.models

import atala.clock.TimeSlot

class ChainSegment[Tx] private (val blocks: List[Block[Tx]]) extends AnyVal {
  def mostRecentToOldest: List[Block[Tx]] = blocks.sortBy(_.body.timeSlot)(Ordering[TimeSlot].reverse)
  def oldestToMostRecent: List[Block[Tx]] = blocks.sortBy(_.body.timeSlot)
  def oldestBlock: Option[Block[Tx]] = if (blocks.isEmpty) None else Some(blocks.minBy(_.body.timeSlot))
  def mostRecentBlock: Option[Block[Tx]] = if (blocks.isEmpty) None else Some(blocks.maxBy(_.body.timeSlot))
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
