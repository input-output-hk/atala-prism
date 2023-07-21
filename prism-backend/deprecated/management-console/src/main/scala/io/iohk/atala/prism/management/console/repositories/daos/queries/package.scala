package io.iohk.atala.prism.management.console.repositories.daos

import doobie._
import doobie.implicits._
import io.iohk.atala.prism.management.console.models.PaginatedQueryConstraints

package object queries {

  def limitFr(limit: Int): Fragment = fr"""LIMIT $limit"""

  def offsetFr(offset: Int): Fragment = fr"""OFFSET $offset"""

  def toWhereCondition(
      left: Fragment,
      right: Fragment,
      condition: PaginatedQueryConstraints.ResultOrdering.Direction
  ): Fragment = {
    val conditionFr = condition match {
      case PaginatedQueryConstraints.ResultOrdering.Direction.Ascending => fr">"
      case PaginatedQueryConstraints.ResultOrdering.Direction.Descending =>
        fr"<"
    }

    left ++ conditionFr ++ right
  }

  def orderByFr[T](
      ordering: PaginatedQueryConstraints.ResultOrdering[T],
      uniqueColumn: String
  )(
      toColumnName: T => String
  ): Fragment = {
    val field = toColumnName(ordering.field)
    val condition = ordering.direction match {
      case PaginatedQueryConstraints.ResultOrdering.Direction.Ascending => "ASC"
      case PaginatedQueryConstraints.ResultOrdering.Direction.Descending =>
        "DESC"
    }

    fr"ORDER BY" ++ Fragment.const(s"$field $condition, $uniqueColumn")
  }
}
