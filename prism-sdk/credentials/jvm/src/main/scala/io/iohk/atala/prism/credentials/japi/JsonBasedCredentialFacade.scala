package io.iohk.atala.prism.credentials.japi

import io.iohk.atala.prism.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.credentials.japi
import io.iohk.atala.prism.credentials.{Credential => SCredential}

private[japi] class JsonBasedCredentialFacade(
    override val wrapped: JsonBasedCredential,
    contentWrapper: CredentialContentFacadeFactory
) extends ECVerifiableCredentialFacade(
      wrapped,
      contentWrapper,
      ECCredentialSignature
    ) {
  override def wrapSigned(signed: SCredential): Credential = {
    signed match {
      case jsonSigned: JsonBasedCredential =>
        new JsonBasedCredentialFacade(jsonSigned, contentWrapper)
      case _ =>
        throw new IllegalArgumentException(s"Expected JsonBasedCredential, got ${signed.getClass.getName}")
    }
  }
}

private[japi] object JsonBasedCredentialFacade {

  @throws(classOf[CredentialParsingException])
  def parse(credential: String): JsonBasedCredentialFacade = {
    val parsedCredential = JsonBasedCredential.fromString(credential) match {
      case Left(error) => throw new CredentialParsingException(error.message)
      case Right(credential) => credential
    }

    new japi.JsonBasedCredentialFacade(
      parsedCredential,
      CredentialContentFacade
    )
  }
}
