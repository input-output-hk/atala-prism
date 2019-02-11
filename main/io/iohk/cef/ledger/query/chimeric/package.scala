package io.iohk.cef.ledger.query

import io.iohk.cef.ledger.chimeric.ChimericStateResult

package object chimeric {
  type ChimericPartition = ChimericStateResult
  type ChimericQueryEngine = LedgerQueryEngine[ChimericPartition]
  type ChimericQueryService = LedgerQueryService[ChimericPartition, ChimericQuery]
}
