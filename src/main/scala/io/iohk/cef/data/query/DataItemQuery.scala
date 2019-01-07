package io.iohk.cef.data.query

import io.iohk.cef.data.query.Query.Predicate

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

  def queryCata[T](fNoPred: => T, fPred: Predicate => T, query: Query): T = query match {
    case NoPredicateQuery =>
      fNoPred
    case p: Predicate =>
      fPred(p)
  }

  sealed trait Predicate extends Query {
    def and(that: Predicate): Predicate.And = Predicate.And(Seq(this, that))
    def or(that: Predicate): Predicate.Or = Predicate.Or(Seq(this, that))
  }

  object Predicate {
    case class Eq(field: Field, value: Value) extends Predicate
    case class And(predicates: Seq[Predicate]) extends Predicate
    case class Or(predicates: Seq[Predicate]) extends Predicate

    def predicateCata[T](fEq: (Field, Value) => T, fAnd: Seq[T] => T, fOr: Seq[T] => T, predicate: Predicate): T =
      predicate match {
        case Eq(field, value) =>
          fEq(field, value)
        case And(predicates) =>
          val ts: Seq[T] = predicates.map(p => predicateCata(fEq, fAnd, fOr, p))
          fAnd(ts)
        case Or(predicates) =>
          val ts: Seq[T] = predicates.map(p => predicateCata(fEq, fAnd, fOr, p))
          fOr(ts)
      }

    def toPredicateFn[T](fieldAccessor: (T, Field) => Value, predicate: Predicate): T => Boolean = {
      predicateCata[T => Boolean](
        fEq = (field, value) => t => fieldAccessor(t, field) == value,
        fAnd = predicates => t => predicates.forall(_.apply(t)),
        fOr = predicates => t => predicates.exists(_.apply(t)),
        predicate = predicate
      )
    }
  }
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
  implicit def floatRefConv(value: Float): FloatRef = FloatRef(value)
  implicit def longRefConv(value: Long): LongRef = LongRef(value)
  implicit def intRefConv(value: Int): IntRef = IntRef(value)
  implicit def shortRefConv(value: Short): ShortRef = ShortRef(value)
  implicit def byteRefConv(value: Byte): ByteRef = ByteRef(value)
  implicit def booleanRefConv(value: Boolean): BooleanRef = BooleanRef(value)
  implicit def charRefConv(value: Char): CharRef = CharRef(value)
  implicit def stringRefConv(value: String): StringRef = StringRef(value)
}
