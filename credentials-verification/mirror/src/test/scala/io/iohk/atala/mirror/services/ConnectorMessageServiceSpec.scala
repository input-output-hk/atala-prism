package io.iohk.atala.mirror.services

import io.iohk.atala.mirror.MirrorFixtures
import io.iohk.atala.mirror.db.{CardanoAddressInfoDao, ConnectorMessageOffsetDao}
import io.iohk.atala.mirror.models.CardanoAddressInfo.CardanoAddress
import io.iohk.atala.mirror.stubs.ConnectorClientServiceStub
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import monix.execution.Scheduler.Implicits.global
import doobie.implicits._
import io.iohk.atala.mirror.models.ConnectorMessageId

import scala.concurrent.duration.DurationInt

// sbt "project mirror" "testOnly *services.ConnectorMessageServiceSpec"
class ConnectorMessageServiceSpec extends PostgresRepositorySpec with MirrorFixtures {
  import ConnectorMessageFixtures._

  "messagesUpdatesStream" should {
    "process messages" in {
      // given
      ConnectionFixtures.insertAll(databaseTask).runSyncUnsafe()
      val connectorClientService =
        new ConnectorClientServiceStub(receivedMessages = Seq(cardanoAddressInfoMessage1, credentialMessage1))

      val cardanoAddressInfoService = new CardanoAddressInfoService(databaseTask)
      val connectorMessagesService = new ConnectorMessagesService(
        databaseTask,
        connectorClientService,
        List(cardanoAddressInfoService.cardanoAddressInfoMessageProcessor)
      )

      // when
      val (cardanoAddressInfoOption, lastSeenMessageId) = (for {
        _ <- connectorMessagesService.messagesUpdatesStream.interruptAfter(5.second).compile.drain
        cardanoAddressInfoOption <- CardanoAddressInfoDao.findBy(CardanoAddress(cardanoAddress1)).transact(databaseTask)
        lastSeenMessageId <- ConnectorMessageOffsetDao.findLastMessageOffset().transact(databaseTask)
      } yield (cardanoAddressInfoOption, lastSeenMessageId)).runSyncUnsafe()

      // then
      cardanoAddressInfoOption.map(_.cardanoAddress) mustBe Some(CardanoAddress(cardanoAddress1))
      lastSeenMessageId mustBe Some(ConnectorMessageId(credentialMessage1.id))
    }
  }
}
