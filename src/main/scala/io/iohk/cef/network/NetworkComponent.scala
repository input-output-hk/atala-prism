package io.iohk.cef.network
import io.iohk.cef.core.Envelope
import io.iohk.cef.ledger.{Block, Transaction}

trait NetworkComponent[F[_], State] {

  def disseminateTx(txEnvelope: Envelope[Transaction[State]]): F[Either[NetworkError, Unit]]

  def disseminateBlock[Header](block: Envelope[Block[State, Header, Transaction[State]]]): F[Unit]
}
