package io.iohk.atala.prism.node.cardano.models

import derevo.derive
import tofu.logging.derivation.loggable

@derive(loggable)
sealed trait BlockError extends Product with Serializable
object BlockError {
  @derive(loggable)
  final case class NotFound(blockNo: Int) extends BlockError
  @derive(loggable)
  final case object NoneAvailable extends BlockError
}
