package io.iohk.atala.prism.node.models

import derevo.derive
import enumeratum.{CirceEnum, Enum}
import enumeratum.EnumEntry.Snakecase
import io.iohk.atala.prism.node.cardano.models.Lovelace
import tofu.logging.{DictLoggable, LogRenderer}
import tofu.logging.derivation.loggable

@derive(loggable)
final case class WalletDetails(balance: Balance, state: WalletState)

final case class Balance(available: Lovelace)
object Balance {
  implicit val balanceLoggable: DictLoggable[Balance] =
    new DictLoggable[Balance] {
      override def fields[I, V, R, S](a: Balance, i: I)(implicit
          r: LogRenderer[I, V, R, S]
      ): R =
        r.addString("Balance", a.available.toString(), i)

      override def logShow(a: Balance): String = s"Balance{${a.available}}"
    }
}

final case class WalletState(status: WalletStatus)

object WalletState {
  implicit val walletStateLoggable: DictLoggable[WalletState] =
    new DictLoggable[WalletState] {
      override def fields[I, V, R, S](a: WalletState, i: I)(implicit
          r: LogRenderer[I, V, R, S]
      ): R =
        r.addString("WalletState", a.status.entryName, i)

      override def logShow(a: WalletState): String =
        s"WalletState{${a.status.entryName}}"
    }
}

sealed trait WalletStatus extends Snakecase

object WalletStatus extends Enum[WalletStatus] with CirceEnum[WalletStatus] {
  lazy val values: IndexedSeq[WalletStatus] = findValues
  final case object Ready extends WalletStatus
  final case object Syncing extends WalletStatus
  final case object NotResponding extends WalletStatus
}
