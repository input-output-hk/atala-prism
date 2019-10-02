package io.iohk.node.bitcoin.models

sealed trait BlockError extends Product with Serializable
object BlockError {

  final case class NotFound(blockhash: Blockhash) extends BlockError
  final case class HeightNotFound(height: Int) extends BlockError
  final case object NoOneAvailable extends BlockError
}
