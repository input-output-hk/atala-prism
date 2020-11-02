package io.iohk.atala.prism.credentials.japi

private[japi] trait CredentialContentFacadeFactory[-T] {
  def wrap(credentialContent: T): CredentialContent
}
