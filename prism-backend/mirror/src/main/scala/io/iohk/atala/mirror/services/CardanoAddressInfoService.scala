package io.iohk.atala.mirror.services

import java.time.Instant

import cats.data.{EitherT, OptionT}
import cats.free.Free
import cats.implicits._
import doobie.free.connection
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import io.iohk.atala.mirror.db.{CardanoAddressInfoDao, ConnectionDao}
import monix.eval.Task
import doobie.implicits._
import io.iohk.atala.mirror.models.CardanoAddressInfo.CardanoNetwork
import io.iohk.atala.prism.mirror.payid.{Address, AddressDetails, CryptoAddressDetails, PayID, PaymentInformation}
import io.iohk.atala.mirror.models.{CardanoAddressInfo, Connection}
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
import io.iohk.atala.prism.models.ConnectorMessageId
import io.iohk.atala.prism.services.{NodeClientService, MessageProcessor}
import io.iohk.atala.prism.services.MessageProcessor.{MessageProcessorResult, MessageProcessorException}

import scala.util.Try

class CardanoAddressInfoService(tx: Transactor[Task], httpConfig: HttpConfig, nodeService: NodeClientService) {

  private implicit def ec = EC

  private val logger = LoggerFactory.getLogger(classOf[CardanoAddressInfoService])

  private val PAY_ID_NAME_MINIMUM_LENGTH = 4
  private val PAY_ID_NAME_MAXIMUM_LENGTH = 60
  private val PAY_ID_NAME_ALLOWED_CHARACTERS = "[a-zA-Z0-9-.]+".r

  val cardanoAddressInfoMessageProcessor: MessageProcessor = { receivedMessage =>
    parseCardanoAddressInfoMessage(receivedMessage)
      .map(saveCardanoAddressInfo(receivedMessage, _))
  }

