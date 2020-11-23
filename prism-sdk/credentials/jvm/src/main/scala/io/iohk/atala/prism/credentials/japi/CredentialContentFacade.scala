package io.iohk.atala.prism.credentials.japi

import java.time.LocalDate
import java.util.Optional

import io.iohk.atala.prism.credentials.{CredentialContent => SCredentialContent}

private[japi] class CredentialContentFacade(wrapped: SCredentialContent[_]) extends CredentialContent {
  import io.iohk.atala.prism.util.ArrayOps._

  def getCredentialType: Array[String] = wrapped.credentialType.toStringArray
  def getIssuerDid: Optional[String] = Optional.ofNullable(wrapped.issuerDid.map(_.value).orNull)
  def getIssuanceKeyId: Optional[String] = Optional.ofNullable(wrapped.issuanceKeyId.orNull)
  def getIssuanceDate: Optional[LocalDate] = Optional.ofNullable(wrapped.issuanceDate.orNull)
  def getExpiryDate: Optional[LocalDate] = Optional.ofNullable(wrapped.expiryDate.orNull)
}

object CredentialContentFacade extends CredentialContentFacadeFactory[SCredentialContent[_]] {
  override def wrap(content: SCredentialContent[_]): CredentialContent = new CredentialContentFacade(content)
}
