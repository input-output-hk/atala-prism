package io.iohk.atala.prism.vault

import com.google.protobuf.ByteString
import io.iohk.atala.prism.protos.vault_api
import io.iohk.atala.prism.protos.vault_api.StoreRecordRequest
import io.iohk.atala.prism.vault.model.Record

import scala.util.Random

object TestUtils {

  def randomRecordType(): Record.Type = {
    Record.Type.unsafeFrom(Random.nextString(10).map(_.toByte).toArray)
  }

  def randomRecordId(): Record.Id = {
    Record.Id.unsafeFrom(Random.nextString(10).map(_.toByte).toArray)
  }

  def createRequest(
      type_ : Record.Type,
      id: Record.Id,
      payload: String
  ): (StoreRecordRequest, Record) = {
    val payloadBytes = payload.getBytes()
    val rec = Record(type_, id, Record.Payload(payloadBytes.toVector))
    (
      vault_api.StoreRecordRequest(
        `type` = ByteString.copyFrom(type_.encrypted.toArray),
        id = ByteString.copyFrom(id.encrypted.toArray),
        payload = ByteString.copyFrom(payloadBytes)
      ),
      rec
    )
  }

  def createRequest(payload: String, type_ : Record.Type = randomRecordType()): (StoreRecordRequest, Record) = {
    createRequest(type_, randomRecordId(), payload)
  }
}
