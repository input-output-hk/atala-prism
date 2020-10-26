package io.iohk.atala.mirror.db

import doobie.util.update.Update
import doobie.free.connection.ConnectionIO
import io.iohk.atala.mirror.models.{CardanoAddressInfo, ConnectorMessageId}
import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.mirror.models.CardanoAddressInfo.CardanoAddress

object CardanoAddressInfoDao {

  def findBy(address: CardanoAddress): ConnectionIO[Option[CardanoAddressInfo]] = {
    sql"""
         | SELECT address, connection_token, registration_date, message_id
         | FROM cardano_addresses_info
         | WHERE address = $address
    """.stripMargin.query[CardanoAddressInfo].option
  }

  val findLastSeenMessageId: ConnectionIO[Option[ConnectorMessageId]] =
    sql"""
         | SELECT message_id
         | FROM cardano_addresses_info
         | ORDER BY registration_date DESC
         | LIMIT 1
    """.stripMargin.query[ConnectorMessageId].option

  def insert(cardanoAddress: CardanoAddressInfo): ConnectionIO[Int] =
    insertMany.toUpdate0(cardanoAddress).run

  val insertMany: Update[CardanoAddressInfo] =
    Update[CardanoAddressInfo](
      """INSERT INTO
        | cardano_addresses_info(address, connection_token, registration_date, message_id)
        | values (?, ?, ?, ?)""".stripMargin
    )
}
