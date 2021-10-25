package io.iohk.atala.prism.intdemo

import java.time.LocalDate

import cats.effect.IO
import doobie.implicits._
import doobie.implicits.legacy.localdate._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.intdemo.IntDemoRepository.valueOf
import io.iohk.atala.prism.connector.model.TokenString
import io.iohk.atala.prism.intdemo.protos.intdemo_models

import scala.concurrent.Future

class IntDemoRepository(xa: Transactor[IO]) {

  def mergeSubjectStatus(
      token: TokenString,
      status: intdemo_models.SubjectStatus
  ): Future[Int] = {
    IntDemoRepository
      .mergeStatus(token.token, valueOf(status))
      .transact(xa)
      .unsafeToFuture()
  }

  def findSubjectStatus(
      token: TokenString
  ): Future[Option[intdemo_models.SubjectStatus]] = {
    IntDemoRepository
      .findStatus(token.token)
      .transact(xa)
      .unsafeToFuture()
  }

  def mergePersonalInfo(
      token: TokenString,
      firstName: String,
      dateOfBirth: LocalDate
  ): Future[Int] = {
    IntDemoRepository
      .mergePersonalInfo(token.token, firstName, dateOfBirth)
      .transact(xa)
      .unsafeToFuture()
  }

  def findPersonalInfo(
      token: TokenString
  ): Future[Option[(String, LocalDate)]] = {
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

  def valueOf(status: intdemo_models.SubjectStatus): Int =
    status match {
      case intdemo_models.SubjectStatus.UNCONNECTED =>
        intdemo_models.SubjectStatus.UNCONNECTED.value
      case intdemo_models.SubjectStatus.CONNECTED =>
        intdemo_models.SubjectStatus.CONNECTED.value
      case intdemo_models.SubjectStatus.CREDENTIAL_AVAILABLE =>
        intdemo_models.SubjectStatus.CREDENTIAL_AVAILABLE.value
      case intdemo_models.SubjectStatus.CREDENTIAL_SENT =>
        intdemo_models.SubjectStatus.CREDENTIAL_SENT.value
      case intdemo_models.SubjectStatus.CREDENTIAL_RECEIVED =>
        intdemo_models.SubjectStatus.CREDENTIAL_RECEIVED.value
      case intdemo_models.SubjectStatus.Unrecognized(value) => value
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

  def findStatus(
      token: String
  ): doobie.ConnectionIO[Option[intdemo_models.SubjectStatus]] = {
    sql"""
         |SELECT status FROM intdemo_credential_status
         |WHERE token = $token
       """.stripMargin
      .query[Int]
      .map(intdemo_models.SubjectStatus.fromValue)
      .option
  }

  def findPersonalInfo(
      token: String
  ): doobie.ConnectionIO[Option[(String, LocalDate)]] = {
    sql"""
         |SELECT first_name, date_of_birth
         |FROM intdemo_id_personal_info
         |WHERE token = $token
         |""".stripMargin.query[(String, LocalDate)].option
  }
}
