package io.iohk.cef.query.ledger

import io.iohk.cef.ledger.chimeric.ChimericStateResult

package object chimeric {
  type ChimericPartition = ChimericStateResult
  type ChimericQueryEngine = LedgerQueryEngine[ChimericPartition]
  type ChimericQueryService = LedgerQueryService[ChimericPartition, ChimericQuery]
}
