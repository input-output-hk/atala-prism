package io.iohk.atala.prism.credentials.japi

import java.util.Optional

import io.iohk.atala.prism.credentials.content.{CredentialContent => SCredentialContent}
import io.iohk.atala.prism.credentials.content.CredentialContent.{CredentialContentException, FieldNotFoundException}

private[japi] class CredentialContentFacade(wrapped: SCredentialContent) extends CredentialContent {

  @throws(classOf[CredentialContentException])
  def getString(field: String): Optional[String] = toOptional(wrapped.getString(field))

  @throws(classOf[CredentialContentException])
  def getInt(field: String): Optional[Int] = toOptional(wrapped.getInt(field))

  // TODO: Implement methods for `getSeq` and `getSubFields`.

  @throws(classOf[CredentialContentException])
  private def toOptional[T](value: Either[CredentialContentException, T]): Optional[T] = {
    value match {
      case Right(value) => Optional.of(value)
      case Left(_: FieldNotFoundException) => Optional.empty
      case Left(ex) => throw ex
    }
  }
}

object CredentialContentFacade extends CredentialContentFacadeFactory {
  override def wrap(content: SCredentialContent): CredentialContent = new CredentialContentFacade(content)
}
