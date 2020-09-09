package io.iohk.atala.mirror

import cats.effect.{ExitCode, IO}
import com.typesafe.config.ConfigFactory
import doobie.util.transactor.Transactor
import io.grpc.{Server, ServerBuilder}
import io.iohk.atala.mirror.config.MirrorConfig
import io.iohk.atala.prism.protos.mirror_api.MirrorServiceGrpc
import io.iohk.atala.prism.repositories.{SchemaMigrations, TransactorFactory}
import monix.eval.{Task, TaskApp}
import monix.execution.Cancelable
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

object MirrorApp extends TaskApp {
  case class Config(port: Int)

  override def run(args: List[String]): Task[ExitCode] = {
    for {
      _ <- new MirrorApp().run()
    } yield ExitCode.Success
  }
}

class MirrorApp() {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def run(): Task[Unit] = {

    for {
      globalConfig <- Task {
        logger.info("Loading config")
        ConfigFactory.load()
      }

      databaseConfig = MirrorConfig.transactorConfig(globalConfig)
      _ <- Task(runMigrations(databaseConfig))
      transactor = createTransactor(databaseConfig)

      mirrorConfig = MirrorConfig.mirrorConfig(globalConfig)

      mirrorService = new MirrorService

      _ <- runServer(mirrorService, mirrorConfig)
    } yield ()
  }

  def createTransactor(databaseConfig: TransactorFactory.Config): Transactor[IO] = {
    logger.info("Connecting to the database")
    TransactorFactory(databaseConfig)
  }

  def runMigrations(databaseConfig: TransactorFactory.Config): Unit = {
    logger.info("Applying database migrations")

    val appliedMigrations = SchemaMigrations.migrate(databaseConfig)

    if (appliedMigrations == 0) {
      logger.info("Database up to date")
    } else {
      logger.info(s"$appliedMigrations migration scripts applied")
    }
  }

  def runServer(mirrorService: MirrorServiceGrpc.MirrorService, config: MirrorApp.Config): Task[Unit] = {
    Task.create { (scheduler, callback) =>
      logger.info("Starting server")
      import io.grpc.protobuf.services.ProtoReflectionService
      val server: Server = ServerBuilder
        .forPort(config.port)
        .addService(MirrorServiceGrpc.bindService(mirrorService, scheduler))
        .addService(
          ProtoReflectionService.newInstance()
        ) //TODO: Decide before release if we should keep this (or guard it with a config flag)
        .build()

      server.start()
      var serverWatcher: Cancelable = null
      serverWatcher = scheduler.scheduleAtFixedRate(100.millis, 100.millis) {
        if (server.isTerminated) {
          callback.onSuccess(())
          serverWatcher.cancel()
        }
      }

      Cancelable { () =>
        val _ = server.shutdown()
      }
    }
  }
}
