package io.iohk.atala.prism.credentials.japi

import io.iohk.atala.prism.credentials.{Credential => SCredential}
import io.iohk.atala.prism.crypto.japi.{ECFacade, ECPrivateKeyFacade, ECPublicKeyFacade}
import io.iohk.atala.prism.crypto.{ECSignature, japi}

private[japi] abstract class ECVerifiableCredentialFacade(
    override val wrapped: SCredential,
    contentWrapper: CredentialContentFacadeFactory,
    signatureWrapper: CredentialSignatureFacadeFactory[ECSignature]
) extends CredentialWrapper(wrapped, contentWrapper)
    with VerifiableCredential {
  import io.iohk.atala.prism.util.ArrayOps._

  protected def wrapSigned(signed: SCredential): Credential

  override def getSignature: CredentialSignature = wrapped.signature.map(signatureWrapper.wrap).orNull

  override def isSigned: Boolean = wrapped.isSigned

  override def isUnverifiable: Boolean = wrapped.isUnverifiable

  override def getCanonicalForm: String = wrapped.canonicalForm

  override def getHash: Array[Byte] = wrapped.hash.value.toByteArray

  override def sign(privateKey: japi.ECPrivateKey, ec: japi.EC): Credential = {
    val scalaKey = ECPrivateKeyFacade.unwrap(privateKey)
    wrapSigned(wrapped.sign(scalaKey)(ECFacade.unwrap(ec)))
  }

  override def verifySignature(publicKey: japi.ECPublicKey, ec: japi.EC): Boolean = {
    val scalaKey = ECPublicKeyFacade.unwrap(publicKey)
    wrapped.isValidSignature(scalaKey)(ECFacade.unwrap(ec))
  }
}
