package io.iohk.cef.data.query
import io.iohk.cef.data.TableId

sealed trait Query[T] {
  final val languageVersion: Int = 1
}

class BasicQuery(val tableId: TableId, val eqPredicates: Seq[Eq])

sealed trait Predicate
case class Eq(a: Field, b: Value)

case class Value(ref: Ref)

case class Field(index: Int)

sealed trait Ref

object Ref {
  case class DoubleRef(value: Double) extends Ref
  case class FloatRef(value: Float) extends Ref
  case class LongRef(value: Long) extends Ref
  case class IntRef(value: Int) extends Ref
  case class ShortRef(value: Short) extends Ref
  case class ByteRef(value: Byte) extends Ref
  case class BooleanRef(value: Boolean) extends Ref
  case class CharRef(value: Char) extends Ref
  case class StringRef(value: String) extends Ref
}
