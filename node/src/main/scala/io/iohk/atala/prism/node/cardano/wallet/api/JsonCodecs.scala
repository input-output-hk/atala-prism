package io.iohk.atala.prism.node.cardano.wallet.api

import io.circe._
import io.circe.generic.semiauto._
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.cardano.models.{Address, Lovelace, _}
import io.iohk.atala.prism.node.cardano.modeltags
import io.iohk.atala.prism.node.cardano.wallet.CardanoWalletApiClient.{CardanoWalletError, EstimatedFee}
import shapeless.tag
import shapeless.tag.@@

import scala.util.Try

private[api] object JsonCodecs {
  private def taggedCodec[A, T](implicit
      d: Decoder[A],
      e: Encoder[A]
  ): Codec[A @@ T] =
    Codec.from(
      d.map { a =>
        tag[T][A](a)
      },
      e.contramap((i: A @@ T) => i: A)
    )

  implicit val cardanoWalletErrorCodec: Codec[CardanoWalletError] =
    deriveCodec[CardanoWalletError]

  implicit val addressCodec: Codec[Address] =
    taggedCodec[String, modeltags.AddressTag]

  implicit val lovelaceEncoder: Encoder[Lovelace] = (a: Lovelace) =>
    Json.obj(
      ("quantity", Json.fromBigInt(a)),
      ("unit", Json.fromString("lovelace"))
    )
  implicit val lovelaceDecoder: Decoder[Lovelace] = (cursor: HCursor) => {
    for {
      quantity <- cursor
        .downField("quantity")
        .as[BigInt]
        .map(a => tag[modeltags.LovelaceTag][BigInt](a))
      // Fail decoding if "unit" != "lovelace"
      _ <- cursor.downField("unit").as[String].map {
        case "lovelace" => ()
        case unit =>
          throw new RuntimeException(s"Unexpected amount unit: $unit")
      }
    } yield quantity
  }

  implicit val transactionIdEncoder: Encoder[TransactionId] =
    Encoder[String].contramap(_.toString)
  implicit val transactionIdDecoder: Decoder[TransactionId] =
    Decoder.decodeString.emapTry { string =>
      Try(
        TransactionId
          .from(string)
          .getOrElse(throw new RuntimeException("Invalid transaction ID"))
      )
    }
  val transactionIdFromTransactionDecoder: Decoder[TransactionId] =
    (cursor: HCursor) => {
      for {
        transactionId <- cursor.downField("id").as[TransactionId]
      } yield transactionId
    }

  implicit val paymentCodec: Codec[Payment] = deriveCodec[Payment]

  implicit val transactionStatusDecoder: Decoder[TransactionStatus] =
    Decoder.decodeString.emapTry { string =>
      Try(TransactionStatus.withNameInsensitive(string))
    }

  implicit val transactionDetailsDecoder: Decoder[TransactionDetails] =
    (cursor: HCursor) => {
      for {
        transactionId <- cursor.downField("id").as[TransactionId]
        status <- cursor.downField("status").as[TransactionStatus]
      } yield TransactionDetails(transactionId, status)
    }

  implicit val estimatedFeeDecoder: Decoder[EstimatedFee] = (cursor: HCursor) => {
    for {
      min <- cursor.downField("estimated_min").as[Lovelace]
      max <- cursor.downField("estimated_max").as[Lovelace]
    } yield EstimatedFee(min, max)
  }

  implicit lazy val balanceDecoder: Decoder[Balance] = deriveDecoder[Balance]
  implicit lazy val walletStateDecoder: Decoder[WalletState] =
    deriveDecoder[WalletState]
  implicit lazy val walletDetailsDecoder: Decoder[WalletDetails] =
    deriveDecoder[WalletDetails]
}
