package io.iohk.atala.mirror.db

import doobie.util.update.Update
import doobie.free.connection.ConnectionIO
import io.iohk.atala.mirror.models.CardanoWallet

import doobie.implicits._
import doobie.postgres.implicits._
import doobie.implicits.legacy.instant._

object CardanoWalletDao {

  def findByName(name: String): ConnectionIO[Option[CardanoWallet]] = {
    sql"""
         | SELECT id, name, connection_token, extended_public_key, last_generated_no, last_used_no, registration_date
         | FROM cardano_wallets
         | WHERE name = $name""".stripMargin
      .query[CardanoWallet]
      .option
  }

  def insert(cardanoWallet: CardanoWallet): ConnectionIO[CardanoWallet.Id] =
    insertMany
      .toUpdate0(cardanoWallet)
      .withUniqueGeneratedKeys[CardanoWallet.Id]("id")

  val insertMany: Update[CardanoWallet] =
    Update[CardanoWallet](
      """INSERT INTO
        | cardano_wallets(id, name, connection_token, extended_public_key, last_generated_no, last_used_no, registration_date)
        | values (?, ?, ?, ?, ?, ?, ?)""".stripMargin
    )
}
