package io.iohk.cef.ledger.storage

import akka.util.ByteString
import io.iohk.cef.ledger.{Block, LedgerState}

import scala.language.higherKinds
import scala.util.Try

trait LedgerStateStorage[F[_], State <: LedgerState[Key, _], Key] {

  def slice(keys: Set[Key]): Try[State]

  def update[B <: Block[State, Key]](previousHash: ByteString, newState: State): F[Unit]
}
