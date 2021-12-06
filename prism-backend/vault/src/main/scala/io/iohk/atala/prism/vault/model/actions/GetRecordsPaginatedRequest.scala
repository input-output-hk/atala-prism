package io.iohk.atala.prism.vault.model.actions

import io.iohk.atala.prism.vault.model.Record

case class GetRecordsPaginatedRequest(type_ : Record.Type, lastSeenId: Option[Record.Id], limit: Int)
