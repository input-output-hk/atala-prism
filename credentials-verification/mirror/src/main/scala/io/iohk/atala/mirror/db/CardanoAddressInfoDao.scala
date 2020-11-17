package io.iohk.atala.mirror.db

import doobie.util.update.Update
import doobie.free.connection.ConnectionIO
import io.iohk.atala.mirror.models.{CardanoAddressInfo, ConnectorMessageId}
import doobie.implicits._
import io.iohk.atala.mirror.models.CardanoAddressInfo.CardanoNetwork
import io.iohk.atala.mirror.models.Connection.ConnectionToken
import doobie.implicits.legacy.instant._
import doobie.util.meta.Meta
import io.iohk.atala.mirror.models.CardanoAddressInfo.CardanoAddress
import io.iohk.atala.prism.mirror.payid.Address.VerifiedAddress
import io.iohk.atala.prism.mirror.payid.implicits._
import io.iohk.atala.prism.jose.implicits._

object CardanoAddressInfoDao {

  implicit val verifiedAddressMeta: Meta[VerifiedAddress] = Metas.circeMeta[VerifiedAddress]

  def findBy(address: CardanoAddress): ConnectionIO[Option[CardanoAddressInfo]] = {
    sql"""
         | SELECT address, payid_verified_address, network, connection_token, registration_date, message_id
         | FROM cardano_addresses_info
         | WHERE address = $address
    """.stripMargin.query[CardanoAddressInfo].option
  }

  def findBy(
      connectionToken: ConnectionToken,
      cardanoNetwork: CardanoNetwork
  ): ConnectionIO[List[CardanoAddressInfo]] = {
    sql"""
         | SELECT address, payid_verified_address, network, connection_token, registration_date, message_id
         | FROM cardano_addresses_info
         | WHERE connection_token = $connectionToken AND network = $cardanoNetwork
    """.stripMargin.query[CardanoAddressInfo].to[List]
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
        | cardano_addresses_info(address, payid_verified_address, network, connection_token, registration_date, message_id)
        | values (?, ?, ?, ?, ?, ?)""".stripMargin
    )
}
