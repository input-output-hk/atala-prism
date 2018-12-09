package io.iohk.cef.data.storage.mv
import java.nio.file.Path

import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.data.query.{Field, Query, Value}
import io.iohk.cef.data.storage.TableStorage
import io.iohk.cef.data.{DataItem, DataItemId, TableId}
import io.iohk.cef.error.ApplicationError
import org.h2.mvstore.MVMap
import io.iohk.cef.data.error.{DataItemNotFound, InvalidQueryError}
import io.iohk.cef.data.query.Query.Predicate.toPredicateFn
import io.iohk.cef.data.query.Query.{Predicate, queryCata}
import io.iohk.cef.data.query.Value.StringRef
import io.iohk.cef.utils.mv.MVTable

import scala.collection.JavaConverters._
import scala.reflect.runtime.universe._

class MVTableStorage[I: NioCodec: TypeTag](tableId: TableId, storageFile: Path) extends TableStorage[I](tableId) {

  private val mvTable = new MVTable(tableId, storageFile, NioCodec[DataItem[I]])

  override def insert(dataItem: DataItem[I]): Unit = {
    mvTable.update(_.put(dataItem.id, dataItem))
  }

  override def delete(dataItem: DataItem[I]): Unit =
    mvTable.update(_.remove(dataItem.id))

  override def select(query: Query): Either[ApplicationError, Seq[DataItem[I]]] =
    try {
      Right(runSearch(query))
    } catch {
      case Error(e: InvalidQueryError) =>
        Left(e: ApplicationError)
    }

  override def selectSingle(dataItemId: DataItemId): Either[ApplicationError, DataItem[I]] = {
    Option(table.get(dataItemId))
      .map(Right(_))
      .getOrElse(Left(DataItemNotFound(tableId, dataItemId)))
  }

  private def runSearch(query: Query): Seq[DataItem[I]] =
    queryCata(
      fNoPred = noPredicateResult,
      fPred = evaluatePredicate,
      query
    )

  private def table: MVMap[String, DataItem[I]] = mvTable.table

  private def noPredicateResult: Seq[DataItem[I]] =
    table.values().asScala.toSeq

  private def accessField(dataItem: DataItem[I], field: Field): Value = field match {
    case Field(0) => StringRef(dataItem.id)
    case _ => throw Error(InvalidQueryError(tableId, field))
  }

  private def evaluatePredicate(predicate: Predicate): Seq[DataItem[I]] =
    table.asScala.values.filter(dataItem => toPredicateFn(accessField, predicate)(dataItem)).toSeq

  case class Error[T](t: T) extends RuntimeException
}
