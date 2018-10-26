package io.iohk.cef.data.query
import io.iohk.cef.TableId

sealed class Query[T, U] private (val select: Select[T], val projection: Projection[T, U]) {
  final val languageVersion: Int = 1

  def tableId: TableId = select.tableId
}

object Query {
  def apply[T, U](select: Select[T], projection: Projection[T, U]): Query[T, U] =
    new Query(select, projection)

  def apply[T](select: Select[T]): Query[T, T] =
    new Query[T, T](select, new Projection[T, T](identity))
}

sealed class Projection[T, U](val f: T => U)
sealed class Select[T](val tableId: TableId, val predicate: T => Boolean)
