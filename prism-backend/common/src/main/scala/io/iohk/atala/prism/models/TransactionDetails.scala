package io.iohk.atala.prism.models

import derevo.derive
import tofu.logging.derivation.loggable

@derive(loggable)
case class TransactionDetails(id: TransactionId, status: TransactionStatus)
