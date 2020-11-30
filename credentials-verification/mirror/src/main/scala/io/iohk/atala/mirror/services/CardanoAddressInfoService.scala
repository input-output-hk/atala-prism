package io.iohk.atala.mirror.services

import java.time.Instant

import cats.data.OptionT
import cats.free.Free
import cats.implicits._
import doobie.free.connection
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import io.iohk.atala.mirror.db.{CardanoAddressInfoDao, ConnectionDao}
import monix.eval.Task
import doobie.implicits._
import io.iohk.atala.mirror.models.CardanoAddressInfo.CardanoNetwork
import io.iohk.atala.mirror.NodeUtils
import io.iohk.atala.prism.mirror.payid.{Address, AddressDetails, CryptoAddressDetails, PayID, PaymentInformation}
import io.iohk.atala.mirror.models.{CardanoAddressInfo, Connection, ConnectorMessageId}
import io.iohk.atala.mirror.utils.ConnectionUtils
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.protos.credential_models.{
  AtalaMessage,
  PayIdMessage,
  PayIdNameRegistrationMessage,
  RegisterAddressMessage
}
import org.slf4j.LoggerFactory
import io.circe.parser._
import io.iohk.atala.mirror.config.HttpConfig
import io.iohk.atala.mirror.models.Connection.PayIdName
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.mirror.payid.Address.VerifiedAddress
import io.iohk.atala.prism.mirror.payid.implicits._
import io.iohk.atala.prism.identity.DID

import scala.util.Try

class CardanoAddressInfoService(tx: Transactor[Task], httpConfig: HttpConfig, nodeService: NodeClientService) {

  private implicit def ec = EC

  private val logger = LoggerFactory.getLogger(classOf[CardanoAddressInfoService])

  private val PAY_ID_NAME_MINIMUM_LENGTH = 4
  private val PAY_ID_NAME_MAXIMUM_LENGTH = 60
  private val PAY_ID_NAME_ALLOWED_CHARACTERS = "[a-zA-Z0-9-.]+".r

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
        payidVerifiedAddress = None,
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

  def findPaymentInfoByHolderDid(
      did: DID,
      cardanoNetwork: CardanoNetwork
  ): Task[Option[(Connection, List[CardanoAddressInfo])]] = {
    (for {
      connection <- OptionT(ConnectionDao.findByHolderDID(did))
      cardanoAddressesInfo <- OptionT.liftF(CardanoAddressInfoDao.findBy(connection.token, cardanoNetwork))
    } yield (connection, cardanoAddressesInfo)).value.transact(tx)
  }

