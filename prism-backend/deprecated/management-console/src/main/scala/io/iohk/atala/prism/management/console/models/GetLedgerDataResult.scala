package io.iohk.atala.prism.management.console.models

import io.iohk.atala.prism.protos.node_models.LedgerData

case class GetLedgerDataResult(
    publicationData: Option[LedgerData],
    revocationData: Option[LedgerData],
    credentialRevocationData: Option[LedgerData]
)
