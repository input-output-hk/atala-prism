package io.iohk.atala.mirror.http

import cats.effect._
import io.iohk.atala.mirror.config.HttpConfig
import io.iohk.atala.mirror.http.endpoints.PaymentEndpoints
import monix.eval.Task
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._
import org.http4s.server.middleware.Logger

import scala.concurrent.ExecutionContext.Implicits.global

class ApiServer(paymentEndpoints: PaymentEndpoints, httpConfig: HttpConfig)(implicit
    concurrentEffect: ConcurrentEffect[Task]
) extends Http4sDsl[Task] {

  implicit val cs: ContextShift[Task] = Task.contextShift
  implicit val timer: Timer[Task] = Task.timer

  lazy val payIdServer: BlazeServerBuilder[Task] = {
    val httpApp = Router(
      s"/" -> paymentEndpoints.service
    ).orNotFound

    val endpointsWrappers = Logger.httpApp[Task](logHeaders = true, logBody = true)(httpApp)

    BlazeServerBuilder[Task](global).bindHttp(httpConfig.payIdPort, "localhost").withHttpApp(endpointsWrappers)
  }
}
