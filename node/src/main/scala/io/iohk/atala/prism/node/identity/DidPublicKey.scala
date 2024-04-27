package io.iohk.atala.prism.node.identity

import io.iohk.atala.prism.protos.node_models.{KeyUsage, PublicKey}
import io.iohk.atala.prism.node.crypto.CryptoUtils.{SecpPublicKey, SecpPublicKeyOps}

case class DidPublicKey(id: String, usage: PublicKeyUsage, publicKey: SecpPublicKey) {
  def toProto: PublicKey =
    PublicKey(
      id = id,
      usage = usage.toProto,
      keyData = PublicKey.KeyData.CompressedEcKeyData(publicKey.toProto)
    )
}

sealed trait PublicKeyUsage {

  def toProto: KeyUsage

  def derivationIndex: Int

  def name: String =
    if (toProto.name.nonEmpty) toProto.name else throw new IllegalStateException("Key usage must have name")

}

object PublicKeyUsage {
  lazy val values: List[PublicKeyUsage] =
    List(
      MasterKeyUsage,
      IssuingKeyUsage,
      KeyAgreementKeyUsage,
      AuthenticationKeyUsage,
      RevocationKeyUsage,
      CapabilityInvocationKeyUsage,
      CapabilityDelegationKeyUsage
    )

  def fromProto(keyUsage: KeyUsage): PublicKeyUsage = {
    values.find(_.toProto == keyUsage).getOrElse(throw new IllegalStateException("Key usage not supported"))
  }
}

object MasterKeyUsage extends PublicKeyUsage {
  override def toProto: KeyUsage = KeyUsage.MASTER_KEY
  override def derivationIndex: Int = 0
}

object IssuingKeyUsage extends PublicKeyUsage {
  override def toProto: KeyUsage = KeyUsage.ISSUING_KEY
  override def derivationIndex: Int = 1
}

object KeyAgreementKeyUsage extends PublicKeyUsage() {
  override def toProto: KeyUsage = KeyUsage.KEY_AGREEMENT_KEY
  override def derivationIndex: Int = 2
}

object AuthenticationKeyUsage extends PublicKeyUsage() {
  override def toProto: KeyUsage = KeyUsage.AUTHENTICATION_KEY
  override def derivationIndex: Int = 3
}

object RevocationKeyUsage extends PublicKeyUsage {
  override def toProto: KeyUsage = KeyUsage.REVOCATION_KEY
  override def derivationIndex: Int = 4
}

object CapabilityInvocationKeyUsage extends PublicKeyUsage() {
  override def toProto: KeyUsage = KeyUsage.CAPABILITY_INVOCATION_KEY
  override def derivationIndex: Int = 5
}

object CapabilityDelegationKeyUsage extends PublicKeyUsage {
  override def toProto: KeyUsage = KeyUsage.CAPABILITY_DELEGATION_KEY
  override def derivationIndex: Int = 6
}
