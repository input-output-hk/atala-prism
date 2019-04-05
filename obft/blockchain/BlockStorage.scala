package obft.blockchain

import io.iohk.multicrypto._

class BlockStorage[Tx]() {

  private[blockchain] var data: Map[Hash, AnyBlock[Tx]] = Map.empty

  def get(id: Hash): Option[AnyBlock[Tx]] =
    data.get(id)

  def put(id: Hash, block: AnyBlock[Tx]): Unit =
    data += (id -> block)

  def remove(id: Hash): Unit =
    data -= id

}

object BlockStorage {
  def apply[Tx](): BlockStorage[Tx] =
    new BlockStorage[Tx]()
}
