package io.iohk.atala.prism.node.cardano.models
import derevo.derive
import enumeratum._
import tofu.logging.derivation.loggable
@derive(loggable)
case class CardanoWalletError(message: String, code: CardanoWalletErrorCode)
    extends RuntimeException(f"Status [${code.entryName}]. $message")

object CardanoWalletError {
  private val undefinedErrorDescription: String =
    "Undefined internal error in Cardano Wallet occurred."

  def fromString(message: String, errorCode: String): CardanoWalletError = {
    val errorCodeMaybe = CardanoWalletErrorCode.fromString(errorCode)
    errorCodeMaybe.fold {
      val errorDescription = f"$undefinedErrorDescription [$errorCode]"
      CardanoWalletError(
        errorDescription,
        CardanoWalletErrorCode.UndefinedCardanoWalletError
      )
    } { code =>
      CardanoWalletError(message, code)
    }
  }
}

@derive(loggable)
sealed trait CardanoWalletErrorCode extends EnumEntry.Snakecase
object CardanoWalletErrorCode extends Enum[CardanoWalletErrorCode] {
  val values: IndexedSeq[CardanoWalletErrorCode] = findValues

  // Errors 403 Forbidden
  case object InvalidWalletType extends CardanoWalletErrorCode
  case object AlreadyWithdrawing extends CardanoWalletErrorCode
  case object UtxoTooSmall extends CardanoWalletErrorCode
  case object CannotCoverFee extends CardanoWalletErrorCode
  case object NotEnoughMoney extends CardanoWalletErrorCode
  case object TransactionIsTooBig extends CardanoWalletErrorCode
  case object NoRootKey extends CardanoWalletErrorCode
  case object WrongEncryptionPassphrase extends CardanoWalletErrorCode
  case object TransactionAlreadyInLedger extends CardanoWalletErrorCode

  // Errors 404 Not Found
  case object NoSuchWallet extends CardanoWalletErrorCode
  case object NoSuchTransaction extends CardanoWalletErrorCode

  // Errors 406 Not Acceptable
  case object NotAcceptable extends CardanoWalletErrorCode

  // Errors 415 Not Supported Media Type
  case object UnsupportedMediaType extends CardanoWalletErrorCode

  // Errors 400 Undefined
  case object UndefinedCardanoWalletError extends CardanoWalletErrorCode

  def fromString(errorCode: String): Option[CardanoWalletErrorCode] =
    CardanoWalletErrorCode.withNameInsensitiveOption(errorCode)
}
