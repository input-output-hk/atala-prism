package io.iohk.node.bitcoin.models

sealed trait SendDataTxError extends Product with Serializable
object SendDataTxError {
  case object CanNotPayTxFee extends SendDataTxError // There is not enough to pay the transaction fee
  case object InvalidRequest extends SendDataTxError // Some of the information provided to the Bitcoin API
  //                                                    has been rejected
  case object InvalidTransaction extends SendDataTxError
  case object RawTransactionAlreadyExists extends SendDataTxError
}
