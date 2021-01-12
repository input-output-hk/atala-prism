package io.iohk.atala.cvp.webextension.common.models

final case class CredentialSubject(id: String, properties: Map[String, String])
final case class ConnectorRequest(bytes: Array[Byte])
