package io.iohk.cef.db.scalike
import io.iohk.cef.data.query.Query.{BasicQuery, NoPredicateQuery}
import io.iohk.cef.data.query._
import io.iohk.cef.db.scalike.ParameterBinderFactoryImplicits._
import scalikejdbc._

object QueryScalikeTranslator {

  def queryPredicateTranslator[Table](
      fieldGetter: SqlFieldGetter[Table],
      syntaxProvider: QuerySQLSyntaxProvider[SQLSyntaxSupport[Table], Table]): PredicateTranslator[Query, SQLSyntax] = {
    q =>
      {
        val predTranslator = predicateTranslator(fieldGetter, syntaxProvider)
        q match {
          case BasicQuery(predicate) => predTranslator.translate(predicate)
          case _: NoPredicateQuery.type => None
          case _ => throw new IllegalArgumentException("Unexpected query type. Not able to translate.")
        }
      }
  }

  def predicateTranslator[Table](
      fieldGetter: SqlFieldGetter[Table],
      syntaxProvider: scalikejdbc.QuerySQLSyntaxProvider[scalikejdbc.SQLSyntaxSupport[Table], Table])
    : PredicateTranslator[Predicate, SQLSyntax] =
    pc => {
      val andTranslator = andPredicateComposerTranslator(fieldGetter, syntaxProvider)
      val orTranslator = orPredicateComposerTranslator(fieldGetter, syntaxProvider)
      val eqTranslator = eqPredicateTranslator(fieldGetter, syntaxProvider)
      pc match {
        case and: Predicate.And => andTranslator.translate(and)
        case or: Predicate.Or => orTranslator.translate(or)
        case eq: Predicate.Eq => eqTranslator.translate(eq)
      }
    }

  def orPredicateComposerTranslator[Table](
      fieldGetter: SqlFieldGetter[Table],
      syntaxProvider: scalikejdbc.QuerySQLSyntaxProvider[scalikejdbc.SQLSyntaxSupport[Table], Table])
    : PredicateTranslator[Predicate.Or, SQLSyntax] =
    pc => {
      val pt = predicateTranslator(fieldGetter, syntaxProvider)
      Some(sqls.joinWithOr(pc.predicates.map(pt.translate).flatten: _*))
    }

  def andPredicateComposerTranslator[Table](
      fieldGetter: SqlFieldGetter[Table],
      syntaxProvider: scalikejdbc.QuerySQLSyntaxProvider[scalikejdbc.SQLSyntaxSupport[Table], Table])
    : PredicateTranslator[Predicate.And, SQLSyntax] =
    pc => {
      val pt = predicateTranslator(fieldGetter, syntaxProvider)
      Some(sqls.joinWithAnd(pc.predicates.map(pt.translate).flatten: _*))
    }

  def eqPredicateTranslator[Table](
      fieldGetter: SqlFieldGetter[Table],
      syntaxProvider: scalikejdbc.QuerySQLSyntaxProvider[scalikejdbc.SQLSyntaxSupport[Table], Table])
    : PredicateTranslator[Predicate.Eq, SQLSyntax] =
    p => Some(sqls.eq(fieldGetter.getField(p.field.index, syntaxProvider), p.value))
}
