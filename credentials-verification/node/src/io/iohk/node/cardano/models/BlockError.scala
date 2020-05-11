package io.iohk.node.cardano.models

sealed trait BlockError extends Product with Serializable
object BlockError {
  final case class NotFound(blockHash: BlockHash) extends BlockError
  final case object NoneAvailable extends BlockError
}
