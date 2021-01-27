package io.iohk.atala.prism.management.console.repositories.daos
package queries

import doobie._
import doobie.implicits._
import doobie.util.fragments._
import io.iohk.atala.prism.management.console.models.{Contact, PaginatedQueryConstraints, ParticipantId}

object FindContactsQueryBuilder {

  def build(participantId: ParticipantId, constraints: Contact.PaginatedQuery): Fragment = {
    val whereInstitution = fr"""contacts.created_by = $participantId"""
    val whereGroup = constraints.filters.flatMap(_.groupName).map { group =>
      fr"""g.name = $group"""
    }
    val whereScroll = constraints.scrollId.map { scrollId => whereScrollFr(scrollId, constraints.ordering) }

    val baseQuery = (constraints.scrollId, constraints.filters.flatMap(_.groupName)) match {
      case (Some(scrollId), Some(_)) => selectFromScrollGroupFR(scrollId)
      case (Some(scrollId), None) => selectFromScrollFR(scrollId)
      case (None, Some(_)) => selectFromGroupFR
      case (None, None) => selectFR ++ fr"FROM contacts"
    }

    val orderBy = orderByFr(constraints.ordering, "contact_id") {
      case Contact.SortBy.ExternalId => "contacts.external_id"
      case Contact.SortBy.CreatedAt => "contacts.created_at"
    }

    baseQuery ++
      whereAndOpt(Some(whereInstitution), whereGroup, whereScroll) ++
      orderBy ++
      limitFr(constraints.limit)
  }

  private def whereScrollFr(
      scrollId: Contact.Id,
      ordering: PaginatedQueryConstraints.ResultOrdering[Contact.SortBy]
  ): Fragment = {
    val (left, right) = ordering.field match {
      case Contact.SortBy.ExternalId =>
        fr"(contacts.external_id, contact_id)" -> fr"(last_seen_external_id, $scrollId)"

      case Contact.SortBy.CreatedAt =>
        fr"(contacts.created_at, contact_id)" -> fr"(last_seen_time, $scrollId)"
    }

    toWhereCondition(left, right, ordering.condition)
  }

  private def scrollCTE(scrollId: Contact.Id) = {
    fr"""
        WITH CTE AS (
          SELECT created_at AS last_seen_time, external_id AS last_seen_external_id
          FROM contacts
          WHERE contact_id = $scrollId
        )
       """
  }

  private val selectFR = fr"""SELECT contact_id, external_id, contact_data, contacts.created_at"""
  private def selectFromScrollGroupFR(scrollId: Contact.Id) = {
    scrollCTE(scrollId) ++
      selectFR ++
      fr"""
          |FROM CTE CROSS JOIN contacts
          |     JOIN contacts_per_group USING (contact_id)
          |     JOIN institution_groups g USING (group_id)
          |""".stripMargin
  }

  private def selectFromGroupFR = {
    selectFR ++
      fr"""
          |FROM contacts
          |     JOIN contacts_per_group USING (contact_id)
          |     JOIN institution_groups g USING (group_id)
          |""".stripMargin
  }

  private def selectFromScrollFR(scrollId: Contact.Id) = {
    scrollCTE(scrollId) ++ selectFR ++
      fr"""
          |FROM CTE CROSS JOIN contacts
          |""".stripMargin
  }
}
