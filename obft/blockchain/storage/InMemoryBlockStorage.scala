package obft.blockchain.storage

import io.iohk.multicrypto._
import obft.blockchain.AnyBlock

class InMemoryBlockStorage[Tx] extends BlockStorage[Tx] {

  private[blockchain] var data: Map[Hash, AnyBlock[Tx]] = Map.empty

  override def get(id: Hash): Option[AnyBlock[Tx]] = data.get(id)

  override def put(id: Hash, block: AnyBlock[Tx]): Unit = data += (id -> block)

  override def remove(id: Hash): Unit = data -= id
}
