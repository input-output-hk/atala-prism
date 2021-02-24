package io.iohk.atala.cvp.webextension.common.models

final case class CredentialSubject(
    id: String, // this is the console credential id
    properties: Map[String, String] // fields that will be part of the credential subject (claims)
)
final case class ConnectorRequest(bytes: Array[Byte])
