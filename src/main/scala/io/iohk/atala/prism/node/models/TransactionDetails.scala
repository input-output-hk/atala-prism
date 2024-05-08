package io.iohk.atala.prism.node.models

import derevo.derive
import tofu.logging.derivation.loggable

@derive(loggable)
case class TransactionDetails(id: TransactionId, status: TransactionStatus)
