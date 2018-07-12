package io.iohk.cef.ledger.storage

import akka.util.ByteString
import io.iohk.cef.ledger.{Block, BlockHeader, LedgerState, Transaction}

import scala.language.higherKinds

trait LedgerStateStorage[F[_], State <: LedgerState[Key, _], Key, Header <: BlockHeader, Tx <: Transaction[State, Key]] {

  def slice(keys: Set[Key]): State

  def update[B <: Block[State, Key, Header, Tx]](previousHash: ByteString, newState: State): F[Unit]
}
