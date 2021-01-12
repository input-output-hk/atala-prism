package io.iohk.atala.prism.daos

import doobie.free.connection.ConnectionIO
import io.iohk.atala.prism.models.ConnectorMessageId
import doobie.implicits._

object ConnectorMessageOffsetDao {

  def findLastMessageOffset(): ConnectionIO[Option[ConnectorMessageId]] = {
    sql"""
    | SELECT message_id
    | FROM connector_message_offset
    | LIMIT 1
    """.stripMargin.query[ConnectorMessageId].option
  }

  def updateLastMessageOffset(connectorMessageId: ConnectorMessageId): ConnectionIO[Int] =
    sql"""
    | INSERT INTO connector_message_offset(id, message_id)
    | VALUES (1, ${connectorMessageId.messageId})
    | ON CONFLICT(id) DO UPDATE SET message_id = ${connectorMessageId.messageId}
    """.stripMargin.update.run
}
