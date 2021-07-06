package io.iohk.atala.mirror.db

import cats.data.NonEmptyList
import doobie.Fragments
import doobie.free.connection
import doobie.implicits._
import io.iohk.atala.mirror.models.{CardanoAddress, CardanoAddressBlockInfo, CardanoBlockId}
import doobie.implicits.legacy.instant._
import doobie.util.fragment

object CardanoDBSyncDao {

  private val selectCardanoAddressBlockInfo: fragment.Fragment =
    fr"""
      | SELECT tx_out.address, block.time, block.id
      | FROM tx_out
      | JOIN tx ON tx_out.tx_id = tx.id
      | JOIN block ON block.id = tx.block_id""".stripMargin

  def findUsedAddresses(addresses: List[CardanoAddress]): doobie.ConnectionIO[List[CardanoAddressBlockInfo]] = {
    NonEmptyList
      .fromList(addresses)
      .map { nonEmptyAddresses =>
        (selectCardanoAddressBlockInfo ++ fr"WHERE" ++ Fragments.in(fr"tx_out.address", nonEmptyAddresses))
          .query[CardanoAddressBlockInfo]
          .to[List]
      }
      .getOrElse(connection.pure(List.empty))

  }

  def findAddressesInBlockWithLastBlockId(
      lastFoundBlockId: CardanoBlockId,
      limit: Int
  ): doobie.ConnectionIO[(Option[CardanoBlockId], List[CardanoAddressBlockInfo])] = {
    for {
      lastBlockId <- findLastBlockId(lastFoundBlockId, limit)
      addresses <- findAddressInBlocks(lastFoundBlockId, limit)
    } yield (lastBlockId, addresses)
  }

  def findLastBlockId(
      lastFoundBlockId: CardanoBlockId,
      limit: Int
  ): doobie.ConnectionIO[Option[CardanoBlockId]] = {
    (fr"SELECT max(id) FROM block WHERE id > ${lastFoundBlockId} AND id <= ${lastFoundBlockId.id + limit}")
      .query[CardanoBlockId]
      .option
  }

  def findAddressInBlocks(
      lastFoundBlockId: CardanoBlockId,
      limit: Int
  ): doobie.ConnectionIO[List[CardanoAddressBlockInfo]] = {
    (selectCardanoAddressBlockInfo ++ fr"WHERE block.id > ${lastFoundBlockId} AND block.id <= ${lastFoundBlockId.id + limit}")
      .query[CardanoAddressBlockInfo]
      .to[List]
  }

}
