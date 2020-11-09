package io.iohk.atala.mirror.services

import java.time.Instant

import cats.data.OptionT
import cats.free.Free
import cats.implicits._
import doobie.free.connection
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import io.iohk.atala.mirror.db.{CardanoAddressInfoDao, ConnectionDao, PayIdMessageDao}
import monix.eval.Task
import doobie.implicits._
import io.iohk.atala.mirror.models.CardanoAddressInfo.CardanoNetwork
import io.iohk.atala.mirror.models
import io.iohk.atala.mirror.models.payid.{Address, AddressDetails, PaymentInformation}
import io.iohk.atala.mirror.models.{CardanoAddressInfo, Connection, ConnectorMessageId, DID}
import io.iohk.atala.mirror.utils.ConnectionUtils
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.protos.credential_models.{AtalaMessage, PayIdMessage, RegisterAddressMessage}
import org.slf4j.LoggerFactory
import io.iohk.atala.mirror.models.payid.AddressDetails.CryptoAddressDetails
import io.circe.parser._
import io.iohk.atala.mirror.config.HttpConfig

import scala.util.Try

class CardanoAddressInfoService(tx: Transactor[Task], httpConfig: HttpConfig) {

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

    (for {
      connection <- OptionT(ConnectionUtils.findConnection(receivedMessage, logger).transact(tx))
      cardanoAddress = CardanoAddressInfo(
        cardanoAddress = CardanoAddressInfo.CardanoAddress(addressMessage.cardanoAddress),
        cardanoNetwork = CardanoNetwork(addressMessage.cardanoNetwork),
        connectionToken = connection.token,
        registrationDate = CardanoAddressInfo.RegistrationDate(Instant.now()),
        messageId = ConnectorMessageId(receivedMessage.id)
      )
      _ <- OptionT.liftF(saveCardanoAddress(cardanoAddress).transact(tx))
    } yield ()).value.map(_ => ())
  }

  private def parseCardanoAddressInfoMessage(message: ReceivedMessage): Option[RegisterAddressMessage] = {
    Try(AtalaMessage.parseFrom(message.message.toByteArray)).toOption
      .flatMap(_.message.mirrorMessage)
      .flatMap(_.message.registerAddressMessage)
  }

  def findPaymentInfo(
      did: DID,
      cardanoNetwork: CardanoNetwork
  ): Task[Option[(Connection, List[CardanoAddressInfo])]] = {
    (for {
      connection <- OptionT(ConnectionDao.findByHolderDID(did))
      cardanoAddressesInfo <- OptionT.liftF(CardanoAddressInfoDao.findBy(connection.token, cardanoNetwork))
    } yield (connection, cardanoAddressesInfo)).value.transact(tx)
  }

  val payIdMessageProcessor: MessageProcessor = new MessageProcessor {
    def attemptProcessMessage(receivedMessage: ReceivedMessage): Option[Task[Unit]] = {
      parsePayIdMessage(receivedMessage).map(payIdMessage => savePaymentInformation(receivedMessage, payIdMessage))
    }
  }

  private[services] def parsePayIdMessage(message: ReceivedMessage): Option[PayIdMessage] = {
    Try(AtalaMessage.parseFrom(message.message.toByteArray)).toOption
      .flatMap(_.message.mirrorMessage)
      .flatMap(_.message.payIdMessage)
  }

  private[services] def savePaymentInformation(
      receivedMessage: ReceivedMessage,
      payIdMessage: PayIdMessage
  ): Task[Unit] = {

    (for {
      paymentInformation <-
        decode[PaymentInformation](payIdMessage.paymentInformation).left
          .map { error =>
            logger.warn(
              s"Could not parse payment information: ${payIdMessage.paymentInformation} error: ${error.getMessage}"
            )
          }
          .toOption
          .toOptionT[Task]

      connection <- OptionT(ConnectionUtils.findConnection(receivedMessage, logger).transact(tx))

      (holderDID, payIdHostAddress) <- (paymentInformation.payId
          .flatMap(parseDidAndHostFromPayId)
          .orElse {
            logger.warn(s"Cannot parse did from payId: ${paymentInformation.payId}")
            None
          })
        .toOptionT[Task]

      _ <-
        (if (connection.holderDID.contains(holderDID)) Some(())
         else {
           logger.warn(
             s"holderDID from connection: ${connection.holderDID} doesn't match holderDID parsed from paymentId: $holderDID "
           )
           None
         }).toOptionT[Task]

      _ <-
        (if (httpConfig.payIdHostAddress == payIdHostAddress) Some(())
         else {
           logger.warn(
             s"payIdHostAddress from config: ${httpConfig.payIdHostAddress} " +
               s"doesn't match host address from payment information: $payIdHostAddress "
           )
           None
         }).toOptionT[Task]

      cardanoAddress = paymentInformation.verifiedAddresses.flatMap(verifiedAddress =>
        parseAddress(receivedMessage, verifiedAddress.payload, connection)
      )

      _ <- OptionT.liftF(cardanoAddress.traverse(saveCardanoAddress).transact(tx))
      _ <- OptionT.liftF(
        PayIdMessageDao
          .insert(
            models.PayIdMessage(
              connectorMessageId = ConnectorMessageId(receivedMessage.id),
              rawPaymentInformation = models.PayIdMessage.RawPaymentInformation(payIdMessage.paymentInformation)
            )
          )
          .transact(tx)
      )

    } yield ()).value.map(_ => ())
  }

  private[services] def parseDidAndHostFromPayId(payId: String): Option[(DID, String)] =
    payId.split("\\$") match {
      case Array(did, host) => Some(DID(did) -> host)
      case _ => None
    }

  private[services] def parseAddress(
      receivedMessage: ReceivedMessage,
      addressPayload: String,
      connection: Connection
  ): Option[CardanoAddressInfo] = {
    for {
      //TODO not sure if payload in verified address is actually Address https://github.com/payid-org/payid/issues/704
      paymentAddress <- decode[Address](addressPayload).left.map { error =>
        logger.warn(s"Could not parse payment address payload: $addressPayload error: ${error.getMessage}")
      }.toOption

      cryptoAddressDetails <- parseCryptoAddressDetails(paymentAddress.addressDetails).orElse {
        logger.warn(s"Could not parse crypto address details: ${paymentAddress.addressDetails}")
        None
      }
    } yield {
      CardanoAddressInfo(
        cardanoAddress = CardanoAddressInfo.CardanoAddress(cryptoAddressDetails.address),
        cardanoNetwork = CardanoNetwork(paymentAddress.paymentNetwork),
        connectionToken = connection.token,
        registrationDate = CardanoAddressInfo.RegistrationDate(Instant.now()),
        messageId = ConnectorMessageId(receivedMessage.id)
      )
    }
  }

  private[services] def parseCryptoAddressDetails(addressDetails: AddressDetails): Option[CryptoAddressDetails] =
    addressDetails match {
      case cryptoAddressDetails: CryptoAddressDetails => Some(cryptoAddressDetails)
      case _ => None
    }

  def saveCardanoAddress(cardanoAddress: CardanoAddressInfo): ConnectionIO[Unit] =
    for {
      alreadyExistingAddressOption <- CardanoAddressInfoDao.findBy(cardanoAddress.cardanoAddress)
      _ <-
        if (alreadyExistingAddressOption.isDefined) {
          val alreadyExistingAddress = alreadyExistingAddressOption.get
          logger.warn(
            s"Cardano address with id: ${alreadyExistingAddress.cardanoAddress.address} already exists. " +
              s"It belongs to ${alreadyExistingAddress.connectionToken.token} connection token"
          )
          Free.pure[connection.ConnectionOp, Unit](())
        } else
          CardanoAddressInfoDao.insert(cardanoAddress)
    } yield ()

}
