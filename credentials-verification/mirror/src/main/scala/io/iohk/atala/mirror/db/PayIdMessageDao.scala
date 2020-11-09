package io.iohk.atala.mirror.db

import doobie.util.update.Update
import doobie.free.connection.ConnectionIO
import io.iohk.atala.mirror.models.{ConnectorMessageId, PayIdMessage}
import doobie.implicits._

object PayIdMessageDao {

  def findBy(connectorMessageId: ConnectorMessageId): ConnectionIO[Option[PayIdMessage]] = {
    sql"""
         | SELECT connector_message_id, raw_message
         | FROM payid_raw_messages
         | WHERE connector_message_id = $connectorMessageId
    """.stripMargin.query[PayIdMessage].option
  }

  def insert(payIdMessage: PayIdMessage): ConnectionIO[Int] =
    insertMany.toUpdate0(payIdMessage).run

  val insertMany: Update[PayIdMessage] =
    Update[PayIdMessage](
      """INSERT INTO
        | payid_raw_messages(connector_message_id, raw_message)
        | values (?, ?)""".stripMargin
    )

}
