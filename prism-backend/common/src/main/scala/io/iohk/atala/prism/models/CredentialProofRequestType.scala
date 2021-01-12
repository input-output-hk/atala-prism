package io.iohk.atala.prism.models

sealed trait CredentialProofRequestType {
  val typeId: String
}

object CredentialProofRequestType {
  case object RedlandIdCredential extends CredentialProofRequestType {
    override val typeId: String = "VerifiableCredential/RedlandIdCredential"
  }
  case class SignedCredential(typeId: String) extends CredentialProofRequestType
}
