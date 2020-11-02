package io.iohk.atala.prism.credentials.japi

import io.iohk.atala.prism.credentials.{Credential => SCredential}
import io.iohk.atala.prism.util.ArrayOps._

private[japi] class CredentialWrapper[C](
    val wrapped: SCredential[C],
    protected val contentWrapper: CredentialContentFacadeFactory[C]
) extends Credential {
  override def getContentBytes: Array[Byte] = wrapped.contentBytes.toByteArray

  override def getContent: CredentialContent = contentWrapper.wrap(wrapped.content)
}
