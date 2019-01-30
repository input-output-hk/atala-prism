package io.iohk.cef.data.query

/**
  * Translates any part of a query's AST into their corresponding low level representation.
  * The lower level type would be defined by the library that
  * its being utilised for database access. For instance, in sqlikejdbc, the lower level type is SQLSyntax.
  */
trait Translator[HighLevelQueryPart, LowLvlQueryPart] {
  def translate(queryPart: HighLevelQueryPart): Option[LowLvlQueryPart]
}
