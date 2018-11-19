package io.iohk.cef.db.scalike
import io.iohk.cef.data.DataItem
import io.iohk.cef.data.query.Query.{NoPredicateQuery, Predicate}
import io.iohk.cef.data.query._
import io.iohk.cef.data.storage.scalike.DataItemTable
import io.iohk.cef.db.scalike.ParameterBinderFactoryImplicits._
import scalikejdbc._

object QueryScalikeTranslator {

  def queryPredicateTranslator[Table](
      fieldGetter: Translator[Field, SQLSyntax],
      syntaxProvider: QuerySQLSyntaxProvider[SQLSyntaxSupport[Table], Table]): Translator[Query, SQLSyntax] = { q =>
    {
      val predTranslator = predicateTranslator(fieldGetter, syntaxProvider)
      q match {
        case p: Predicate => predTranslator.translate(p)
        case _: NoPredicateQuery.type => None
        case _ => throw new IllegalArgumentException("Unexpected query type. Not able to translate.")
      }
    }
  }

  def predicateTranslator[Table](
      fieldGetter: Translator[Field, SQLSyntax],
      syntaxProvider: scalikejdbc.QuerySQLSyntaxProvider[scalikejdbc.SQLSyntaxSupport[Table], Table])
    : Translator[Predicate, SQLSyntax] =
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
      fieldGetter: Translator[Field, SQLSyntax],
      syntaxProvider: scalikejdbc.QuerySQLSyntaxProvider[scalikejdbc.SQLSyntaxSupport[Table], Table])
    : Translator[Predicate.Or, SQLSyntax] =
    pc => {
      val pt = predicateTranslator(fieldGetter, syntaxProvider)
      Some(sqls.joinWithOr(pc.predicates.map(pt.translate).flatten: _*))
    }

  def andPredicateComposerTranslator[Table](
      fieldGetter: Translator[Field, SQLSyntax],
      syntaxProvider: scalikejdbc.QuerySQLSyntaxProvider[scalikejdbc.SQLSyntaxSupport[Table], Table])
    : Translator[Predicate.And, SQLSyntax] =
    pc => {
      val pt = predicateTranslator(fieldGetter, syntaxProvider)
      Some(sqls.joinWithAnd(pc.predicates.map(pt.translate).flatten: _*))
    }

  def eqPredicateTranslator[Table](
      fieldGetter: Translator[Field, SQLSyntax],
      syntaxProvider: scalikejdbc.QuerySQLSyntaxProvider[scalikejdbc.SQLSyntaxSupport[Table], Table])
    : Translator[Predicate.Eq, SQLSyntax] =
    p => fieldGetter.translate(p.field).map(f => sqls.eq(f, p.value))

  def dataItemFieldTranslator[T](
      di: QuerySQLSyntaxProvider[SQLSyntaxSupport[DataItemTable], DataItemTable]): Translator[Field, SQLSyntax] =
    f => {
      f.index match {
        case DataItem.FieldIds.DataItemId => Some(di.dataItemId)
        case DataItem.FieldIds.DataTableId => Some(di.dataTableId)
      }
    }
}
