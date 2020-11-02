package io.iohk.atala.prism.credentials.japi

import java.time.LocalDate

import io.iohk.atala.prism.credentials.{CredentialContent => SCredentialContent}

private[japi] class CredentialContentFacade(wrapped: SCredentialContent[_]) extends CredentialContent {
  import io.iohk.atala.prism.util.ArrayOps._

  def getCredentialType(): Array[String] = wrapped.credentialType.toStringArray

  def getIssuerDid(): String = wrapped.issuerDid.map(_.value).orNull

  def hasIssuanceKeyId(): Boolean = wrapped.issuanceKeyId.isDefined
  def getIssuanceKeyId(): String = wrapped.issuanceKeyId.orNull

  def hasIssuanceDate(): Boolean = wrapped.issuanceDate.isDefined
  def getIssuanceDate(): LocalDate = wrapped.issuanceDate.orNull

  def hasExpiryDate(): Boolean = wrapped.expiryDate.isDefined
  def getExpiryDate(): LocalDate = wrapped.expiryDate.orNull
}

object CredentialContentFacade extends CredentialContentFacadeFactory[SCredentialContent[_]] {
  override def wrap(content: SCredentialContent[_]): CredentialContent = new CredentialContentFacade(content)
}
