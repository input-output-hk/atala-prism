package io.iohk.atala.mirror.db

import cats.data.NonEmptyList
import doobie.Fragments
import doobie.free.connection
import doobie.implicits._
import io.iohk.atala.mirror.models.{CardanoAddress, CardanoAddressWithUsageInfo}
import doobie.implicits.legacy.instant._

object CardanoDBSyncDao {

  def findUsedAddresses(addresses: List[CardanoAddress]): doobie.ConnectionIO[List[CardanoAddressWithUsageInfo]] = {
    NonEmptyList
      .fromList(addresses)
      .map { nonEmptyAddresses =>
        (fr"""
           | SELECT tx_out.address, block.time
           | FROM tx_out
           | JOIN tx ON tx_out.tx_id = tx.id
           | JOIN block ON block.id = tx.block_id
           | WHERE""".stripMargin ++ Fragments.in(fr"tx_out.address", nonEmptyAddresses))
          .query[CardanoAddressWithUsageInfo]
          .to[List]
      }
      .getOrElse(connection.pure(List.empty))

  }

}
