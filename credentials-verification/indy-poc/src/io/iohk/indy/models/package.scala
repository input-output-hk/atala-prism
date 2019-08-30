package io.iohk.indy

package object models {

  case class Did(string: String) extends AnyVal
  case class JsonString(string: String) extends AnyVal
  case class MasterSecretId(string: String) extends AnyVal
  case class CredentialId(string: String) extends AnyVal
  case class CredentialDefinitionId(string: String) extends AnyVal

  case class WalletDid(did: Did, verificationKey: String)
  case class CredentialDefinition(id: String, json: JsonString)
  case class CreateCredentialRequest(json: JsonString, metadataJson: JsonString)

  sealed abstract class UserRole(val string: String)
  object UserRole {
    final case object Trustee extends UserRole("TRUSTEE")
    final case object Steward extends UserRole("STEWARD")
    final case object TrustAnchor extends UserRole("TRUST_ANCHOR")
    final case object NetworkMonitor extends UserRole("NETWORK_MONITOR")
  }
}
