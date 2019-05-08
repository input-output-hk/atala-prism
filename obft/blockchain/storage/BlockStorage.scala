package obft.blockchain.storage

import io.iohk.multicrypto._
import obft.blockchain.models.AnyBlock

trait BlockStorage[Tx] {

  def get(id: Hash): Option[AnyBlock[Tx]]

  def put(id: Hash, block: AnyBlock[Tx]): Unit

  def remove(id: Hash): Unit
}
