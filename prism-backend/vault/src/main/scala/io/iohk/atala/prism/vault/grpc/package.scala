package io.iohk.atala.prism.vault

import io.iohk.atala.prism.grpc.ProtoConverter
import io.iohk.atala.prism.protos.vault_api
import io.iohk.atala.prism.vault.model.Record
import io.iohk.atala.prism.vault.model.actions.{GetRecordRequest, GetRecordsPaginatedRequest, StoreRecordRequest}

import scala.util.Try

package object grpc {

  implicit val storeRecordRequestConverter: ProtoConverter[
    vault_api.StoreRecordRequest,
    StoreRecordRequest
  ] = { (request: vault_api.StoreRecordRequest, _) =>
    for {
      record <- Try {
        if (request.record.isEmpty)
          throw new RuntimeException("Record is required")
        else request.record.get
      }

      type_ <- Try(Record.Type.unsafeFrom(record.`type`.toByteArray))
      id <- Try(Record.Id.unsafeFrom(record.id.toByteArray))
      payload <- Try(Record.Payload.unsafeFrom(record.payload.toByteArray))

    } yield StoreRecordRequest(Record(type_, id, payload))
  }

  implicit val getRecordRequestConverter: ProtoConverter[
    vault_api.GetRecordRequest,
    GetRecordRequest
  ] = { (request: vault_api.GetRecordRequest, _) =>
    for {
      type_ <- Try(Record.Type.unsafeFrom(request.`type`.toByteArray))
      id <- Try(Record.Id.unsafeFrom(request.id.toByteArray))
    } yield GetRecordRequest(type_, id)
  }

  implicit val getRecordsPaginatedRequestConverter: ProtoConverter[
    vault_api.GetRecordsPaginatedRequest,
    GetRecordsPaginatedRequest
  ] = { (request: vault_api.GetRecordsPaginatedRequest, _) =>
    for {
      type_ <- Try(Record.Type.unsafeFrom(request.`type`.toByteArray))
      lastId <- Try(
        if (request.lastSeenId.isEmpty) None
        else Some(Record.Id.unsafeFrom(request.lastSeenId.toByteArray))
      )
    } yield GetRecordsPaginatedRequest(type_, lastId, request.limit)
  }
}
