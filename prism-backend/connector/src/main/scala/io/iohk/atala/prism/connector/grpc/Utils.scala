package io.iohk.atala.prism.connector.grpc

import io.iohk.atala.prism.connector.model
import io.iohk.atala.prism.connector.model.{ConnectionId, MessageId}

import scala.util.{Failure, Success, Try}

object Utils {

  def getMessageIdField(
      id: String,
      fieldName: String
  ): Try[Option[model.MessageId]] =
    id match {
      case "" => Success(None)
      case id =>
        model.MessageId
          .from(id)
          .fold(
            _ =>
              Failure(
                new IllegalArgumentException(
                  s"Invalid value for $fieldName, expected valid id, got $id"
                )
              ),
            id => Success(Some(id))
          )
    }

  def parseConnectionId(probablyConnectionId: String): Try[ConnectionId] =
    model.ConnectionId
      .from(probablyConnectionId)
      .recoverWith { case _ =>
        Failure(new IllegalArgumentException("Invalid connectionId value"))
      }

  def parseMessageId(messageId: String): Try[Option[MessageId]] = {
    val id = messageId.trim
    if (id.nonEmpty) MessageId.from(id).map(Some(_))
    else Try(None)
  }

}
