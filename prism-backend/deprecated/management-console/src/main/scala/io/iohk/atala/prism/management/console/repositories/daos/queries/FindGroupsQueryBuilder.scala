package io.iohk.atala.prism.management.console.repositories.daos
package queries

import io.iohk.atala.prism.management.console.models.{InstitutionGroup, ParticipantId}
import doobie._
import doobie.implicits._
import doobie.implicits.legacy.localdate._
import doobie.util.fragments._

object FindGroupsQueryBuilder {

  def build(
      institutionId: ParticipantId,
      query: InstitutionGroup.PaginatedQuery
  ): Fragment = {

    val orderBy = orderByFr(query.ordering, "inst_gr.group_id") {
      case InstitutionGroup.SortBy.Name => "inst_gr.name"
      case InstitutionGroup.SortBy.CreatedAt => "inst_gr.created_at"
      case InstitutionGroup.SortBy.NumberOfContacts => "number_of_contacts"
    }

    selectFr ++
      fromFr ++
      joinContactsPerGroupFr(query) ++
      whereFr(institutionId, query) ++
      orderBy ++
      limitFr(query.limit) ++
      offsetFr(query.offset)
  }

  def buildTotalNumberOfRecordsQuery(
      institutionId: ParticipantId,
      query: InstitutionGroup.PaginatedQuery
  ): Fragment = {
    selectCountFr ++
      fromFr ++
      joinContactsPerGroupFr(query) ++
      whereFr(institutionId, query)
  }

  private def whereFr(
      institutionId: ParticipantId,
      query: InstitutionGroup.PaginatedQuery
  ): Fragment = {
    val whereInstitution = fr"""inst_gr.institution_id = $institutionId"""

    val whereName = query.filters.flatMap(_.name).map { name =>
      val regexName = s"%${name.value}%"
      fr"inst_gr.name ILIKE $regexName"
    }

    val whereCreatedBefore =
      query.filters.flatMap(_.createdBefore).map { createdBefore =>
        fr"inst_gr.created_at::DATE <= $createdBefore"
      }

    val whereCreatedAfter =
      query.filters.flatMap(_.createdAfter).map { createdAfter =>
        fr"inst_gr.created_at::DATE >= $createdAfter"
      }

    val whereContactId = query.filters.flatMap(_.contactId).map { contactId =>
      fr"cg.contact_id = $contactId"
    }

    whereAndOpt(
      Some(whereInstitution),
      whereName,
      whereCreatedBefore,
      whereCreatedAfter,
      whereContactId
    )
  }

  private val selectFr =
    fr"""
         |SELECT inst_gr.group_id, inst_gr.name, inst_gr.institution_id, inst_gr.created_at, (
         |  SELECT COUNT(*)
         |  FROM contacts_per_group
         |  WHERE group_id = inst_gr.group_id
         |) AS number_of_contacts""".stripMargin

  private val selectCountFr = fr"SELECT count(*)"

  private val fromFr =
    fr"FROM institution_groups inst_gr"

  private def joinContactsPerGroupFr(query: InstitutionGroup.PaginatedQuery) =
    if (query.filters.flatMap(_.contactId).isDefined)
      fr"JOIN contacts_per_group cg USING (group_id)"
    else fr""

}
