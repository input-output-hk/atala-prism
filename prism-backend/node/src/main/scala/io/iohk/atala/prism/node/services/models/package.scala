package io.iohk.atala.prism.node.services

import io.iohk.atala.prism.models.TransactionInfo
import io.iohk.atala.prism.protos.node_internal

import scala.concurrent.Future

package object models {
  case class AtalaObjectNotification(
      atalaObject: node_internal.AtalaObject,
      transaction: TransactionInfo
  )

  type AtalaObjectNotificationHandler = AtalaObjectNotification => Future[Unit]
}
