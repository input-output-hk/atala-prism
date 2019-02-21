package io.iohk.cef.frontend.services

import io.iohk.cef.ledger.{LedgerId, Transaction}
import io.iohk.cef.ledger.query.LedgerQuery
import io.iohk.cef.transactionservice.NodeTransactionService

trait LedgerService[State, Tx <: Transaction[State], Q <: LedgerQuery[State]] {

  def isLedgerSupported(ledgerId: LedgerId): Boolean = nodeTransactionService.supportedLedgerIds.contains(ledgerId)

  //FIXME: Hack. See CE-592
  def ledgerId: LedgerId = {
    nodeTransactionService.supportedLedgerIds.headOption
      .getOrElse(throw new IllegalStateException("No ledger Ids found in this service"))
  }

  protected def nodeTransactionService: NodeTransactionService[State, Tx, Q]
}
