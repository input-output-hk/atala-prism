package io.iohk.atala.prism.identity.japi

import io.iohk.atala.prism.identity
import java.util.Optional

import io.iohk.atala.prism.compat.AsJavaConverter

class DIDFacade(did: identity.DID) extends DID {
  override def getValue: String = did.value

  override def isLongForm: Boolean = did.isLongForm

  override def isCanonicalForm: Boolean = did.isCanonicalForm

  override def getSuffix: Optional[String] =
    AsJavaConverter.asJavaOptional(Some(did.suffix.value))

  override def getCanonicalSuffix: Optional[String] =
    AsJavaConverter.asJavaOptional(did.getCanonicalSuffix.map(_.value))
}
