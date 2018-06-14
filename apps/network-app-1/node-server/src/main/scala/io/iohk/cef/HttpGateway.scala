package io.iohk.cef

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json.DefaultJsonProtocol._

import scala.concurrent.Future

object HttpGateway {

  case class MessageRequest(message: String)

  implicit val messageRequestJsonFormat = jsonFormat1(MessageRequest)

  def route(requestHandler: MessageRequest => Future[Unit]): Route =
    path("message") {
      post {
        entity(as[MessageRequest]) { messageRequest =>
          onSuccess(requestHandler(messageRequest)) {
            complete(StatusCodes.NoContent)
          }
        }
      }
    }

}
