package io.iohk.cef.ledger.chimeric

import java.util.Base64

import akka.util.ByteString
import io.iohk.crypto._

object ChimericLedgerState {

  private val AddressPrefix = "ad="
  private val AddressNoncePrefix = "an="
  private val TxOutRefPrefix = "to="
  private val CurrencyPrefix = "cr="
  private val PrefixLength = 3
  private val Delimiter = "--"

  List(AddressPrefix, AddressNoncePrefix, TxOutRefPrefix, CurrencyPrefix).foreach { prefix =>
    require(prefix.size == PrefixLength)
  }

  private def addressEncoder(address: Address): String = Base64.getEncoder.encodeToString(address.toByteString.toArray)
  private def addressDecoder(addressStr: String): Address =
    SigningPublicKey
      .decodeFrom(ByteString(Base64.getDecoder.decode(addressStr)))
      .getOrElse(throw new IllegalArgumentException(s"Wrong adress format ${addressStr}"))

  def getAddressPartitionId(address: Address): String = s"$AddressPrefix${addressEncoder(address)}"
  def getAddressNoncePartitionId(address: Address): String = s"$AddressNoncePrefix${addressEncoder(address)}"
  def getUtxoPartitionId(txOutRef: TxOutRef): String = s"$TxOutRefPrefix${txOutRef.txId}${Delimiter}${txOutRef.index}"
  def getCurrencyPartitionId(currency: Currency): String = s"$CurrencyPrefix$currency"

  def toStateKey(partitionId: String): ChimericStateQuery = partitionId.take(PrefixLength) match {
    case AddressPrefix | AddressNoncePrefix =>
      val keyAsString = partitionId.drop(PrefixLength)
      val key = addressDecoder(keyAsString)
      AddressQuery(key)

    case TxOutRefPrefix =>
      val utxo = partitionId.drop(PrefixLength).split(Delimiter)
      if (utxo.size != 2) {
        throw new IllegalArgumentException(s"Wrong format for the utxo key ${partitionId}")
      } else {
        UtxoQuery(TxOutRef(utxo.head, utxo.tail.head.toInt))
      }
    case CurrencyPrefix =>
      CurrencyQuery(partitionId.drop(PrefixLength))
    case _ => throw new IllegalStateException(s"Invalid partition key prefix: ${partitionId.take(PrefixLength)}")
  }
}

//Values
sealed trait ChimericStateResult

case class CreateCurrencyResult(createCurrency: CreateCurrency) extends ChimericStateResult
case class UtxoResult(value: Value, signingPublicKey: SigningPublicKey) extends ChimericStateResult
case class AddressResult(value: Value) extends ChimericStateResult
case class NonceResult(nonce: Int) extends ChimericStateResult

//Keys
sealed trait ChimericStateQuery

case class AddressQuery(address: Address) extends ChimericStateQuery
case class AddressNonceQuery(address: Address) extends ChimericStateQuery
case class CurrencyQuery(currency: Currency) extends ChimericStateQuery
case class UtxoQuery(txOutRef: TxOutRef) extends ChimericStateQuery
