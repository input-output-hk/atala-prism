package io.iohk.atala.prism.admin

import cats.effect.IO
import cats.implicits._
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.util.update.Update0
import io.iohk.atala.prism.admin.AdminRepository.{log, _}
import io.iohk.atala.prism.admin.Errors.AdminError.DatabaseError
import io.iohk.atala.prism.admin.Splitter.sqlSplit
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither._
import io.iohk.cvp.utils.Using.using
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.io.{BufferedSource, Source}

class AdminRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  def insertDemoDataset(): FutureEither[DatabaseError, List[Int]] = {
    (deletes ++ inserts)
      .traverse(update => dbUpdate(update))
      .transact(xa)
      .attempt
      .unsafeToFuture()
      .toFutureEither
      .mapLeft(DatabaseError)
  }

  private def dbUpdate(stmt: String): doobie.ConnectionIO[Int] = {
    log.info(stmt)
    Update0(stmt, None).run
  }
}

object AdminRepository {

  val log = LoggerFactory.getLogger(classOf[AdminRepository])

  val deletes = List(
    "delete from messages",
    "delete from connections",
    "delete from connection_tokens",
    "delete from credentials",
    "delete from holder_public_keys",
    "delete from issuer_subjects",
    "delete from payments",
    "delete from issuer_groups",
    "delete from issuers",
    "delete from participants",
    "delete from verifier_holders",
    "delete from verifiers",
    "delete from stored_credentials"
  )

  val inserts = using(fakeDataScriptAsSource)(source => sqlSplit(source.mkString).toList)

  private def fakeDataScriptAsSource: BufferedSource = {
    Source.fromInputStream(getClass.getClassLoader.getResourceAsStream("fake_data.sql"))
  }
}
