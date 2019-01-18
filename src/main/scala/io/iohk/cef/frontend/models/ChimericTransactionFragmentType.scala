package io.iohk.cef.frontend.models

import enumeratum._
import io.iohk.cef.ledger.chimeric._
import io.iohk.cef.ledger.chimeric

sealed abstract class ChimericTransactionFragmentType extends EnumEntry
sealed abstract class SignableChimericTransactionFragmentType extends ChimericTransactionFragmentType
sealed abstract class NonSignableChimericTransactionFragmentType extends ChimericTransactionFragmentType

object ChimericTransactionFragmentType extends Enum[ChimericTransactionFragmentType] {

  val values = findValues

  case object Withdrawal extends SignableChimericTransactionFragmentType
  case object Input extends SignableChimericTransactionFragmentType

  case object Mint extends NonSignableChimericTransactionFragmentType
  case object Fee extends NonSignableChimericTransactionFragmentType
  case object Output extends NonSignableChimericTransactionFragmentType
  case object Deposit extends NonSignableChimericTransactionFragmentType
  case object CreateCurrency extends NonSignableChimericTransactionFragmentType
  case object SignatureTxFragment extends NonSignableChimericTransactionFragmentType

  def of(it: ChimericTxFragment): ChimericTransactionFragmentType =
    it match {
      case s: SignableChimericTxFragment => SignableChimericTransactionFragmentType.of(s)
      case n: NonSignableChimericTxFragment => NonSignableChimericTransactionFragmentType.of(n)
    }
}

object SignableChimericTransactionFragmentType {
  def of(it: SignableChimericTxFragment): SignableChimericTransactionFragmentType =
    it match {
      case _: chimeric.Withdrawal => ChimericTransactionFragmentType.Withdrawal
      case _: chimeric.Input => ChimericTransactionFragmentType.Input
    }
}

object NonSignableChimericTransactionFragmentType {
  def of(it: NonSignableChimericTxFragment): NonSignableChimericTransactionFragmentType =
    it match {
      case _: chimeric.Mint => ChimericTransactionFragmentType.Mint
      case _: chimeric.Fee => ChimericTransactionFragmentType.Fee
      case _: chimeric.Output => ChimericTransactionFragmentType.Output
      case _: chimeric.Deposit => ChimericTransactionFragmentType.Deposit
      case _: chimeric.CreateCurrency => ChimericTransactionFragmentType.CreateCurrency
      case _: chimeric.SignatureTxFragment => ChimericTransactionFragmentType.SignatureTxFragment
    }
}
