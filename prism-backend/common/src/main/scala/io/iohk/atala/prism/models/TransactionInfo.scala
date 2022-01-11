package io.iohk.atala.prism.models

import derevo.derive
import tofu.logging.derivation.loggable

@derive(loggable)
case class TransactionInfo(
    transactionId: TransactionId, // ID of the transaction
    ledger: Ledger, // Ledger the transaction was published to
    block: Option[BlockInfo] = None // Block the transaction is included in
)
