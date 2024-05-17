package io.iohk.atala.prism.node.models

import derevo.derive
import io.iohk.atala.prism.protos.common_models
import tofu.logging.derivation.loggable

@derive(loggable)
case class TransactionInfo(
    transactionId: TransactionId, // ID of the transaction
    ledger: Ledger, // Ledger the transaction was published to
    block: Option[BlockInfo] = None // Block the transaction is included in
) {
  def toProto: common_models.TransactionInfo =
    common_models.TransactionInfo(transactionId.toString, ledger.toProto, block.map(_.toProto))
}
