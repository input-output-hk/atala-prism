package io.iohk.atala.prism.credentials.japi

import io.iohk.atala.prism.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.credentials.json.implicits._
import io.iohk.atala.prism.credentials.{
  japi,
  CredentialContent => SCredentialContent,
  VerifiableCredential => SVerifiableCredential
}
import io.iohk.atala.prism.crypto.{ECPrivateKey, ECPublicKey, ECSignature}

private[japi] class JsonBasedCredentialFacade[C](
    override val wrapped: JsonBasedCredential[C],
    contentWrapper: CredentialContentFacadeFactory[C]
) extends ECVerifiableCredentialFacade[C](
      wrapped,
      contentWrapper,
      ECCredentialSignature
    ) {
  override def wrapSigned(signed: SVerifiableCredential[C, ECSignature, ECPrivateKey, ECPublicKey]): Credential = {
    signed match {
      case jsonSigned: JsonBasedCredential[C] =>
        new JsonBasedCredentialFacade[C](jsonSigned, contentWrapper)
      case _ =>
        throw new IllegalArgumentException(s"Expected JsonBasedCredential, got ${signed.getClass.getName}")
    }
  }
}

private[japi] object JsonBasedCredentialFacade {

  @throws(classOf[CredentialParsingException])
  def parse(credential: String): JsonBasedCredentialFacade[SCredentialContent[_]] = {
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
