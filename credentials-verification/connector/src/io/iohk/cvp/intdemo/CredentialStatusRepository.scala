package io.iohk.cvp.intdemo

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor

import scala.concurrent.{ExecutionContext, Future}

class CredentialStatusRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  def merge(token: String, status: Int): Future[Int] = {
    CredentialStatusRepository
      .merge(token, status)
      .transact(xa)
      .unsafeToFuture()
  }

  def find(token: String): Future[Option[Int]] = {
    CredentialStatusRepository
      .find(token)
      .transact(xa)
      .unsafeToFuture()
  }
}

object CredentialStatusRepository {

  case class DatabaseError(databaseReportedError: Throwable) {
    override def toString: String = s"Database error: $databaseReportedError"
  }

  def merge(
      token: String,
      status: Int
  ): doobie.ConnectionIO[Int] = {
    sql"""
         |INSERT INTO intdemo_credential_status(token, status)
         |VALUES ($token, $status)
         |ON CONFLICT (token) DO
         |UPDATE SET status = $status
         |""".stripMargin.update.run
  }

  def find(token: String): doobie.ConnectionIO[Option[Int]] = {
    sql"""
         |SELECT status FROM intdemo_credential_status
         |WHERE token = $token
       """.stripMargin
      .query[Int]
      .option
  }
}
