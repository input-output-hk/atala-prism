package io.iohk.cef.ledger.persistence

import scala.language.higherKinds

trait LedgerStorage[F[_], State <: LedgerState] {

  def push(block: Block[State]): F[Unit]

  def pop(): F[Block[State]]

  def peek(): F[Block[State]]
}
