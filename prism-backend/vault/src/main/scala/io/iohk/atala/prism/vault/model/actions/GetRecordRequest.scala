package io.iohk.atala.prism.vault.model.actions

import io.iohk.atala.prism.vault.model.Record

case class GetRecordRequest(type_ : Record.Type, id: Record.Id)
