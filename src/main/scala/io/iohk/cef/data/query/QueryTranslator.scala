package io.iohk.cef.data.query

/**
  * Translates the query Q into a predicate type. This predicate type would be defined by the lower level library that
  * its being utilised for database access. For instance, in sqlikejdbc, the predicate type is SQLSyntax.
  * @tparam Q
  * @tparam Predicate
  */
trait QueryTranslator[Q <: Query, Predicate] {
  def translatePredicates[Table](q: Q): Option[Predicate]
}
