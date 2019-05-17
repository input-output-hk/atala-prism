package atala.obft.blockchain.storage

import atala.obft.blockchain.models._
import io.iohk.multicrypto._

/**
  * As the genesis block is expected to be hardcoded, there is no need to store it.
  */
trait BlockStorage[Tx] {

  def get(id: Hash): Option[Block[Tx]]

  def getNumberOfBlocks(): Int

  def getLatestBlock(): Option[Block[Tx]]

  def put(id: Hash, block: Block[Tx]): Unit

  def remove(id: Hash): Unit

  def update(removeList: List[Hash], addList: List[(Hash, Block[Tx])]): Unit
}
