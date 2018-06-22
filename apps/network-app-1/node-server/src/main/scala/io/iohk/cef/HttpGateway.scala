package io.iohk.cef

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json.DefaultJsonProtocol._

import scala.concurrent.Future

object HttpGateway {

  case class MessageRequest(message: String, expectedPeerCount: Int)

  implicit val messageRequestJsonFormat = jsonFormat2(MessageRequest)

  def route(requestHandler: MessageRequest => Future[Int]): Route =
    path("message") {
      post {
        entity(as[MessageRequest]) { messageRequest =>
          onSuccess(requestHandler(messageRequest)) { peerCount =>
            if (peerCount == messageRequest.expectedPeerCount)
              complete(StatusCodes.NoContent)
            else
              complete(StatusCodes.BadGateway, s"Expected ${messageRequest.expectedPeerCount} confirms but received $peerCount.")
          }
        }
      }
    }

}
