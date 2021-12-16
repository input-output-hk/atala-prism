package io.iohk.atala.prism.management.console.repositories

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.management.console.DataPreparation._
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.daos.CredentialTypeCategoryDao
import io.iohk.atala.prism.utils.IOUtils._
import org.scalatest.OptionValues._
import tofu.logging.Logs
import doobie.implicits._

//sbt "project management-console" "testOnly *CredentialTypeRepositorySpec"
class CredentialTypeCategoryRepositorySpec extends AtalaWithPostgresSpec {

  val logs: Logs[IO, IO] = Logs.sync[IO, IO]

  lazy val repository = CredentialTypeCategoryRepository.unsafe(database, logs)

  "create" should {
    "create credential type category" in {
      val participantId = createParticipant("Institution-1")
      val createCredentialTypeCategory =
        CreateCredentialTypeCategory("some category", CredentialTypeCategoryState.Ready)

      val created = repository.create(participantId, createCredentialTypeCategory).unsafeRunSync().toOption.value

      val found = CredentialTypeCategoryDao
        .find(participantId)
        .transact(database)
        .unsafeRunSync()
        .head

      created mustBe found
    }
  }

  "findByInstitution" should {
    "find all credential type categories of a given institution" in {
      val participantId = createParticipant("Institution-1")
      val createCredentialTypeCategory1 =
        CreateCredentialTypeCategory("some category1", CredentialTypeCategoryState.Ready)

      val createCredentialTypeCategory2 =
        CreateCredentialTypeCategory("some category1", CredentialTypeCategoryState.Ready)

      val created = Seq(
        CredentialTypeCategoryDao.create(participantId, createCredentialTypeCategory1),
        CredentialTypeCategoryDao.create(participantId, createCredentialTypeCategory2)
      ).map(_.transact(database).unsafeRunSync())

      val found = repository.findByInstitution(participantId).unsafeRunSync().toOption.value

      created mustBe found

    }
  }

  "archive" should {
    "change the state of the credential to archived" in {
      val participantId = createParticipant("Institution-1")
      val createCredentialTypeCategory1 =
        CreateCredentialTypeCategory("some category1", CredentialTypeCategoryState.Ready)

      val created = CredentialTypeCategoryDao
        .create(participantId, createCredentialTypeCategory1)
        .transact(database)
        .unsafeRunSync()

      created.state mustBe CredentialTypeCategoryState.Ready

      val updated = repository.archive(created.id).unsafeRunSync().toOption.value

      updated.state mustBe CredentialTypeCategoryState.Archived
    }
  }

  "unArchive" should {
    "change the state of the credential from archived to ready" in {
      val participantId = createParticipant("Institution-1")
      val createCredentialTypeCategory1 =
        CreateCredentialTypeCategory("some category1", CredentialTypeCategoryState.Archived)

      val created = CredentialTypeCategoryDao
        .create(participantId, createCredentialTypeCategory1)
        .transact(database)
        .unsafeRunSync()

      created.state mustBe CredentialTypeCategoryState.Archived

      val updated = repository.unArchive(created.id).unsafeRunSync().toOption.value

      updated.state mustBe CredentialTypeCategoryState.Ready
    }
  }
}
