package io.iohk.atala.prism.credentials.japi

import io.iohk.atala.prism.credentials.{VerifiableCredential => SVerifiableCredential}
import io.iohk.atala.prism.crypto.japi.{ECFacade, ECPrivateKeyFacade, ECPublicKeyFacade}
import io.iohk.atala.prism.crypto.{ECPrivateKey, ECPublicKey, ECSignature, japi}

private[japi] abstract class ECVerifiableCredentialFacade[C](
    override val wrapped: SVerifiableCredential[C, ECSignature, ECPrivateKey, ECPublicKey],
    contentWrapper: CredentialContentFacadeFactory[C],
    signatureWrapper: CredentialSignatureFacadeFactory[ECSignature]
) extends CredentialWrapper(wrapped, contentWrapper)
    with VerifiableCredential {
  import io.iohk.atala.prism.util.ArrayOps._

  protected def wrapSigned(signed: SVerifiableCredential[C, ECSignature, ECPrivateKey, ECPublicKey]): Credential

  override def getSignature: CredentialSignature = wrapped.signature.map(signatureWrapper.wrap).orNull

  override def isSigned: Boolean = wrapped.isSigned

  override def isUnverifiable: Boolean = wrapped.isUnverifiable

  override def getCanonicalForm: String = wrapped.canonicalForm

  override def getHash: Array[Byte] = wrapped.hash.value.toByteArray

  override def sign(privateKey: japi.ECPrivateKey, ec: japi.EC): Credential = {
    val scalaKey = ECPrivateKeyFacade.unwrap(privateKey)
    def signBytes(bytes: IndexedSeq[Byte]): ECSignature = ECFacade.unwrap(ec).sign(bytes.toByteArray, scalaKey)
    wrapSigned(wrapped.sign(signBytes))
  }

  override def verifySignature(publicKey: japi.ECPublicKey, ec: japi.EC): Boolean = {
    val scalaKey = ECPublicKeyFacade.unwrap(publicKey)
    def verifyBytes(bytes: IndexedSeq[Byte], signature: ECSignature): Boolean = {
      ECFacade.unwrap(ec).verify(bytes.toByteArray, scalaKey, signature)
    }
    wrapped.isValidSignature(verifyBytes)
  }
}
