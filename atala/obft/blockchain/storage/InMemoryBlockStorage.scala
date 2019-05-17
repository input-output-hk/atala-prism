package atala.obft.blockchain.storage

import atala.obft.blockchain.models.Block
import io.iohk.multicrypto._

class InMemoryBlockStorage[Tx] extends BlockStorage[Tx] {

  private[blockchain] var data: Map[Hash, Block[Tx]] = Map.empty

  override def get(id: Hash): Option[Block[Tx]] = data.get(id)

  override def getLatestBlock: Option[Block[Tx]] = {
    if (data.isEmpty) None
    else Some(data.maxBy(_._2.body.timeSlot.index)._2)
  }

  override def put(id: Hash, block: Block[Tx]): Unit = data += (id -> block)

  override def remove(id: Hash): Unit = data -= id

  override def getNumberOfBlocks(): Int = data.size

  override def update(removeList: List[Hash], addList: List[(Hash, Block[Tx])]): Unit = {
    for (hash <- removeList) remove(hash)
    for ((h, b) <- addList) put(h, b)
  }
}
