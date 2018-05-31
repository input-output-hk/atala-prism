package io.iohk.cef

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{as, complete, entity, path, post}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

object HttpGateway {

  case class MessageRequest(message: String)

  implicit val messageRequestJsonFormat = jsonFormat1(MessageRequest)

  def route(requestHandler: MessageRequest => Unit): Route =
    path("message") {
      post {
        entity(as[MessageRequest]) { messageRequest =>
          requestHandler(messageRequest)
          complete(StatusCodes.NoContent)
        }
      }
    }
}
