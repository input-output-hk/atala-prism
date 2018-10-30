package io.iohk.cef.ledger.chimeric
import akka.util.ByteString
import io.iohk.cef.crypto._
import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.protobuf.ChimericLedger.{ChimericStringValueProto, CreateCurrencyProto, NonceProto}
import io.iohk.cef.protobuf.ChimericLedgerState.ChimericStateValueProto.Value._
import io.iohk.cef.protobuf.ChimericLedgerState.{AddressResultProto, ChimericStateValueProto, UtxoResultProto}

import scala.util.Try

object ChimericStateSerializer {
  implicit val byteStringSerializable = new ByteStringSerializable[ChimericStateResult] {

    override def decode(bytes: ByteString): Option[ChimericStateResult] = {
      for {
        parsed <- Try(ChimericStateValueProto.parseFrom(bytes.toArray)).toOption
      } yield {
        parsed.value match {
          case StringCreateCurrencyWrapper(CreateCurrencyProto(currency)) =>
            CreateCurrencyResult(CreateCurrency(currency))

          case StringNonceWrapper(NonceProto(nonce)) => NonceResult(nonce)

          case AddressResultWrapper(AddressResultProto(ChimericStringValueProto(map), key)) =>
            val value = Value(map.mapValues(BigDecimal.apply))
            val publicKey = SigningPublicKey
              .decodeFrom(ByteString(key.asReadOnlyByteBuffer()))
              .map(Option.apply)
              .getOrElse(Option.empty)

            AddressResult(value, publicKey)

          case UtxoResultWrapper(UtxoResultProto(ChimericStringValueProto(map), key)) =>
            val value = Value(map.mapValues(BigDecimal.apply))
            val publicKey = SigningPublicKey
              .decodeFrom(ByteString(key.asReadOnlyByteBuffer()))
              .map(Option.apply)
              .getOrElse(Option.empty)

            UtxoResult(value, publicKey)

          case Empty => throw new IllegalArgumentException("Expected a string state value wrapper but got Empty")
        }
      }
    }

    override def encode(t: ChimericStateResult): ByteString = {
      val proto = t match {
        case NonceResult(nonce) => StringNonceWrapper(NonceProto(nonce))

        case CreateCurrencyResult(createCurrency) =>
          StringCreateCurrencyWrapper(CreateCurrencyProto(createCurrency.currency))

        case UtxoResult(value, signingPublicKey) =>
          val keyBytes = signingPublicKey
            .map(_.toByteString.toArray)
            .getOrElse(Array.empty[Byte])

          val inner = UtxoResultProto(
            ChimericStringValueProto(encodeValue(value)),
            com.google.protobuf.ByteString.copyFrom(keyBytes)
          )

          UtxoResultWrapper(inner)

        case AddressResult(value, signingPublicKey) =>
          val keyBytes = signingPublicKey
            .map(_.toByteString.toArray)
            .getOrElse(Array.empty[Byte])

          val inner = AddressResultProto(
            ChimericStringValueProto(encodeValue(value)),
            com.google.protobuf.ByteString.copyFrom(keyBytes)
          )

          AddressResultWrapper(inner)
      }

      ByteString(ChimericStateValueProto(proto).toByteArray)
    }
  }

  private def encodeValue(value: Value): Map[String, String] = {
    value.iterator.map { case (key, inner) => (key, inner.toString()) }.toMap
  }
}
