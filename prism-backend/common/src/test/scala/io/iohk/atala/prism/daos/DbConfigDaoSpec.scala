package io.iohk.atala.prism.daos

import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec

// sbt "project common" "testOnly *daos.DbConfigDaoSpec"
class DbConfigDaoSpec extends AtalaWithPostgresSpec {

  override protected def migrationScriptsLocation: String = "common/db/migration"

  "DbConfigDao" should {
    "return None if a key doesn't exist" in {
      DbConfigDao.get("key").transact(database).unsafeRunSync() mustBe None
    }

    "set once and return valye by key" in {
      // when
      val value = (for {
        _ <- DbConfigDao.setIfNotExists("key", "value")
        value <- DbConfigDao.get("key")
      } yield value)
        .transact(database)
        .unsafeRunSync()

      // then
      value mustBe Some("value")
    }

    "update when key exists" in {
      // when
      val value = (for {
        _ <- DbConfigDao.setOrUpdate("key", "value")
        _ <- DbConfigDao.setOrUpdate("key", "updated value")
        value <- DbConfigDao.get("key")
      } yield value)
        .transact(database)
        .unsafeRunSync()

      // then
      value mustBe Some("updated value")
    }

    "return previous value when key exists" in {
      // when
      val value = (for {
        _ <- DbConfigDao.setIfNotExists("key", "value")
        _ <- DbConfigDao.setIfNotExists("key", "updated value")
        value <- DbConfigDao.get("key")
      } yield value)
        .transact(database)
        .unsafeRunSync()

      // then
      value mustBe Some("value")
    }
  }
}
