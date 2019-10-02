package io.iohk.node.bitcoin

import io.iohk.node.bitcoin.models.{Block, BlockError, Blockhash}
import io.iohk.node.utils.FutureEither

trait BitcoinClient {

  import BitcoinClient._

  // Bitcoin always has at least 1 block (the genesis one)
  def getLatestBlockhash: Result[Nothing, Blockhash]
  def getBlock(blockhash: Blockhash): Result[BlockError.NotFound, Block]
  def getBlockhash(height: Int): Result[BlockError.HeightNotFound, Blockhash]
}

object BitcoinClient {

  type Result[E, A] = FutureEither[E, A]

  case class Config(host: String, port: Int, username: String, password: String)

}
