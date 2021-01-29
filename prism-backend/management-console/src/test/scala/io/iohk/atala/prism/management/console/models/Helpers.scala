package io.iohk.atala.prism.management.console.models

object Helpers {
  // helper to keep the behavior before adding sorting/filters
  def legacyQuery(
      scrollId: Option[Contact.Id],
      groupName: Option[InstitutionGroup.Name],
      limit: Int
  ): Contact.PaginatedQuery = {
    import PaginatedQueryConstraints._

    PaginatedQueryConstraints(
      limit = limit,
      ordering = ResultOrdering(Contact.SortBy.createdAt, ResultOrdering.Direction.Ascending),
      scrollId = scrollId,
      filters = Some(Contact.FilterBy(groupName))
    )
  }
}