  private def saveCardanoAddressInfo(
      receivedMessage: ReceivedMessage,
      addressMessage: RegisterAddressMessage
  ): MessageProcessorResult = {

    (for {
      connection <- EitherT(Connection.fromReceivedMessage(receivedMessage).transact(tx))
      cardanoAddress = CardanoAddressInfo(
        cardanoAddress = CardanoAddressInfo.CardanoAddress(addressMessage.cardanoAddress),
        payidVerifiedAddress = None,
        cardanoNetwork = CardanoNetwork(addressMessage.cardanoNetwork),
        connectionToken = connection.token,
        registrationDate = CardanoAddressInfo.RegistrationDate(Instant.now()),
        messageId = ConnectorMessageId(receivedMessage.id)
      )
      _ <- EitherT.right[MessageProcessorException](saveCardanoAddress(cardanoAddress).transact(tx))
    } yield ()).value
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

  val payIdMessageProcessor: MessageProcessor = { receivedMessage =>
    parsePayIdMessage(receivedMessage)
      .map(savePaymentInformation(receivedMessage, _))
  }

  private[services] def parsePayIdMessage(message: ReceivedMessage): Option[PayIdMessage] = {
    Try(AtalaMessage.parseFrom(message.message.toByteArray)).toOption
      .flatMap(_.message.mirrorMessage)
      .flatMap(_.message.payIdMessage)
  }

  private[services] def savePaymentInformation(
      receivedMessage: ReceivedMessage,
      payIdMessage: PayIdMessage
  ): MessageProcessorResult = {

    (for {
      paymentInformation <- EitherT.fromEither[Task](
        decode[PaymentInformation](payIdMessage.paymentInformation).leftMap(error =>
          MessageProcessorException(
            s"Could not parse payment information: ${payIdMessage.paymentInformation} error: ${error.getMessage}"
          )
        )
      )

      connection <- EitherT(Connection.fromReceivedMessage(receivedMessage).transact(tx))

      splittedPayId <- EitherT.fromOption[Task](
        paymentInformation.payId.flatMap(splitPayId),
        MessageProcessorException(s"Cannot split payId to prefix and host address: ${paymentInformation.payId}")
      )
      (payIdPrefix, payIdHostAddress) =
        splittedPayId // we don't use better-monadic-for, so I had to split the above line into two

      holderDID <-
        EitherT.fromOption[Task](DID.fromString(payIdPrefix), MessageProcessorException("Invalid holder DID."))

      _ <- EitherT.cond[Task](
        connection.holderDID.contains(holderDID) || connection.payIdName.contains(PayIdName(payIdPrefix)),
        (), {
          val message = connection.holderDID match {
            case Some(_) =>
              s"holderDID from connection: ${connection.holderDID} doesn't match holderDID parsed from paymentId: $holderDID"
            case None => "holderDID from connection is not defined"
          }
          MessageProcessorException(
            s"$message and payIdName from connection ${connection.payIdName} doesn't match payIdName parsed from paymentId $payIdPrefix"
          )
        }
      )

      _ <- EitherT.cond[Task](
        httpConfig.payIdHostAddress == payIdHostAddress,
        (),
        MessageProcessorException(
          s"payIdHostAddress from config: ${httpConfig.payIdHostAddress} " +
            s"doesn't match host address from payment information: $payIdHostAddress"
        )
      )

      addressWithVerifiedSignature <-
        EitherT
          .liftF(paymentInformation.verifiedAddresses.toList.traverseFilter { address =>
            verifySignature(address).map { isValidSignature =>
              if (isValidSignature) Some(address)
              else {
                logger.warn(s"Address: $address signature is not valid")
                None
              }
            }
          })
          .leftMap(error => MessageProcessorException(error))

      cardanoAddressesWithVerifiedSignature = addressWithVerifiedSignature.flatMap(verifiedAddress =>
        parseAddress(receivedMessage, verifiedAddress.content.payload.payIdAddress, Some(verifiedAddress), connection)
      )

      cardanoAddresses =
        paymentInformation.addresses.flatMap(address => parseAddress(receivedMessage, address, None, connection))

      _ <-
        EitherT
          .liftF(
            (cardanoAddresses ++ cardanoAddressesWithVerifiedSignature).toList.traverse(saveCardanoAddress).transact(tx)
          )
          .leftMap(error => MessageProcessorException(error))

    } yield ()).value
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
        NodeClientService
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

  val payIdNameRegistrationMessageProcessor: MessageProcessor = { receivedMessage =>
    parsePayIdNameRegistrationMessage(receivedMessage)
      .map(processPayIdNameRegistrationMessage(receivedMessage, _))
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
  ): MessageProcessorResult = {
    val payIdName = PayIdName(payIdMessage.name)
    (for {
      connection <- EitherT(Connection.fromReceivedMessage(receivedMessage))
      connectionWithTheSamePayIdName <- EitherT.liftF(ConnectionDao.findByPayIdName(payIdName))

      _ <- EitherT.fromEither[ConnectionIO](verifyPayIdString(payIdName.name))

      _ <- EitherT.cond[ConnectionIO](
        connection.payIdName.isEmpty,
        (),
        MessageProcessorException(
          s"Cannot register pay id name: $payIdName, " +
            s"connection: ${connection.token} has already registered name: ${connection.payIdName}"
        )
      )

      _ <- EitherT.cond[ConnectionIO](
        connectionWithTheSamePayIdName.isEmpty,
        (),
        MessageProcessorException(
          s"Cannot register pay id name: $payIdName, " +
            s"name has already been registered by connection token: ${connectionWithTheSamePayIdName.map(_.token)}"
        )
      )

      _ <-
        EitherT
          .liftF(ConnectionDao.update(connection.copy(payIdName = Some(payIdName))))
          .leftMap(error => MessageProcessorException(error))
    } yield ()).value.transact(tx)
  }

  private def verifyPayIdString(payIdName: String): Either[MessageProcessorException, Boolean] = {
    val onlyAllowedCharacters = PAY_ID_NAME_ALLOWED_CHARACTERS.matches(payIdName)
    val requiredLength =
      payIdName.length >= PAY_ID_NAME_MINIMUM_LENGTH && payIdName.length <= PAY_ID_NAME_MAXIMUM_LENGTH

    (onlyAllowedCharacters, requiredLength) match {
      case (false, _) =>
        Left(
          MessageProcessorException(
            s"Cannot register pay id name: $payIdName, name contains not allowed characters. " +
              s"Allowed characters regex: $PAY_ID_NAME_ALLOWED_CHARACTERS"
          )
        )
      case (_, false) =>
        Left(
          MessageProcessorException(
            s"Cannot register pay id name: $payIdName, " +
              s"name's length must be between $PAY_ID_NAME_MINIMUM_LENGTH and $PAY_ID_NAME_MAXIMUM_LENGTH characters long"
          )
        )
      case _ => Right(true)
    }
  }

}
