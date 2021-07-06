package io.iohk.atala.mirror.db

import cats.data.NonEmptyList
import doobie.util.update.Update
import doobie.free.connection.ConnectionIO
import io.iohk.atala.mirror.models.CardanoWallet
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.implicits.legacy.instant._
import cats.implicits._
import doobie.free.connection
import doobie.{FC, Fragments}

object CardanoWalletDao {

  private val selectCardanoWallet =
    fr"""
      | SELECT id, name, connection_token, extended_public_key, last_generated_no, last_used_no, registration_date
      | FROM cardano_wallets
      """.stripMargin

  def findByName(name: String): ConnectionIO[Option[CardanoWallet]] = {
    (selectCardanoWallet ++ fr"WHERE name = $name")
      .query[CardanoWallet]
      .option
  }

  def findById(id: CardanoWallet.Id): ConnectionIO[Option[CardanoWallet]] = {
    (selectCardanoWallet ++ fr"WHERE id = $id")
      .query[CardanoWallet]
      .option
  }

  def findByIds(ids: List[CardanoWallet.Id]): doobie.ConnectionIO[List[CardanoWallet]] = {
    NonEmptyList
      .fromList(ids)
      .map { nonEmptyIds =>
        (selectCardanoWallet ++ fr"WHERE" ++ Fragments.in(fr"id", nonEmptyIds))
          .query[CardanoWallet]
          .to[List]
      }
      .getOrElse(connection.pure(List.empty))
  }

  def insert(cardanoWallet: CardanoWallet): ConnectionIO[CardanoWallet.Id] =
    insertMany
      .toUpdate0(cardanoWallet)
      .withUniqueGeneratedKeys[CardanoWallet.Id]("id")

  def updateLastUsedNo(cardanoWalletId: CardanoWallet.Id, lastUsedNo: Int): doobie.ConnectionIO[Unit] = {
    sql"""
    | UPDATE cardano_wallets SET
    | last_used_no = $lastUsedNo
    | WHERE id = $cardanoWalletId
       """.stripMargin.update.run.flatTap(n => ensureOneRecordUpdated(n, cardanoWalletId)).void
  }

  def updateLastGeneratedNo(cardanoWalletId: CardanoWallet.Id, lastGeneratedNo: Int): doobie.ConnectionIO[Unit] = {
    sql"""
    | UPDATE cardano_wallets SET
    | last_generated_no = $lastGeneratedNo
    | WHERE id = $cardanoWalletId
       """.stripMargin.update.run.flatTap(n => ensureOneRecordUpdated(n, cardanoWalletId)).void
  }

  def updateLastGeneratedAndUsedNo(
      cardanoWalletId: CardanoWallet.Id,
      lastUsedNo: Int,
      lastGeneratedNo: Int
  ): doobie.ConnectionIO[Unit] = {
    sql"""
    | UPDATE cardano_wallets SET
    | last_used_no = $lastUsedNo,
    | last_generated_no = $lastGeneratedNo
    | WHERE id = $cardanoWalletId
      """.stripMargin.update.run.flatTap(n => ensureOneRecordUpdated(n, cardanoWalletId)).void
  }

  private def ensureOneRecordUpdated(n: Int, cardanoWalletId: CardanoWallet.Id): ConnectionIO[Unit] = {
    FC.raiseError(
        new RuntimeException(
          s"Error updating cardano wallet with id: $cardanoWalletId, expected result count: 1, actual $n"
        )
      )
      .whenA(n != 1)
  }

  val insertMany: Update[CardanoWallet] =
    Update[CardanoWallet](
      """INSERT INTO
        | cardano_wallets(id, name, connection_token, extended_public_key, last_generated_no, last_used_no, registration_date)
        | values (?, ?, ?, ?, ?, ?, ?)""".stripMargin
    )
}
