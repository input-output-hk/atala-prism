package io.iohk.atala.prism.repositories.ops

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.util.{Read, fragment}

object SqlTestOps {
  implicit class Implicits(val sql: fragment.Fragment) {
    def runUpdate()(implicit
        database: Transactor[IO]
    ): Unit = {
      sql.update.run.transact(database).unsafeRunSync()
      ()
    }

    def runUnique[T: Read]()(implicit
        database: Transactor[IO]
    ): T = {
      sql.query[T].unique.transact(database).unsafeRunSync()
    }

    def queryList[T: Read]()(implicit
        database: Transactor[IO]
    ): Seq[T] = {
      sql.query[T].to[Seq].transact(database).unsafeRunSync()
    }
  }
}
