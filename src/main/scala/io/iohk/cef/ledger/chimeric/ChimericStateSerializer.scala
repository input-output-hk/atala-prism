package io.iohk.cef.ledger.chimeric
import akka.util.ByteString
import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.protobuf.ChimericLedger.{ChimericStringValueProto, CreateCurrencyProto}
import io.iohk.cef.protobuf.ChimericLedgerState.ChimericStateValueProto
import io.iohk.cef.protobuf.ChimericLedgerState.ChimericStateValueProto.Value.{
  StringCreateCurrencyWrapper,
  StringValueWrapper
}

import scala.util.Try

object ChimericStateSerializer {
  implicit val byteStringSerializable = new ByteStringSerializable[ChimericStateValue] {

    override def decode(bytes: ByteString): Option[ChimericStateValue] = {
      for {
        parsed <- Try(ChimericStateValueProto.parseFrom(bytes.toArray)).toOption
      } yield {
        if (parsed.value.isStringCreateCurrencyWrapper) {
          CreateCurrencyHolder(CreateCurrency(parsed.value.stringCreateCurrencyWrapper.get.currency))
        } else if (parsed.value.isStringValueWrapper) {
          ValueHolder(
            Value(parsed.value.stringValueWrapper.get.valueStringWrapper.mapValues(BigDecimal(_)))
          )
        } else {
          throw new IllegalArgumentException(s"Unidentified value: ${parsed.value}")
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
