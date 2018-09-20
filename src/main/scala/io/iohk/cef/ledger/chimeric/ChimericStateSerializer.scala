package io.iohk.cef.ledger.chimeric
import akka.util.ByteString
import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.protobuf.ChimericLedger.{ChimericStringValueProto, CreateCurrencyProto}
import io.iohk.cef.protobuf.ChimericLedgerState.ChimericStateValueProto
import io.iohk.cef.protobuf.ChimericLedgerState.ChimericStateValueProto.Value.{Empty, StringCreateCurrencyWrapper, StringValueWrapper}

import scala.util.Try

object ChimericStateSerializer {
  implicit val byteStringSerializable = new ByteStringSerializable[ChimericStateValue] {

    override def decode(bytes: ByteString): Option[ChimericStateValue] = {
      for {
        parsed <- Try(ChimericStateValueProto.parseFrom(bytes.toArray)).toOption
      } yield {
        parsed.value match {
          case StringValueWrapper(ChimericStringValueProto(valueStringWrapper)) =>
            ValueHolder(Value(valueStringWrapper.mapValues(BigDecimal(_))))
          case StringCreateCurrencyWrapper(CreateCurrencyProto(currency)) =>
            CreateCurrencyHolder(CreateCurrency(currency))
          case Empty => throw new IllegalArgumentException("Expected a string state value wrapper but got Empty")
        }
      }
    }

    override def encode(t: ChimericStateValue): ByteString = {
      val value = t match {
        case ValueHolder(value) =>
          StringValueWrapper(ChimericStringValueProto(value.iterator.map {
            case (key, value) => (key, value.toString())
          }.toMap))
        case CreateCurrencyHolder(createCurrency) =>
          StringCreateCurrencyWrapper(CreateCurrencyProto(createCurrency.currency))
      }
      ByteString(ChimericStateValueProto(value).toByteArray)
    }
  }
}
