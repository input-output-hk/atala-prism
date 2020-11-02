package io.iohk.atala.prism.credentials.japi

import io.iohk.atala.prism.crypto.ECSignature

private[japi] class ECCredentialSignature(wrapped: ECSignature) extends CredentialSignature {
  def getData(): Array[Byte] = wrapped.data
  def getHexEncoded(): String = wrapped.getHexEncoded
}

private[japi] object ECCredentialSignature extends CredentialSignatureFacadeFactory[ECSignature] {
  override def wrap(signature: ECSignature): CredentialSignature = {
    new ECCredentialSignature(signature)
  }
}
