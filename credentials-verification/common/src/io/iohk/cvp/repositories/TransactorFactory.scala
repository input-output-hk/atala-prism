package io.iohk.cvp.repositories

import cats.effect.{ContextShift, IO}
import doobie.util.transactor.Transactor

import scala.concurrent.ExecutionContext

object TransactorFactory {

  case class Config(username: String, password: String, jdbcUrl: String)

  // TODO: Use HikariTranasctor
  def apply(config: Config): Transactor[IO] = {
    // We need a ContextShift[IO] before we can construct a Transactor[IO]. The passed ExecutionContext
    // is where nonblocking operations will be executed.
    implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

    // A transactor that gets connections from java.sql.DriverManager and excutes blocking operations
    // on an unbounded pool of daemon threads. See the chapter on connection handling for more info.
    Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      config.jdbcUrl,
      config.username,
      config.password
    )
  }
}
