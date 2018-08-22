package io.iohk.cef.network
import io.iohk.cef.core.Envelope
import io.iohk.cef.ledger.{Block, Transaction}

trait NetworkComponent[F[_], State] {

  def disseminateTransaction(txEnvelope: Envelope[Transaction[State]]): F[Either[NetworkError, Unit]]

  def disseminateBlock[Header](block: Envelope[Block[State, Header, Transaction[State]]]): F[Either[NetworkError, Unit]]

  def receiveTransaction(txEnvelope: Envelope[Transaction[State]]): F[Either[NetworkError, Transaction[State]]]

  def receiveBlock[Header](blockEnvelope: Envelope[Block[State, Header, Transaction[State]]]): F[Either[NetworkError, Block[State, Header, Transaction[State]]]]
}
