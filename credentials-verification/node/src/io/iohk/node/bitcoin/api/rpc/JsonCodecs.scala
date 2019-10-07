package io.iohk.node.bitcoin
package api
package rpc

import io.circe._
import io.iohk.node.bitcoin.models._
import shapeless.tag.@@
import shapeless.tag
import scala.util.{Failure, Success, Try}
import io.circe.generic.semiauto._
import ApiModel._
import BitcoinApiClient.BitcoinError
import BitcoinApiClient.ErrorResponse

object JsonCodecs {

  private def taggedCodec[A, T](implicit d: Decoder[A], e: Encoder[A]): Codec[A @@ T] =
    Codec.from(
      d.map { a =>
        tag[T][A](a)
      },
      e.contramap((i: A @@ T) => (i: A))
    )

  implicit val bitcoinErrorDecoder: Codec[BitcoinError] =
    deriveCodec[BitcoinError]
  implicit val errorResponse: Codec[ErrorResponse] =
    deriveCodec[ErrorResponse]

  implicit val btcCodec: Codec[Btc] = taggedCodec[BigDecimal, modeltags.BtcTag]
  implicit val voutCodec: Codec[Vout] = taggedCodec[Int, modeltags.VoutTag]
  implicit val addressCodec: Codec[Address] = taggedCodec[String, modeltags.AddressTag]
  implicit val rawTransactionCodec: Codec[RawTransaction] = taggedCodec[String, modeltags.RawTransactionTag]
  implicit val rawSignedTransactionCodec: Codec[RawSignedTransaction] =
    taggedCodec[String, modeltags.RawSignedTransactionTag]

  implicit val utxoCodec: Codec[Utxo] = deriveCodec[Utxo]

  implicit val BlockhashJsonEncoder: Encoder[Blockhash] =
    Encoder[String].contramap(_.string)

  implicit val BlockVerbosityJsonEncoder: Encoder[BlockVerbosity] =
    Encoder[Int].contramap(_.int)

  implicit val AddressJsonEncoder: Encoder[Address] =
    Encoder[String].contramap(identity)

  implicit val AddressTypeEncoder: Encoder[AddressType] =
    Encoder[String].contramap(_.identifier)

  implicit val TransactionIdEncoder: Encoder[TransactionId] =
    Encoder[String].contramap(_.string)

  implicit val TransactionInputEncoder: Encoder[TransactionInput] =
    deriveEncoder[TransactionInput]

  implicit val TransactionOutputEncoder: Encoder[TransactionOutput] =
    Encoder[Map[String, String]].contramap(_.asMap)

  implicit val RawTransactionEncoder: Encoder[RawTransaction] =
    Encoder[String].contramap(identity)

  implicit val RawSignedTransactionEncoder: Encoder[RawSignedTransaction] =
    Encoder[String].contramap(identity)

  implicit val blockhashDecoder: Decoder[Blockhash] = Decoder.decodeString.emapTry { string =>
    Try(Blockhash.from(string).getOrElse(throw new RuntimeException("Invalid blockhash")))
  }

  implicit val blockHeaderDecoder: Decoder[BlockHeader] =
    Decoder.forProduct4("hash", "height", "time", "previousblockhash")(BlockHeader.apply)

  implicit val canonicalBlockDecoder: Decoder[Block.Canonical] = blockHeaderDecoder.map(Block.Canonical.apply)

  implicit val transactionIdDecoder: Decoder[TransactionId] = Decoder.decodeString.emapTry { string =>
    Try(TransactionId.from(string).getOrElse(throw new RuntimeException("Invalid transaction id")))
  }

  implicit val transactionOutputScriptDecoder: Decoder[Transaction.OutputScript] =
    Decoder.forProduct2("type", "asm")(Transaction.OutputScript.apply)

  implicit val transactionOutputDecoder: Decoder[Transaction.Output] =
    Decoder.forProduct3("value", "n", "scriptPubKey")(Transaction.Output.apply)

  implicit val fullBlockDecoder: Decoder[Block.Full] = {
    Decoder.decodeJson.emapTry { json =>
      val result = for {
        header <- json.as[BlockHeader]
        txDecoder = transactionDecoder(header.hash)
        txs <- json.hcursor.downField("tx").as[List[Transaction]](Decoder.decodeList(txDecoder))
      } yield Block.Full(header, txs)

      result match {
        case Left(e) => Failure(e)
        case Right(x) => Success(x)
      }
    }
  }

  private def transactionDecoder(blockhash: Blockhash): Decoder[Transaction] = {
    Decoder.forProduct2[Transaction, TransactionId, List[Transaction.Output]]("txid", "vout")(
      (id, vout) => Transaction(id = id, vout = vout, blockhash = blockhash)
    )
  }
}
