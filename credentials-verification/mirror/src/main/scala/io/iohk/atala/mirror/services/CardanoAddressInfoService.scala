package io.iohk.atala.mirror.services

import java.time.Instant

import cats.data.OptionT
import doobie.util.transactor.Transactor
import io.iohk.atala.mirror.db.CardanoAddressInfoDao
import monix.eval.Task
import doobie.implicits._
import io.iohk.atala.mirror.models.{CardanoAddressInfo, ConnectorMessageId}
import io.iohk.atala.mirror.utils.ConnectionUtils
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.protos.credential_models.{AtalaMessage, RegisterAddressMessage}
import org.slf4j.LoggerFactory

import scala.util.Try

class CardanoAddressInfoService(tx: Transactor[Task]) {

  private val logger = LoggerFactory.getLogger(classOf[CardanoAddressInfoService])

  val cardanoAddressInfoMessageProcessor: MessageProcessor = new MessageProcessor {
    def attemptProcessMessage(receivedMessage: ReceivedMessage): Option[Task[Unit]] = {
      parseCardanoAddressInfoMessage(receivedMessage).map(addressMessage =>
        saveCardanoAddressInfo(receivedMessage, addressMessage)
      )
    }
  }

  private def saveCardanoAddressInfo(
      receivedMessage: ReceivedMessage,
      addressMessage: RegisterAddressMessage
  ): Task[Unit] = {
    def save(cardanoAddress: CardanoAddressInfo): Task[Unit] =
      for {
        alreadyExistingAddressOption <- CardanoAddressInfoDao.findBy(cardanoAddress.cardanoAddress).transact(tx)
        _ <-
          if (alreadyExistingAddressOption.isDefined) {
            val alreadyExistingAddress = alreadyExistingAddressOption.get
            logger.warn(
              s"Cardano address with id: ${alreadyExistingAddress.cardanoAddress.address} already exists. " +
                s"It belongs to ${alreadyExistingAddress.connectionToken.token} connection token"
            )
            Task.unit
          } else
            CardanoAddressInfoDao.insert(cardanoAddress).transact(tx)
      } yield ()

    (for {
      connection <- OptionT(ConnectionUtils.findConnection(receivedMessage, logger).transact(tx))
      cardanoAddress = CardanoAddressInfo(
        cardanoAddress = CardanoAddressInfo.CardanoAddress(addressMessage.cardanoAddress),
        connectionToken = connection.token,
        registrationDate = CardanoAddressInfo.RegistrationDate(Instant.now()),
        messageId = ConnectorMessageId(receivedMessage.id)
      )
      _ <- OptionT.liftF(save(cardanoAddress))
    } yield ()).value.map(_ => ())
  }

  private def parseCardanoAddressInfoMessage(message: ReceivedMessage): Option[RegisterAddressMessage] = {
    Try(AtalaMessage.parseFrom(message.message.toByteArray)).toOption
      .flatMap(_.message.mirrorMessage)
      .flatMap(_.message.registerAddressMessage)
  }

}
