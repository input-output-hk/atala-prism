package io.iohk.cef.db.scalike
import io.iohk.cef.data.query.{AndEqQuery, Query, QueryTranslator}
import io.iohk.cef.data.storage.scalike.ParameterBinderFactoryImplicits._
import scalikejdbc._

object QueryScalikeTranslator {

  def queryTranslator[Table](
      fieldGetter: SqlFieldGetter[Table],
      syntaxProvider: QuerySQLSyntaxProvider[SQLSyntaxSupport[Table], Table]): QueryTranslator[Query, SQLSyntax] = {
    new QueryTranslator[Query, SQLSyntax] {
      override def translatePredicates[Table](q: Query): Option[SQLSyntax] = {
        q match {
          case and: AndEqQuery => andEqQueryTranslator(fieldGetter, syntaxProvider).translatePredicates(and)
          case _ => throw new IllegalArgumentException("Unexpected query type. Not able to translate.")
        }
      }
    }
  }

  def andEqQueryTranslator[Table](
      fieldGetter: SqlFieldGetter[Table],
      syntaxProvider: scalikejdbc.QuerySQLSyntaxProvider[scalikejdbc.SQLSyntaxSupport[Table], Table])
    : QueryTranslator[AndEqQuery, SQLSyntax] = {
    new QueryTranslator[AndEqQuery, SQLSyntax] {
      override def translatePredicates[Table](q: AndEqQuery): Option[SQLSyntax] = {
        if (q.eqPredicates.isEmpty) None
        else
          Some(sqls.joinWithAnd(q.eqPredicates.map(p =>
            sqls.eq(fieldGetter.getField(p.field.index, syntaxProvider), p.value.ref)): _*))
      }
    }
  }
}
