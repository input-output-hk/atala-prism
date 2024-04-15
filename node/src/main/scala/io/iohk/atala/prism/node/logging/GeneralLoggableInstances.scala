package io.iohk.atala.prism.node.logging

import io.grpc.Status
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.node.models.DidSuffix
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import tofu.logging._
import tofu.syntax.monoid.TofuSemigroupOps

/** Used for libraries classes from the outer world (Kotlin prism-sdk classes for example etc.)
  */
object GeneralLoggableInstances {

  implicit val statusLoggable = new DictLoggable[Status] {
    override def fields[I, V, R, S](a: Status, i: I)(implicit r: LogRenderer[I, V, R, S]): R = {
      r.addString("grpc_status", a.toString, i)
    }

    override def logShow(a: Status): String = {
      val description = Option(a.getDescription).map(v => s", description = $v").getOrElse("")
      val cause = Option(a.getCause).map(v => s", cause = ${v.getMessage}").getOrElse("")
      s"code = ${a.getCode}${description}${cause}"
    }
  }

  implicit val didLoggable: DictLoggable[DID] = new DictLoggable[DID] {
    override def fields[I, V, R, S](a: DID, i: I)(implicit
        r: LogRenderer[I, V, R, S]
    ): R = {
      r.addString("PrismDID", a.getValue, i)
    }

    override def logShow(a: DID): String = s"{PrismDID=$a}"
  }

  implicit val didSuffixLoggable: DictLoggable[DidSuffix] =
    new DictLoggable[DidSuffix] {
      override def fields[I, V, R, S](a: DidSuffix, i: I)(implicit
          r: LogRenderer[I, V, R, S]
      ): R = {
        r.addString("DIDSuffix", a.value, i)
      }

      override def logShow(a: DidSuffix): String = s"{DIDSuffix=${a.value}}"
    }

  implicit val ecPublicKeyLoggable: DictLoggable[ECPublicKey] =
    new DictLoggable[ECPublicKey] {
      override def fields[I, V, R, S](a: ECPublicKey, i: I)(implicit
          r: LogRenderer[I, V, R, S]
      ): R = {
        r.addString("x", a.getCurvePoint.getX.toString, i) |+| r.addString(
          "y",
          a.getCurvePoint.getY.toString,
          i
        )
      }

      override def logShow(a: ECPublicKey): String =
        s"{ECPublicKey={x=${a.getCurvePoint.getX.toString}, y=${a.getCurvePoint.getY.toString}"
    }

}
