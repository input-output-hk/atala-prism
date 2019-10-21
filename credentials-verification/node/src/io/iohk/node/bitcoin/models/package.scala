package io.iohk.node.bitcoin

import shapeless.tag.@@
import shapeless.tag

// Kept in a separate package to avoid polluting the `models` namespace
package modeltags {
  sealed trait BtcTag
  sealed trait AddressTag
  sealed trait RawTransactionTag
  sealed trait RawSignedTransactionTag
  sealed trait VoutTag
  sealed trait OpDataTag
}

// Kept in a separate package to avoid polluting the `models` namespace
package modelutils {
  trait TypeCompanion[A, T] {
    def apply(a: A): A @@ T = tag[T][A](a)
    def unapply(wrapper: A @@ T): Option[A] = Some(wrapper)
  }

  abstract class ValidatedTypeCompanion[A, T](isValid: A => Boolean) {
    def apply(a: A): Option[A @@ T] =
      Some(tag[T][A](a)).filter(isValid)
    def unapply(wrapper: A @@ T): Option[A] = Some(wrapper)
  }
}

package object models {

  import modeltags._
  import modelutils._

  type Btc = BigDecimal @@ BtcTag
  object Btc extends TypeCompanion[BigDecimal, BtcTag]

  type Address = String @@ AddressTag
  object Address extends TypeCompanion[String, AddressTag]

  type RawTransaction = String @@ RawTransactionTag
  object RawTransaction extends TypeCompanion[String, RawTransactionTag]

  type RawSignedTransaction = String @@ RawSignedTransactionTag
  object RawSignedTransaction extends TypeCompanion[String, RawSignedTransactionTag]

  type Vout = Int @@ VoutTag
  object Vout extends TypeCompanion[Int, VoutTag]

  // TODO: Eventually we will want to increase to the real max length supported by bitcoin
  val OP_RETURN_MAX_LENGTH = 128
  type OpData = Array[Byte] @@ OpDataTag
  object OpData
      extends ValidatedTypeCompanion[Array[Byte], OpDataTag](bs => op_data_hex(bs).length <= OP_RETURN_MAX_LENGTH)

  private def op_data_hex(in: Array[Byte]): String =
    in.toList.map(b => String.format("%02X", Byte.box(b))).mkString

  implicit class OpDateExtensions(val od: OpData) extends AnyVal {
    def toHex: String =
      op_data_hex(od)
  }
}
