package io.iohk.cef.ledger.persistence

trait LedgerState {
  def commit(): Unit
  def rollback(): Unit
  def begin(): Unit
}
