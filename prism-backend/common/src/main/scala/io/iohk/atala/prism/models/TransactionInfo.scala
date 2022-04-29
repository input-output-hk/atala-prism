package io.iohk.atala.prism.models

import derevo.derive
import tofu.logging.derivation.loggable
import io.iohk.atala.prism.protos.common_models

@derive(loggable)
case class TransactionInfo(
    transactionId: TransactionId, // ID of the transaction
    ledger: Ledger, // Ledger the transaction was published to
    block: Option[BlockInfo] = None // Block the transaction is included in
) {
  def toProto: common_models.TransactionInfo =
    common_models.TransactionInfo(transactionId.toString, ledger.toProto, block.map(_.toProto))
}
