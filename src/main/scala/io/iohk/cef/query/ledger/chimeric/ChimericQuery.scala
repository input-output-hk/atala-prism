package io.iohk.cef.query.chimeric

import io.iohk.cef.ledger.chimeric._
import io.iohk.cef.query.ledger.LedgerQuery

sealed trait ChimericQuery extends LedgerQuery[ChimericPartition]

case class CreatedCurrencyQuery(currency: Currency) extends ChimericQuery {
  type Response = Option[CreateCurrency]
  override protected def perform(queryEngine: ChimericQueryEngine): Response =
    queryEngine.get(ChimericLedgerState.getCurrencyPartitionId(currency)) match {
      case Some(CreateCurrencyResult(currency)) => Some(currency)
      case _ => None
    }
}

case class UtxoBalanceQuery(txOutRef: TxOutRef) extends ChimericQuery {
  type Response = Option[UtxoResult]
  override protected def perform(queryEngine: ChimericQueryEngine): Response =
    queryEngine.get(ChimericLedgerState.getUtxoPartitionId(txOutRef)) match {
      case Some(res: UtxoResult) => Some(res)
      case _ => None
    }
}

case class AddressBalanceQuery(address: Address) extends ChimericQuery {
  type Response = Option[AddressResult]
  override protected def perform(queryEngine: ChimericQueryEngine): Response =
    queryEngine.get(ChimericLedgerState.getAddressPartitionId(address)) match {
      case Some(addr: AddressResult) => Some(addr)
      case _ => None
    }
}

case class AddressNonceQuery(address: Address) extends ChimericQuery {
  type Response = Option[Int]
  override protected def perform(queryEngine: ChimericQueryEngine): Response =
    queryEngine.get(ChimericLedgerState.getAddressPartitionId(address)) match {
      case Some(NonceResult(nonce)) => Some(nonce)
      case _ => None
    }
}
