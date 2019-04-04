package obft.blockchain.storage

import obft.blockchain.AnyBlock

import io.iohk.multicrypto._

trait BlockStorage[Tx] {

  def get(id: Hash): Option[AnyBlock[Tx]]

  def put(id: Hash, block: AnyBlock[Tx]): Unit

  def remove(id: Hash): Unit
}

object BlockStorage {

  def apply[Tx](): BlockStorage[Tx] = new InMemoryBlockStorage[Tx]
}