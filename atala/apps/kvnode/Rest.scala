package atala.apps.kvnode

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import scala.io.StdIn
import atala.config._

case class Rest(bindingAddress: ServerAddress, controller: ObftController[S, Tx, Unit, S]) {

  private val route =
    path("state") {
      get {
        val r = controller.ask(())

        complete(
          HttpEntity(
            ContentTypes.`application/json`,
            responseToJson(r)
          )
        )
      }
    } ~
      path("add" / IntNumber / Segment) { (i, s) =>
        get {
          controller.receiveTransaction((i, s))
          complete(
            HttpEntity(
              ContentTypes.`application/json`,
              s"""{"message": "Transaction ($i, $s) has been tried to be sent to the OBFT network"}"""
            )
          )
        }
      }

  private def responseToJson(s: S): String =
    s.toList
      .sortBy { case (k, _) => k }
      .map { case (k, v) => s"""{"$k": "$v"}""" }
      .mkString("[", ", ", "]")

  def start(): Unit = {

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val bindingFuture = Http().bindAndHandle(route, bindingAddress.host, bindingAddress.port)

    println(s"Server online at http://${bindingAddress.host}:${bindingAddress.port}/")

  }
}
