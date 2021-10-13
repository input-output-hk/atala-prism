package io.iohk.atala.prism.node.services

import io.iohk.atala.prism.models.TransactionInfo
import io.iohk.atala.prism.protos.node_internal
import tofu.logging.{DictLoggable, LogRenderer}

import scala.concurrent.Future
import cats.syntax.semigroup._

package object models {
  case class AtalaObjectNotification(
      atalaObject: node_internal.AtalaObject,
      transaction: TransactionInfo
  )

  object AtalaObjectNotification {
    implicit val atalaObjectNotificationLoggable: DictLoggable[AtalaObjectNotification] =
      new DictLoggable[AtalaObjectNotification] {
        override def fields[I, V, R, S](a: AtalaObjectNotification, i: I)(implicit r: LogRenderer[I, V, R, S]): R = {
          r.addString("atalaObject", a.atalaObject.toProtoString, i) |+| r.addString(
            "transaction",
            a.transaction.toString,
            i
          )
        }

        override def logShow(a: AtalaObjectNotification): String = s"AtalaOperationId{${a.atalaObject.toProtoString}"
      }
  }

  type AtalaObjectNotificationHandler = AtalaObjectNotification => Future[Unit]
}
