package io.iohk.atala.prism.management.console.models

case class PaginatedQueryConstraints[ScrollId, SortBy, FilterBy](
    limit: Int = 10,
    offset: Int = 0,
    ordering: PaginatedQueryConstraints.ResultOrdering[SortBy],
    scrollId: Option[ScrollId] = None,
    filters: Option[FilterBy] = None
)

object PaginatedQueryConstraints {

  /** Defines how to sort the result while retrieving data
    *
    * @param field
    *   the possible field to sort the result by
    * @param direction
    *   whether the result should be sorted in ascending or descending order
    * @tparam T
    *   the type used to sort the result
    */
  case class ResultOrdering[T](field: T, direction: ResultOrdering.Direction = ResultOrdering.Direction.Ascending)

  object ResultOrdering {
    sealed trait Direction extends Product with Serializable
    object Direction {
      final case object Ascending extends Direction
      final case object Descending extends Direction
    }
  }
}
