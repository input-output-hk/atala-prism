package io.iohk.atala.prism.logging

import io.iohk.atala.prism.kotlin.credentials.CredentialBatchId
import io.iohk.atala.prism.kotlin.identity.{DID, DIDSuffix}
import tofu.logging._

/**
  * Used for libraries classes from the outer world (Kotlin prism-sdk classes for example etc.)
  */
object GeneralLoggableInstances {

  implicit val didLoggable: DictLoggable[DID] = new DictLoggable[DID] {
    override def fields[I, V, R, S](a: DID, i: I)(implicit r: LogRenderer[I, V, R, S]): R = {
      r.addString("DID", a.getValue, i)
    }

    override def logShow(a: DID): String = s"{DID=$a}"
  }

  implicit val didSuffixLoggable: DictLoggable[DIDSuffix] = new DictLoggable[DIDSuffix] {
    override def fields[I, V, R, S](a: DIDSuffix, i: I)(implicit r: LogRenderer[I, V, R, S]): R = {
      r.addString("DIDSuffix", a.getValue, i)
    }

    override def logShow(a: DIDSuffix): String = s"{DIDSuffix=${a.getValue}}"
  }

  implicit val credentialBatchIdLoggable: DictLoggable[CredentialBatchId] = new DictLoggable[CredentialBatchId] {
    override def fields[I, V, R, S](a: CredentialBatchId, i: I)(implicit r: LogRenderer[I, V, R, S]): R = {
      r.addString("CredentialBatchId", a.getId, i)
    }

    override def logShow(a: CredentialBatchId): String = s"{CredentialBatchId=$a}"
  }

}
