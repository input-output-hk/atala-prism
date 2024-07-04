package io.iohk.atala.prism.node

import cats.effect.IO
import com.dimafeng.testcontainers.ContainerDef
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.node.logging.TraceId
import io.iohk.atala.prism.node.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.node.repositories.{DockerPostgresService, PostgresRepositorySpec}
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class AtalaWithPostgresSpec extends PostgresRepositorySpec[IO] with ScalaFutures {
  implicit val executionContext: ExecutionContext = ExecutionContext.global
  implicit val db: Transactor[IO] = database
  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 5.millis)
  val dbLiftedToTraceIdIO: Transactor[IOWithTraceIdContext] =
    db.mapK(TraceId.liftToIOWithTraceId)

  override val containerDef: ContainerDef = DockerPostgresService.containerDef
}
