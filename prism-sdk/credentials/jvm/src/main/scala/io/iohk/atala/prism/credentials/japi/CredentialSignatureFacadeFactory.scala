package io.iohk.atala.prism.credentials.japi

private[japi] trait CredentialSignatureFacadeFactory[-T] {
  def wrap(signature: T): CredentialSignature
}
