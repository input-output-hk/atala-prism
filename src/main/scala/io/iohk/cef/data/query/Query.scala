package io.iohk.cef.data.query

import scala.language.implicitConversions

/**
  * The queries should be defined with an AST in which every component is serializable. This means,
  * no node/class/type can receive functions as parameters or can *be* functions.
  * The AST can be made as complex as necessary in the future.
  * The code should only reference the Query type. This way we make ourselves sure that we can extend the AST in the future.
  * Specific treatment for subclasses (translation and serialization) will be handled by type classes.
  */
sealed trait Query {
  final val languageVersion: Int = 1
}

object Query {
  case object NoPredicateQuery extends Query

  /**
    * A very basic query type just to get us started. It represents a query on a specific table with only the predicate
    * component
    */
  case class BasicQuery(predicate: Predicate) extends Query

  implicit def basicQueryWrap(predicate: Predicate): BasicQuery = BasicQuery(predicate)

}

sealed trait Predicate {
  def and(that: Predicate): Predicate.And = Predicate.And(Seq(this, that))
  def or(that: Predicate): Predicate.Or = Predicate.Or(Seq(this, that))
}
object Predicate {
  case class Eq(field: Field, value: Value) extends Predicate
  case class And(predicates: Seq[Predicate]) extends Predicate
  case class Or(predicates: Seq[Predicate]) extends Predicate
}

case class Field(index: Int) {
  def #==(value: Value): Predicate.Eq = Predicate.Eq(this, value)
}

sealed trait Value

object Value {
  case class DoubleRef(value: Double) extends Value
  case class FloatRef(value: Float) extends Value
  case class LongRef(value: Long) extends Value
  case class IntRef(value: Int) extends Value
  case class ShortRef(value: Short) extends Value
  case class ByteRef(value: Byte) extends Value
  case class BooleanRef(value: Boolean) extends Value
  case class CharRef(value: Char) extends Value
  case class StringRef(value: String) extends Value

  //implicit conversions to ease out the usage of values
  implicit def doubleRefConv(value: Double): DoubleRef = DoubleRef(value)
  implicit def floatRef(value: Float): FloatRef = FloatRef(value)
  implicit def longRef(value: Long): LongRef = LongRef(value)
  implicit def intRef(value: Int): IntRef = IntRef(value)
  implicit def shortRef(value: Short): ShortRef = ShortRef(value)
  implicit def byteRef(value: Byte): ByteRef = ByteRef(value)
  implicit def booleanRef(value: Boolean): BooleanRef = BooleanRef(value)
  implicit def charRef(value: Char): CharRef = CharRef(value)
  implicit def stringRef(value: String): StringRef = StringRef(value)
}
