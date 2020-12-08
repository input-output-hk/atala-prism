package io.iohk.atala.prism.credentials.japi

import io.iohk.atala.prism.credentials.content.{CredentialContent => SCredentialContent}

private[japi] trait CredentialContentFacadeFactory {
  def wrap(credentialContent: SCredentialContent): CredentialContent
}