  def findPaymentInfoByPayIdName(
      payIdName: PayIdName,
      cardanoNetwork: CardanoNetwork
  ): Task[Option[(Connection, List[CardanoAddressInfo])]] = {
    (for {
      connection <- OptionT(ConnectionDao.findByPayIdName(payIdName))
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

      (payIdPrefix, payIdHostAddress) <- (paymentInformation.payId
          .flatMap(splitPayId)
          .orElse {
            logger.warn(s"Cannot split payId to prefix and host address: ${paymentInformation.payId}")
            None
          })
        .toOptionT[Task]

      holderDID = DID.fromString(payIdPrefix)

      _ <-
        (if (
           (connection.holderDID.isDefined && connection.holderDID == holderDID)
           || connection.payIdName.contains(PayIdName(payIdPrefix))
         ) Some(())
         else {
           val holderDidUndefined = "holderDID from connection is not defined"
           val holderDidNotMatch =
             s"holderDID from connection: ${connection.holderDID} doesn't match holderDID parsed from paymentId: $holderDID "
           val payIdNameNotMatch =
             s"and payIdName from connection ${connection.payIdName} doesn't match payIdName parsed from paymentId $payIdPrefix"

           val errorMessage =
             (if (connection.holderDID.isEmpty) holderDidUndefined else holderDidNotMatch) + payIdNameNotMatch

           logger.warn(errorMessage)
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

      addressWithVerifiedSignature <- OptionT.liftF(paymentInformation.verifiedAddresses.toList.traverseFilter {
        address =>
          verifySignature(address).map { isValidSignature =>
            if (isValidSignature) Some(address)
            else {
              logger.warn(s"Address: $address signature is not valid")
              None
            }
          }
      })

      cardanoAddressesWithVerifiedSignature = addressWithVerifiedSignature.flatMap(verifiedAddress =>
        parseAddress(receivedMessage, verifiedAddress.content.payload.payIdAddress, Some(verifiedAddress), connection)
      )

      cardanoAddresses =
        paymentInformation.addresses.flatMap(address => parseAddress(receivedMessage, address, None, connection))

      _ <- OptionT.liftF(
        (cardanoAddresses ++ cardanoAddressesWithVerifiedSignature).toList.traverse(saveCardanoAddress).transact(tx)
      )

    } yield ()).value.map(_ => ())
  }

  private[services] def splitPayId(payId: PayID): Option[(String, String)] =
    payId.value.split("\\$") match {
      case Array(payIdPrefix, host) => Some(payIdPrefix -> host)
      case _ => None
    }

  private[services] def parseAddress(
      receivedMessage: ReceivedMessage,
      paymentAddress: Address,
      verifiedAddress: Option[VerifiedAddress],
      connection: Connection
  ): Option[CardanoAddressInfo] = {
    for {
      cryptoAddressDetails <- parseCryptoAddressDetails(paymentAddress.addressDetails).orElse {
        logger.warn(s"Could not parse crypto address details: ${paymentAddress.addressDetails}")
        None
      }
    } yield {
      CardanoAddressInfo(
        cardanoAddress = CardanoAddressInfo.CardanoAddress(cryptoAddressDetails.address),
        payidVerifiedAddress = verifiedAddress,
        cardanoNetwork = CardanoNetwork(paymentAddress.paymentNetwork),
        connectionToken = connection.token,
        registrationDate = CardanoAddressInfo.RegistrationDate(Instant.now()),
        messageId = ConnectorMessageId(receivedMessage.id)
      )
    }
  }

  private[services] def verifySignature(
      verifiedAddress: VerifiedAddress
  ): Task[Boolean] = {
    (for {
      kid <-
        verifiedAddress.content.protectedHeader.jwk.kid
          .orElse {
            logger.warn(
              s"Cannot verify address signature, protected header doesn't contain kid: ${verifiedAddress.content.protectedHeader}"
            )
            None
          }
          .toOptionT[Task]

      (didRaw, keyId) <- (kid.split("#") match {
          case Array(did, key) => Some(did -> key)
          case _ =>
            logger.warn(s"Cannot verify address signature, kid: $kid is not in did:prism:did-suffix#key-id format")
            None
        }).toOptionT[Task]

      did <-
        DID
          .fromString(didRaw)
          .orElse {
            logger.warn(s"Cannot verify address signature, did: ${didRaw} is not in proper format")
            None
          }
          .toOptionT[Task]

      keyData <-
        NodeUtils
          .getKeyData(did, keyId, nodeService)
          .leftMap(error => logger.warn(s"Cannot verify address signature, $error"))
          .toOption

    } yield verifiedAddress.isValidSignature(keyData.publicKey)).value.map(_.getOrElse(false))
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

  val payIdNameRegistrationMessageProcessor: MessageProcessor = new MessageProcessor {
    def attemptProcessMessage(receivedMessage: ReceivedMessage): Option[Task[Unit]] = {
      parsePayIdNameRegistrationMessage(receivedMessage).map(payIdMessage =>
        processPayIdNameRegistrationMessage(receivedMessage, payIdMessage)
      )
    }
  }

  private[services] def parsePayIdNameRegistrationMessage(
      message: ReceivedMessage
  ): Option[PayIdNameRegistrationMessage] = {
    Try(AtalaMessage.parseFrom(message.message.toByteArray)).toOption
      .flatMap(_.message.mirrorMessage)
      .flatMap(_.message.payIdNameRegistrationMessage)
  }

  def processPayIdNameRegistrationMessage(
      receivedMessage: ReceivedMessage,
      payIdMessage: PayIdNameRegistrationMessage
  ): Task[Unit] = {
    val payIdName = PayIdName(payIdMessage.name)
    (for {
      connection <- OptionT(ConnectionUtils.findConnection(receivedMessage, logger))
      connectionWithTheSamePayIdName <- OptionT.liftF(ConnectionDao.findByPayIdName(payIdName))

      _ <-
        if (verifyPayIdString(payIdName.name)) OptionT.pure[ConnectionIO](())
        else OptionT.fromOption[ConnectionIO](None)

      _ <-
        if (connection.payIdName.isEmpty) OptionT.pure[ConnectionIO](())
        else {
          logger.warn(
            s"Cannot register pay id name: $payIdName, " +
              s"connection: ${connection.token} has already registered name: ${connection.payIdName}"
          )
          OptionT.fromOption[ConnectionIO](None)
        }

      _ <-
        if (connectionWithTheSamePayIdName.isEmpty) OptionT.pure[ConnectionIO](())
        else {
          logger.warn(
            s"Cannot register pay id name: $payIdName, " +
              s"name has already been registered by connection token: ${connectionWithTheSamePayIdName.map(_.token)}"
          )
          OptionT.fromOption[ConnectionIO](None)
        }

      _ <- OptionT.liftF(ConnectionDao.update(connection.copy(payIdName = Some(payIdName))))
    } yield ()).value.transact(tx).map(_ => ())
  }

  private def verifyPayIdString(payIdName: String): Boolean = {
    val onlyAllowedCharacters = PAY_ID_NAME_ALLOWED_CHARACTERS.matches(payIdName)
    val requiredLength =
      payIdName.length >= PAY_ID_NAME_MINIMUM_LENGTH && payIdName.length <= PAY_ID_NAME_MAXIMUM_LENGTH

    if (!onlyAllowedCharacters)
      logger.warn(
        s"Cannot register pay id name: $payIdName, name contains not allowed characters. " +
          s"Allowed characters regex: $PAY_ID_NAME_ALLOWED_CHARACTERS"
      )
    if (!requiredLength)
      logger.warn(
        s"Cannot register pay id name: $payIdName, " +
          s"name's length must be between $PAY_ID_NAME_MINIMUM_LENGTH and $PAY_ID_NAME_MAXIMUM_LENGTH characters long"
      )

    onlyAllowedCharacters && requiredLength
  }

}
