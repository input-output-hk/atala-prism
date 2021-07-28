package io.iohk.atala.prism.logging

import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.identity.DID
import tofu.logging._

/**
  * Used for libraries classes from the outer world (Kotlin prism-sdk classes for example etc.)
  */
object GeneralLoggableInstances {

  implicit val SHA256DigestLoggable: DictLoggable[SHA256Digest] = new DictLoggable[SHA256Digest] {
    override def fields[I, V, R, S](a: SHA256Digest, i: I)(implicit r: LogRenderer[I, V, R, S]): R = {
      r.addString("SHA256Digest", a.hexValue, i)
    }

    override def logShow(a: SHA256Digest): String = s"{SHA256Digest=${a.hexValue}}"
  }

  implicit val userLoggable: DictLoggable[DID] = new DictLoggable[DID] {
    override def fields[I, V, R, S](a: DID, i: I)(implicit r: LogRenderer[I, V, R, S]): R = {
      r.addString("DID", a.value, i)
    }

    override def logShow(a: DID): String = s"{DID=$a}"
  }
}
