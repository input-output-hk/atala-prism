package obft.blockchain

import obft.fakes._

class BlockStorage[Tx]() {

  private[blockchain] var data: Map[Hash[AnyBlock[Tx]], AnyBlock[Tx]] = Map.empty

  def get(id: Hash[AnyBlock[Tx]]): Option[AnyBlock[Tx]] =
    data.get(id)

  def put(id: Hash[AnyBlock[Tx]], block: AnyBlock[Tx]): Unit =
    data += (id -> block)

  def remove(id: Hash[AnyBlock[Tx]]): Unit =
    data -= id

}

object BlockStorage {
  def apply[Tx](): BlockStorage[Tx] =
    new BlockStorage[Tx]()
}
