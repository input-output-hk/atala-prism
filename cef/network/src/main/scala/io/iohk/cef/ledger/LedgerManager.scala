package io.iohk.cef.ledger

import scala.concurrent.Future

trait LedgerManager {

  type Transaction

  val LedgerId: String

  def apply(transaction: Transaction): Future[Unit]
}
