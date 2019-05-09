package atala.obft.blockchain.storage

import atala.obft.blockchain.models.Block
import io.iohk.multicrypto._

class InMemoryBlockStorage[Tx] extends BlockStorage[Tx] {

  private[blockchain] var data: Map[Hash, Block[Tx]] = Map.empty

  override def get(id: Hash): Option[Block[Tx]] = data.get(id)

  override def put(id: Hash, block: Block[Tx]): Unit = data += (id -> block)

  override def remove(id: Hash): Unit = data -= id
}
