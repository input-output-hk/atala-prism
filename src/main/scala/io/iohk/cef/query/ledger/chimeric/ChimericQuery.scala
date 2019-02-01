package io.iohk.cef.query.ledger.chimeric

import io.iohk.cef.ledger.chimeric._
import io.iohk.cef.query.Query
import io.iohk.cef.query.ledger.{LedgerQuery, LedgerQueryEngine}
import io.iohk.crypto._

sealed trait ChimericQuery extends LedgerQuery[ChimericPartition]

object ChimericQuery {

  case class CreatedCurrency(currency: Currency) extends ChimericQuery {
    override type Response = Option[CurrencyQuery]

    override protected def perform(queryEngine: ChimericQueryEngine): Response =
      queryEngine.get(ChimericLedgerState.getCurrencyPartitionId(currency)) match {
        case Some(CreateCurrencyResult(CreateCurrency(currency))) => Some(CurrencyQuery(currency))
        case _ => None
      }
  }

  case class UtxoBalance(txOutRef: TxOutRef) extends ChimericQuery {
    override type Response = Option[UtxoResult]

    override protected def perform(queryEngine: ChimericQueryEngine): Response =
      queryEngine.get(ChimericLedgerState.getUtxoPartitionId(txOutRef)) match {
        case Some(res: UtxoResult) => Some(res)
        case _ => None
      }
  }

  case class AddressBalance(address: Address) extends ChimericQuery {
    override type Response = Option[AddressResult]

    override protected def perform(queryEngine: ChimericQueryEngine): Response =
      queryEngine.get(ChimericLedgerState.getAddressPartitionId(address)) match {
        case Some(addr: AddressResult) => Some(addr)
        case _ => None
      }
  }

  case class AddressNonce(address: Address) extends ChimericQuery {
    override type Response = Option[NonceResult]

    override protected def perform(queryEngine: ChimericQueryEngine): Response =
      queryEngine.get(ChimericLedgerState.getAddressNoncePartitionId(address)) match {
        case Some(nonce: NonceResult) => Some(nonce)
        case _ => None
      }
  }

  case object AllCurrencies extends ChimericQuery {
    override type Response = Set[Currency]

    override protected def perform(queryEngine: LedgerQueryEngine[ChimericPartition]): Set[Currency] = {
      queryEngine.keys().map(ChimericLedgerState.toStateKey).collect {
        case c: CurrencyQuery => c.currency
      }
    }
  }

  case class UtxosByPublicKey(publicKey: SigningPublicKey) extends ChimericQuery {
    override type Response = Set[TxOutRef]

    override protected def perform(queryEngine: LedgerQueryEngine[ChimericPartition]): Set[TxOutRef] = {
      val relevantKeys = queryEngine.keys().map(ChimericLedgerState.toStateKey).collect { case u: UtxoQuery => u }
      val balances = relevantKeys.view.map { key =>
        val balanceOpt = Query.performer(UtxoBalance(key.txOutRef), queryEngine)
        balanceOpt.map(balance => (key.txOutRef, balance))
      }
      balances.collect {
        case Some((txOutRef, UtxoResult(_, pk))) if publicKey == pk => txOutRef
      }.toSet
    }
  }

}
