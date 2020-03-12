package io.iohk.cvp.intdemo

import java.time.LocalDate

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.connector.model.TokenString
import io.iohk.cvp.intdemo.IntDemoRepository.valueOf
import io.iohk.cvp.intdemo.protos.SubjectStatus
import io.iohk.cvp.intdemo.protos.SubjectStatus.{
  CONNECTED,
  CREDENTIAL_AVAILABLE,
  CREDENTIAL_RECEIVED,
  CREDENTIAL_SENT,
  UNCONNECTED,
  Unrecognized
}

import scala.concurrent.{ExecutionContext, Future}

class IntDemoRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  def mergeSubjectStatus(token: TokenString, status: SubjectStatus): Future[Int] = {
    IntDemoRepository
      .mergeStatus(token.token, valueOf(status))
      .transact(xa)
      .unsafeToFuture()
  }

  def findSubjectStatus(token: TokenString): Future[Option[SubjectStatus]] = {
    IntDemoRepository
      .findStatus(token.token)
      .transact(xa)
      .unsafeToFuture()
  }

  def mergePersonalInfo(token: TokenString, firstName: String, dateOfBirth: LocalDate): Future[Int] = {
    IntDemoRepository
      .mergePersonalInfo(token.token, firstName, dateOfBirth)
      .transact(xa)
      .unsafeToFuture()
  }

  def findPersonalInfo(token: TokenString): Future[Option[(String, LocalDate)]] = {
    IntDemoRepository
      .findPersonalInfo(token.token)
      .transact(xa)
      .unsafeToFuture()
  }
}

object IntDemoRepository {

  case class DatabaseError(databaseReportedError: Throwable) {
    override def toString: String = s"Database error: $databaseReportedError"
  }

  def valueOf(status: SubjectStatus): Int = status match {
    case UNCONNECTED => UNCONNECTED.value
    case CONNECTED => CONNECTED.value
    case CREDENTIAL_AVAILABLE => CREDENTIAL_AVAILABLE.value
    case CREDENTIAL_SENT => CREDENTIAL_SENT.value
    case CREDENTIAL_RECEIVED => CREDENTIAL_RECEIVED.value
    case Unrecognized(value) => value
  }

  def mergeStatus(
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

  def mergePersonalInfo(
      token: String,
      firstName: String,
      dateOfBirth: LocalDate
  ): doobie.ConnectionIO[Int] = {
    sql"""
         |INSERT INTO intdemo_id_personal_info(token, first_name, date_of_birth)
         |VALUES ($token, $firstName, $dateOfBirth)
         |ON CONFLICT (token) DO
         |UPDATE SET first_name = $firstName, date_of_birth = $dateOfBirth
         |""".stripMargin.update.run

  }

  def findStatus(token: String): doobie.ConnectionIO[Option[SubjectStatus]] = {
    sql"""
         |SELECT status FROM intdemo_credential_status
         |WHERE token = $token
       """.stripMargin
      .query[Int]
      .map(SubjectStatus.fromValue)
      .option
  }

  def findPersonalInfo(token: String): doobie.ConnectionIO[Option[(String, LocalDate)]] = {
    sql"""
         |SELECT first_name, date_of_birth
         |FROM intdemo_id_personal_info
         |WHERE token = $token
         |""".stripMargin.query[(String, LocalDate)].option
  }
}
