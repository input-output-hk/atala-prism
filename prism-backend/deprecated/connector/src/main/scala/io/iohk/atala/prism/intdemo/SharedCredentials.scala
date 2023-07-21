package io.iohk.atala.prism.intdemo

import io.circe.Printer
import io.circe.parser._
import io.iohk.atala.prism.connector.model.Connection
import io.iohk.atala.prism.connector.model.Message
import io.iohk.atala.prism.connector.model.TokenString
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.credential_models
import io.iohk.atala.prism.utils.Base64Utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object SharedCredentials {

  def getTypeId(credential: credential_models.PlainTextCredential): String = {
    val decodedCredential =
      Base64Utils.decodeUrlToString(credential.encodedCredential)
    parse(decodedCredential)
      .flatMap { json =>
        val cursor = json.hcursor.downField("credentialSubject")
        cursor.downField("credentialType").as[String]
      }
      .getOrElse {
        throw new IllegalStateException(
          s"Error trying to get the type of the credential. Document follows: '${decodedCredential}''"
        )
      }
  }

  def credentialsOfType(
      typeIds: Set[String]
  )(messages: Seq[Message]): Seq[credential_models.PlainTextCredential] = {
    messages.collect { case message =>
      val credential = credential_models.AtalaMessage
        .parseFrom(message.content)
        .getPlainCredential
      if (typeIds.contains { getTypeId(credential) }) {
        Some(credential)
      } else {
        None
      }
    }.flatten
  }

  def getSharedCredentials(
      connectorIntegration: ConnectorIntegration,
      connectionToken: TokenString,
      issuerId: ParticipantId
  )(typeIds: Set[String])(implicit
      ec: ExecutionContext
  ): Future[Seq[credential_models.PlainTextCredential]] = {
    for {
      maybeConnection: Option[Connection] <- connectorIntegration
        .getConnectionByToken(connectionToken)
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
