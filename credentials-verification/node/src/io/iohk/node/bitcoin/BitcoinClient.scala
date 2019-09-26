package io.iohk.node.bitcoin

import scala.concurrent.Future

trait BitcoinClient {

  import BitcoinClient._

  // Bitcoin always has at least 1 block (the genesis one)
  def getLatestBlockhash: Result[Nothing, Blockhash]
  def getBlock(blockhash: Blockhash): Result[BlockError.NotFound, Block]
  def getBlockhash(height: Int): Result[BlockError.HeightNotFound, Blockhash]
}

object BitcoinClient {

  type Result[E, A] = Future[Either[E, A]]

  sealed trait BlockError extends Product with Serializable
  object BlockError {

    final case class NotFound(blockhash: Blockhash) extends BlockError
    final case class HeightNotFound(height: Int) extends BlockError
  }

  case class Config(host: String, port: Int, username: String, password: String)

}
