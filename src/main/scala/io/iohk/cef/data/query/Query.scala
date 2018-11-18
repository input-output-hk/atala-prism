package io.iohk.cef.data.query

/**
  * The queries should be defined with an AST in which every component is serializable. This means,
  * no node/class/type can receive functions as parameters.
  * The AST can be made as complex as necessary in the future.
  * The code should only reference this type. This way we make ourselves sure that we can extend the AST in the future.
  * Specific treatment for subclasses (translation and serialization) will be handled by type classes.
  */
sealed trait Query {
  final val languageVersion: Int = 1
}

object Query {
  case object SelectAll extends Query
  /**
    * A very basic query type just to get us started. It represents a query on a specific table where
    * the predicates are all Equality predicates and they are joined by ands
    * @param tableId
    * @param eqPredicates
    */
  case class AndEqQuery(val eqPredicates: Seq[Predicate.Eq]) extends Query

}

sealed trait Predicate
object Predicate {
  case class Eq(field: Field, value: Value) extends Predicate
}

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
