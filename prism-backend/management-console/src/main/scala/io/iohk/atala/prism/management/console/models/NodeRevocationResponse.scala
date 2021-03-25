package io.iohk.atala.prism.management.console.models

import io.iohk.atala.prism.models.TransactionId
import io.iohk.atala.prism.protos.common_models

case class NodeRevocationResponse(
    transactionInfo: common_models.TransactionInfo,
    transactionId: TransactionId
)
