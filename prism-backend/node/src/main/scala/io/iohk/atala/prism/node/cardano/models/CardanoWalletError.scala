package io.iohk.atala.prism.node.cardano.models

private object CardanoWalletErrorsCollector {
  private val codeToErrorMap =
    collection.mutable.Map.empty[String, Class[_ <: CardanoWalletError]]

  def addError(code: String, errorClass: Class[_ <: CardanoWalletError]): Unit =
    codeToErrorMap.update(code, errorClass)

  def codeToError(code: String): Option[Class[_ <: CardanoWalletError]] =
    codeToErrorMap.get(code)
}

sealed trait CardanoWalletError {
  def code: String
  def message: String

  CardanoWalletErrorsCollector.addError(code, getClass)
}

object CardanoWalletError {
  // Errors 403 Forbidden
  case class InvalidWalletType(message: String, code: String = "invalid_wallet_type") extends CardanoWalletError
  case class AlreadyWithdrawing(message: String, code: String = "already_withdrawing") extends CardanoWalletError
  case class UtxoTooSmall(message: String, code: String = "utxo_too_small") extends CardanoWalletError
  case class CannotCoverFee(message: String, code: String = "cannot_cover_fee") extends CardanoWalletError
  case class NotEnoughMoney(message: String, code: String = "not_enough_money") extends CardanoWalletError
  case class TransactionIsTooBig(message: String, code: String = "transaction_is_too_big") extends CardanoWalletError
  case class NoRootKey(message: String, code: String = "no_root_key") extends CardanoWalletError
  case class WrongEncryptionPassphrase(message: String, code: String = "wrong_encryption_passphrase")
      extends CardanoWalletError
  case class TransactionAlreadyInLedger(message: String, code: String = "transaction_already_in_ledger")
      extends CardanoWalletError

  // Errors 404 Not Found
  case class NoSuchWallet(message: String, code: String = "no_such_wallet") extends CardanoWalletError
  case class NoSuchTransaction(message: String, code: String = "no_such_transaction") extends CardanoWalletError

  // Errors 406 Not Acceptable
  case class NotAcceptable(message: String, code: String = "not_acceptable") extends CardanoWalletError

  // Errors 415 Not Supported Media Type
  case class UnsupportedMediaType(message: String, code: String = "unsupported_media_type") extends CardanoWalletError

  // Errors 400 Undefined
  case class UndefinedCardanoWalletError(
      message: String = "Undefined internal error in Cardano Wallet occurred.",
      code: String = "undefined_cardano_wallet_error"
  ) extends CardanoWalletError

  def errorInstance(message: String, code: String): CardanoWalletError = {
    CardanoWalletErrorsCollector
      .codeToError(code)
      .getOrElse(classOf[UndefinedCardanoWalletError])
      .getConstructor(classOf[String], classOf[String])
      .newInstance(message, code)
  }
}
