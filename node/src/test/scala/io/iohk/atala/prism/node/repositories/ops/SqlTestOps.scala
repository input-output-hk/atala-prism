package io.iohk.atala.prism.node.repositories.ops

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.Read
import doobie.util.fragment

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

  implicit class ConnectionIOImplicits[A](val cio: doobie.ConnectionIO[A]) {
    def unsafeRun()(implicit database: Transactor[IO]): A = {
      cio.transact(database).unsafeRunSync()
    }
  }
}
