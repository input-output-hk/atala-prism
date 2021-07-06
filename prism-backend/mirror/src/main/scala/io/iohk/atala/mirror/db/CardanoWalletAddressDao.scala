package io.iohk.atala.mirror.db

import cats.data.NonEmptyList
import doobie.{FC, Fragments}
import doobie.util.update.Update
import doobie.free.connection.ConnectionIO
import io.iohk.atala.mirror.models.{
  CardanoAddress,
  CardanoWallet,
  CardanoWalletAddress,
  CardanoWalletAddressWithWalletName
}
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.models.ConnectionToken
import cats.implicits._
import io.iohk.atala.prism.utils.PostgresWhereInSplitter

object CardanoWalletAddressDao {

  def findBy(walletId: CardanoWallet.Id): ConnectionIO[List[CardanoWalletAddress]] = {
    sql"""
         | SELECT address, wallet_id, sequence_no, used_at
         | FROM cardano_wallet_addresses
         | WHERE wallet_id = $walletId""".stripMargin
      .query[CardanoWalletAddress]
      .to[List]
  }

  def findAddresses(addresses: List[CardanoAddress]): doobie.ConnectionIO[List[CardanoWalletAddress]] =
    PostgresWhereInSplitter.splitQueryExecution(addresses, findAddresses)

  private def findAddresses(
      nonEmptyAddresses: NonEmptyList[CardanoAddress]
  ): doobie.ConnectionIO[List[CardanoWalletAddress]] =
    (fr"""
     | SELECT address, wallet_id, sequence_no, used_at
     | FROM cardano_wallet_addresses
     | WHERE""".stripMargin ++ Fragments.in(fr"address", nonEmptyAddresses))
      .query[CardanoWalletAddress]
      .to[List]

  def findByConnectionTokenWithWalletName(
      connectionToken: ConnectionToken
  ): ConnectionIO[List[CardanoWalletAddressWithWalletName]] = {
    sql"""
         | SELECT a.address, a.wallet_id, a.sequence_no, a.used_at, w.name
         | FROM cardano_wallets w
         | JOIN cardano_wallet_addresses a
         | ON w.id = a.wallet_id
         | WHERE w.connection_token = $connectionToken
         | ORDER BY a.sequence_no""".stripMargin
      .query[CardanoWalletAddressWithWalletName]
      .to[List]
  }

  def updateUsedAt(address: CardanoAddress, usedAt: CardanoWalletAddress.UsedAt): doobie.ConnectionIO[Unit] = {
    sql"""
         | UPDATE cardano_wallet_addresses SET
         | used_at = ${Some(usedAt)}
         | WHERE address = $address
       """.stripMargin.update.run
      .flatTap(n =>
        FC.raiseError(
            new RuntimeException(
              s"Error updating cardano wallet address: $address, expected result count: 1, actual $n"
            )
          )
          .whenA(n != 1)
      )
      .void
  }

  def insert(cardanoWalletAddress: CardanoWalletAddress): ConnectionIO[Int] =
    insertMany.toUpdate0(cardanoWalletAddress).run

  val insertMany: Update[CardanoWalletAddress] =
    Update[CardanoWalletAddress](
      """INSERT INTO
        | cardano_wallet_addresses(address, wallet_id, sequence_no, used_at)
        | values (?, ?, ?, ?)""".stripMargin
    )
}
