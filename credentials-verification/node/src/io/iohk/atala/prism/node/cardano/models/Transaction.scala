package io.iohk.atala.prism.node.cardano.models

import io.iohk.atala.prism.models.TransactionId

case class Transaction(id: TransactionId, blockHash: BlockHash)
