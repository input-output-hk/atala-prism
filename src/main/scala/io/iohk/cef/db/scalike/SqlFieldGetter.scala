package io.iohk.cef.db.scalike
import scalikejdbc._

trait SqlFieldGetter[Table] {
  def getField(index: Int, di: QuerySQLSyntaxProvider[SQLSyntaxSupport[Table], Table]): SQLSyntax
}
