package io.iohk.atala.prism.intdemo

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import io.circe.Printer
import io.iohk.connector.model.{Connection, Message, TokenString}
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.prism.protos.credential_models

import scala.concurrent.{ExecutionContext, Future}

object SharedCredentials {

  def credentialsOfType(typeIds: Set[String])(messages: Seq[Message]): Seq[credential_models.Credential] = {
    messages.collect {
      case message =>
        val credential = credential_models.Credential.messageCompanion.parseFrom(message.content)
        if (typeIds.contains(credential.typeId))
          Some(credential)
        else
          None
    }.flatten
  }

  def getSharedCredentials(
      connectorIntegration: ConnectorIntegration,
      connectionToken: TokenString,
      issuerId: ParticipantId
  )(typeIds: Set[String])(implicit ec: ExecutionContext): Future[Seq[credential_models.Credential]] = {
    for {
      maybeConnection: Option[Connection] <- connectorIntegration.getConnectionByToken(connectionToken)
      messages <-
        maybeConnection
          .fold(Future.successful(Seq.empty[Message]))(connection =>
            connectorIntegration.getMessages(issuerId, connection.connectionId)
          )
    } yield {
      credentialsOfType(typeIds)(messages)
    }
  }

  val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  def formatDate(d: LocalDate): String = {
    dateFormatter.format(d)
  }

  val jsonPrinter = Printer(dropNullValues = false, indent = "  ")

}
