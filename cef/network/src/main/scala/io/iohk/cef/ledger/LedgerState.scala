package io.iohk.cef.ledger

import akka.util.ByteString

trait LedgerState[K, V] {
  def hash: ByteString
  def get(key: K): Option[V]
  def contains(key: K): Boolean
}
