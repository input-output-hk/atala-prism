package io.iohk.cef.db.scalike
import io.iohk.cef.data.query.{AndEqQuery, Query}
import io.iohk.cef.data.storage.scalike.ParameterBinderFactoryImplicits._
import scalikejdbc._

trait QueryScalikeTranslator[Q <: Query] {
  def translatePredicates[Table](q: Q,
                                 fieldGetter: SqlFieldGetter[Table],
                                 syntaxProvider: QuerySQLSyntaxProvider[SQLSyntaxSupport[Table], Table]): Option[SQLSyntax]
}

object QueryScalikeTranslator {

  implicit val queryTranslator: QueryScalikeTranslator[Query] =
    new QueryScalikeTranslator[Query] {
      override def translatePredicates[Table](
          q: Query,
          fieldGetter: SqlFieldGetter[Table],
          syntaxProvider: scalikejdbc.QuerySQLSyntaxProvider[scalikejdbc.SQLSyntaxSupport[Table], Table])
        : Option[scalikejdbc.SQLSyntax] = {
        q match {
          case and: AndEqQuery => andEqQueryTranslator.translatePredicates(and, fieldGetter, syntaxProvider)
          case _ => throw new IllegalArgumentException("Unexpected query type. Not able to translate.")
        }
      }
    }

  implicit val andEqQueryTranslator: QueryScalikeTranslator[AndEqQuery] =
    new QueryScalikeTranslator[AndEqQuery] {
      override def translatePredicates[Table](
          q: AndEqQuery,
          fieldGetter: SqlFieldGetter[Table],
          syntaxProvider: scalikejdbc.QuerySQLSyntaxProvider[scalikejdbc.SQLSyntaxSupport[Table], Table])
        : Option[SQLSyntax] = {
        if(q.eqPredicates.isEmpty) None
        else Some(sqls.joinWithAnd(q.eqPredicates.map(p => sqls.eq(fieldGetter.getField(p.a.index, syntaxProvider), p.b.ref)):_*))
      }
    }
}
