package obft.blockchain

import io.iohk.multicrypto._

trait BlockStorage[Tx] {

  def get(id: Hash): Option[AnyBlock[Tx]]

  def put(id: Hash, block: AnyBlock[Tx]): Unit

  def remove(id: Hash): Unit
}

object BlockStorage {

  def apply[Tx](): BlockStorage[Tx] = new InMemory[Tx]

  class InMemory[Tx] extends BlockStorage[Tx] {

    private[blockchain] var data: Map[Hash, AnyBlock[Tx]] = Map.empty

    override def get(id: Hash): Option[AnyBlock[Tx]] = data.get(id)

    override def put(id: Hash, block: AnyBlock[Tx]): Unit = data += (id -> block)

    override def remove(id: Hash): Unit = data -= id
  }
}
