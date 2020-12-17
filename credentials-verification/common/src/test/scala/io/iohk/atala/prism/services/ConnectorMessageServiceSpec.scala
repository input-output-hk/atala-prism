package io.iohk.atala.prism.services

import java.time.{LocalDateTime, ZoneOffset}

import scala.concurrent.duration.DurationInt

import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.daos.ConnectorMessageOffsetDao
import io.iohk.atala.prism.models.ConnectorMessageId
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.protos.credential_models.AtalaMessage
import io.iohk.atala.prism.stubs.ConnectorClientServiceStub
import io.iohk.atala.prism.services.MessageProcessor.MessageProcessorResult

import doobie.implicits._
import monix.execution.Scheduler.Implicits.global

// sbt "project common" "testOnly *services.ConnectorMessageServiceSpec"
class ConnectorMessageServiceSpec extends PostgresRepositorySpec {

  override protected def migrationScriptsLocation: String = "common/db/migration"

  class SpyMessageProcessor extends MessageProcessor {
    var processedMessages = List.empty[ReceivedMessage]

    override def apply(receivedMessage: ReceivedMessage): Option[MessageProcessorResult] = {
      processedMessages = receivedMessage :: processedMessages
      Some(MessageProcessor.successful)
    }
  }

  "messagesUpdatesStream" should {
    "process messages" in {
      // given
      val receivedMessage = ReceivedMessage(
        id = "id1",
        received = LocalDateTime.of(2020, 6, 12, 0, 0).toEpochSecond(ZoneOffset.UTC),
        connectionId = "0a66fcef-4d50-4a67-a365-d4dbebcf22d3",
        message = AtalaMessage().toByteString
      )

      val connectorClientService =
        new ConnectorClientServiceStub(receivedMessages = Seq(receivedMessage))

      val spyMessageProcessor = new SpyMessageProcessor

      val connectorMessagesService = new ConnectorMessagesService(
        connectorService = connectorClientService,
        messageProcessors = List(spyMessageProcessor),
        findLastMessageOffset = ConnectorMessageOffsetDao.findLastMessageOffset().transact(databaseTask),
        saveMessageOffset = ConnectorMessageOffsetDao.updateLastMessageOffset(_).transact(databaseTask).map(_ => ())
      )

      // when
      val lastSeenMessageId = (for {
        _ <- connectorMessagesService.messagesUpdatesStream.interruptAfter(5.second).compile.drain
        lastSeenMessageId <- ConnectorMessageOffsetDao.findLastMessageOffset().transact(databaseTask)
      } yield lastSeenMessageId).runSyncUnsafe()

      // then
      spyMessageProcessor.processedMessages mustBe List(receivedMessage)
      lastSeenMessageId mustBe Some(ConnectorMessageId(receivedMessage.id))
    }
  }
}
