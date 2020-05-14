package io.iohk.node.cardano.wallet.api

import io.circe._
import io.circe.generic.semiauto._
import io.iohk.node.cardano.models.{Address, Lovelace, _}
import io.iohk.node.cardano.modeltags
import io.iohk.node.cardano.wallet.CardanoWalletApiClient.CardanoWalletError
import shapeless.tag
import shapeless.tag.@@

import scala.util.Try

private[api] object JsonCodecs {
  private def taggedCodec[A, T](implicit d: Decoder[A], e: Encoder[A]): Codec[A @@ T] =
    Codec.from(
      d.map { a =>
        tag[T][A](a)
      },
      e.contramap((i: A @@ T) => i: A)
    )

  implicit val cardanoWalletErrorCodec: Codec[CardanoWalletError] =
    deriveCodec[CardanoWalletError]

  implicit val addressCodec: Codec[Address] = taggedCodec[String, modeltags.AddressTag]

  implicit val lovelaceEncoder: Encoder[Lovelace] = (a: Lovelace) =>
    Json.obj(("quantity", Json.fromBigInt(a)), ("unit", Json.fromString("lovelace")))
  implicit val lovelaceDecoder: Decoder[Lovelace] = (cursor: HCursor) => {
    for {
      quantity <- cursor.downField("quantity").as[BigInt].map(a => tag[modeltags.LovelaceTag][BigInt](a))
      // Fail decoding if "unit" != "lovelace"
      _ <- cursor.downField("unit").as[String].map {
        case "lovelace" => ()
        case unit => throw new RuntimeException(s"Unexpected amount unit: $unit")
      }
    } yield quantity
  }

  implicit val transactionIdEncoder: Encoder[TransactionId] =
    Encoder[String].contramap(_.string)
  implicit val transactionIdDecoder: Decoder[TransactionId] = Decoder.decodeString.emapTry { string =>
    Try(TransactionId.from(string).getOrElse(throw new RuntimeException("Invalid transaction ID")))
  }
  val transactionIdFromTransactionDecoder: Decoder[TransactionId] = (cursor: HCursor) => {
    for {
      transactionId <- cursor.downField("id").as[TransactionId]
    } yield transactionId
  }

  implicit val paymentCodec: Codec[Payment] = deriveCodec[Payment]
}
