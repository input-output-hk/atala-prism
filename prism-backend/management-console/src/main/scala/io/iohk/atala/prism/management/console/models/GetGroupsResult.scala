package io.iohk.atala.prism.management.console.models

case class GetGroupsResult(
    groups: List[InstitutionGroup.WithContactCount],
    totalNumberOfRecords: Int
)
