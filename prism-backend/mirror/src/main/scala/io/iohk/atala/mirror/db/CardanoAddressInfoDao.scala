package io.iohk.atala.mirror.db

import cats.data.NonEmptyList
import doobie.Fragments
import doobie.util.update.Update
import doobie.free.connection.ConnectionIO
import io.iohk.atala.mirror.models.CardanoAddressInfo
import doobie.implicits._
import io.iohk.atala.mirror.models.CardanoAddressInfo.CardanoNetwork
import io.iohk.atala.prism.models.{ConnectionToken, ConnectorMessageId}
import doobie.implicits.legacy.instant._
import doobie.util.meta.Meta
import io.iohk.atala.mirror.models.CardanoAddress
import io.iohk.atala.prism.mirror.payid.Address.VerifiedAddress
import io.iohk.atala.prism.mirror.payid.implicits._
import io.iohk.atala.prism.jose.implicits._
import io.iohk.atala.prism.utils.DoobieImplicits

object CardanoAddressInfoDao {

  implicit val verifiedAddressMeta: Meta[VerifiedAddress] = DoobieImplicits.circeMeta[VerifiedAddress]

  def findBy(addresses: NonEmptyList[CardanoAddress]): ConnectionIO[List[CardanoAddressInfo]] = {
    (fr"""
         | SELECT address, payid_verified_address, network, connection_token, registration_date, message_id
         | FROM cardano_addresses_info
         | WHERE""".stripMargin ++ Fragments.in(fr"address", addresses))
      .query[CardanoAddressInfo]
      .to[List]
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

  def findBy(
      connectionToken: ConnectionToken
  ): ConnectionIO[List[CardanoAddressInfo]] = {
    sql"""
         | SELECT address, payid_verified_address, network, connection_token, registration_date, message_id
         | FROM cardano_addresses_info
         | WHERE connection_token = $connectionToken
         | ORDER BY registration_date, address
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
