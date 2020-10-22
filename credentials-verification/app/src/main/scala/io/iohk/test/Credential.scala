package io.iohk.test

case class Credential(metadata: Credential.Metadata, signature: Vector[Byte])

object Credential {

  // TODO: Include a multihash of the signed data instead
  case class Metadata(issuedBy: String, issuedOn: Long, subjectDID: String, data: Vector[Byte])
}
