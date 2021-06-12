package io.iohk.atala.mirror.services

import java.time.Instant
import cats.data.{EitherT, NonEmptyList, OptionT}
import doobie.free.{connection => doobieConnection}
import cats.implicits._
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import io.iohk.atala.mirror.db.{CardanoAddressInfoDao, CardanoWalletAddressDao, ConnectionDao}
import monix.eval.Task
import doobie.implicits._
import io.iohk.atala.mirror.models.CardanoAddressInfo.CardanoNetwork
import io.iohk.atala.prism.mirror.payid.{Address, AddressDetails, CryptoAddressDetails, PayID, PaymentInformation}
import io.iohk.atala.mirror.models.{CardanoAddress, CardanoAddressInfo, CardanoWalletAddressWithWalletName, Connection}
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.protos.credential_models.{
  AtalaMessage,
  CheckPayIdNameAvailabilityMessage,
  PayIdMessage,
  PayIdNameRegistrationMessage,
  RegisterAddressMessage,
  GeneratedCardanoAddress
}
import org.slf4j.LoggerFactory
import io.circe.parser._
import io.grpc.Status
import io.iohk.atala.mirror.config.HttpConfig
import io.iohk.atala.mirror.models.Connection.PayIdName
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.errors.PrismError
import io.iohk.atala.prism.mirror.payid.Address.VerifiedAddress
import io.iohk.atala.prism.mirror.payid.implicits._
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.models.ConnectorMessageId
import io.iohk.atala.prism.services.{MessageProcessor, NodeClientService}
import io.iohk.atala.prism.services.MessageProcessor.MessageProcessorResult
import io.iohk.atala.prism.utils.ConnectionUtils
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import CardanoAddressInfoService._
import io.iohk.atala.prism.protos.credential_models.MirrorMessage
import io.iohk.atala.prism.protos.credential_models.PayIdNameRegisteredMessage
import io.iohk.atala.prism.protos.credential_models.PayIdNameTakenMessage
import io.iohk.atala.prism.protos.credential_models.AddressRegisteredMessage
import io.iohk.atala.prism.protos.credential_models.PaymentInformationSaved
import io.iohk.atala.prism.protos.credential_models.CheckPayIdNameAvailabilityResponse
import io.iohk.atala.prism.protos.credential_models.GetPayIdNameMessage
import io.iohk.atala.prism.protos.credential_models.GetPayIdNameResponse
import io.iohk.atala.prism.protos.credential_models.GetPayIdAddressesMessage
import io.iohk.atala.prism.protos.credential_models.GetPayIdAddressesResponse
import io.iohk.atala.prism.protos.credential_models.ManuallyRegisteredCardanoAddress
import com.google.protobuf.timestamp.Timestamp

import scala.util.Try
import scala.util.matching.Regex

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
      connection <- EitherT(
        ConnectionUtils
          .fromReceivedMessage(receivedMessage, ConnectionDao.findByConnectionId)
          .logSQLErrors("getting connection from received message", logger)
          .transact(tx)
      )
      cardanoAddress = CardanoAddressInfo(
        cardanoAddress = CardanoAddress(addressMessage.cardanoAddress),
        payidVerifiedAddress = None,
        cardanoNetwork = CardanoNetwork(addressMessage.cardanoNetwork),
        connectionToken = connection.token,
        registrationDate = CardanoAddressInfo.RegistrationDate(Instant.now()),
        messageId = ConnectorMessageId(receivedMessage.id)
      )
      _ <- EitherT[Task, PrismError, Unit](
        saveCardanoAddress(List(cardanoAddress))
          .logSQLErrors("saving cardano address", logger)
          .transact(tx)
      )

      messageContent = AddressRegisteredMessage(cardanoAddress = addressMessage.cardanoAddress)
      response = AtalaMessage().withMirrorMessage(MirrorMessage().withAddressRegisteredMessage(messageContent))

    } yield Some(response)).value
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
    } yield (connection, cardanoAddressesInfo)).value
      .logSQLErrors(s"finding payment info, holder did - $did", logger)
      .transact(tx)
  }

  def findPaymentInfoByPayIdName(
      payIdName: PayIdName,
      cardanoNetwork: CardanoNetwork
  ): Task[Option[(Connection, List[CardanoAddressInfo])]] = {
    (for {
      connection <- OptionT(ConnectionDao.findByPayIdName(payIdName))
      cardanoAddressesInfo <- OptionT.liftF(CardanoAddressInfoDao.findBy(connection.token, cardanoNetwork))
    } yield (connection, cardanoAddressesInfo)).value
      .logSQLErrors(s"finding payment info, pay id name - $payIdName", logger)
      .transact(tx)
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
          PaymentInformationDecodingFailure(payIdMessage, error)
        )
      )

      connection <- EitherT(
        ConnectionUtils
          .fromReceivedMessage(receivedMessage, ConnectionDao.findByConnectionId)
          .logSQLErrors("getting connection by received message", logger)
          .transact(tx)
      )

      splittedPayId <- EitherT.fromOption[Task](
        paymentInformation.payId.flatMap(splitPayId),
        InvalidPayIdName(paymentInformation)
      )
      (payIdPrefix, payIdHostAddress) =
        splittedPayId // we don't use better-monadic-for, so I had to split the above line into two

      _ <- assertConnectionContainPayIdInfo(payIdPrefix, connection)
      _ <- assertPayIdHostAddressMach(httpConfig, payIdHostAddress)
      _ <- assertAddressesHaveValidSignatures(paymentInformation)
      _ <- assertAddressesContainCryptoAddressDetails(paymentInformation)

      cardanoAddressesWithVerifiedSignature = paymentInformation.verifiedAddresses.flatMap(verifiedAddress =>
        parseAddress(receivedMessage, verifiedAddress.content.payload.payIdAddress, Some(verifiedAddress), connection)
      )

      cardanoAddresses =
        paymentInformation.addresses.flatMap(address => parseAddress(receivedMessage, address, None, connection))

      _ <- EitherT[Task, PrismError, Unit](
        saveCardanoAddress(cardanoAddresses ++ cardanoAddressesWithVerifiedSignature)
          .logSQLErrors("saving cardano address", logger)
          .transact(tx)
      )

      response =
        AtalaMessage().withMirrorMessage(MirrorMessage().withPaymentInformationSaved(PaymentInformationSaved()))

    } yield Some(response)).value
  }

  def assertConnectionContainPayIdInfo(
      payIdPrefix: String,
      connection: Connection
  ): EitherT[Task, ConnectionDoesNotContainPayIdInfo, Unit] = {
    EitherT.cond[Task](
      connection.holderDID.map(_.toString).contains(payIdPrefix) || connection.payIdName
        .contains(PayIdName(payIdPrefix)),
      (),
      ConnectionDoesNotContainPayIdInfo(connection, payIdPrefix)
    )
  }

  def assertPayIdHostAddressMach(
      httpConfig: HttpConfig,
      payIdHostAddress: String
  ): EitherT[Task, PayIdHostAddressDoesNotMach, Unit] = {
    EitherT.cond[Task](
      httpConfig.payIdHostAddress == payIdHostAddress,
      (),
      PayIdHostAddressDoesNotMach(httpConfig, payIdHostAddress)
    )
  }

  def assertAddressesHaveValidSignatures(
      paymentInformation: PaymentInformation
  ): EitherT[Task, VerifiedAddressesWithNotValidSignature, Unit] = {
    for {
      addressWithNotValidSignature <-
        EitherT
          .liftF(paymentInformation.verifiedAddresses.toList.traverseFilter { address =>
            verifySignature(address).map { isValidSignature =>
              if (isValidSignature) None
              else Some(address)
            }
          })

      _ <- EitherT.cond[Task](
        addressWithNotValidSignature.isEmpty,
        (),
        VerifiedAddressesWithNotValidSignature(addressWithNotValidSignature)
      )
    } yield ()
  }

  def assertAddressesContainCryptoAddressDetails(
      paymentInformation: PaymentInformation
  ): EitherT[Task, CryptoAddressDetailsMissing, Unit] = {
    val addressesWithoutCryptoAddressDetails =
      (paymentInformation.verifiedAddresses.map(_.content.payload.payIdAddress) ++ paymentInformation.addresses)
        .flatMap { address =>
          parseCryptoAddressDetails(address.addressDetails) match {
            case Some(_) => None
            case None => Some(address)
          }
        }

    EitherT.cond[Task](
      addressesWithoutCryptoAddressDetails.isEmpty,
      (),
      CryptoAddressDetailsMissing(addressesWithoutCryptoAddressDetails.toList)
    )
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
        cardanoAddress = CardanoAddress(cryptoAddressDetails.address),
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

  def saveCardanoAddress(
      cardanoAddresses: Seq[CardanoAddressInfo]
  ): ConnectionIO[Either[CardanoAddressAlreadyExists, Unit]] =
    (for {
      alreadyExistingAddress <- EitherT.liftF(
        NonEmptyList
          .fromList(cardanoAddresses.map(_.cardanoAddress).toList)
          .map(CardanoAddressInfoDao.findBy)
          .getOrElse(doobieConnection.pure(List.empty))
      )

      _ <- EitherT.cond[ConnectionIO](
        alreadyExistingAddress.isEmpty,
        (),
        CardanoAddressAlreadyExists(alreadyExistingAddress)
      )

      _ <- EitherT.liftF[ConnectionIO, CardanoAddressAlreadyExists, Int](
        CardanoAddressInfoDao.insertMany.updateMany(cardanoAddresses.toList)
      )
    } yield ()).value

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
      connection <- EitherT(ConnectionUtils.fromReceivedMessage(receivedMessage, ConnectionDao.findByConnectionId))
      connectionWithTheSamePayIdName <- EitherT.liftF(ConnectionDao.findByPayIdName(payIdName))

      _ <- EitherT.fromEither[ConnectionIO](verifyPayIdString(payIdName.name))

      _ <- EitherT.cond[ConnectionIO](
        connection.payIdName.isEmpty,
        (),
        ConnectionHasAlreadyRegisteredPayIdName(payIdName.name, connection)
      )

      responseMessage <- EitherT.liftF[ConnectionIO, PrismError, AtalaMessage] {
        if (connectionWithTheSamePayIdName.isEmpty) {
          val messageContent = PayIdNameRegisteredMessage(name = payIdMessage.name)
          val response = AtalaMessage().withMirrorMessage(
            MirrorMessage().withPayIdNameRegisteredMessage(messageContent)
          )
          ConnectionDao.update(connection.copy(payIdName = Some(payIdName))).as(response)
        } else {
          val message = s"Cannot register pay id name: $payIdName, " +
            s"name has already been registered by connection token: ${connectionWithTheSamePayIdName.map(_.token)}"
          val respone =
            AtalaMessage().withMirrorMessage(MirrorMessage().withPayIdNameTakenMessage(PayIdNameTakenMessage(message)))
          doobieConnection.pure(respone)
        }
      }
    } yield Some(responseMessage)).value
      .logSQLErrors("processing pay id name registration message", logger)
      .transact(tx)
  }

  private[services] def verifyPayIdString(payIdName: String): Either[PrismError, Unit] = {
    val onlyAllowedCharacters = PAY_ID_NAME_ALLOWED_CHARACTERS.matches(payIdName)
    val requiredLength =
      payIdName.length >= PAY_ID_NAME_MINIMUM_LENGTH && payIdName.length <= PAY_ID_NAME_MAXIMUM_LENGTH

    (onlyAllowedCharacters, requiredLength) match {
      case (false, _) => Left(PayIdNameNotAllowedCharacters(payIdName, PAY_ID_NAME_ALLOWED_CHARACTERS))
      case (_, false) =>
        Left(PayIdNameIncorrectLength(payIdName, PAY_ID_NAME_MINIMUM_LENGTH, PAY_ID_NAME_MAXIMUM_LENGTH))
      case _ => Right(())
    }
  }

  val checkPayIdNameAvailabilityMessageProcessor: MessageProcessor = { receivedMessage =>
    parseCheckPayIdNameAvailabilityMessage(receivedMessage)
      .map(processCheckPayIdNameAvailabilityMessage)
  }

  private[services] def parseCheckPayIdNameAvailabilityMessage(
      message: ReceivedMessage
  ): Option[CheckPayIdNameAvailabilityMessage] = {
    Try(AtalaMessage.parseFrom(message.message.toByteArray)).toOption
      .flatMap(_.message.mirrorMessage)
      .flatMap(_.message.checkPayIdNameAvailabilityMessage)
  }

  def processCheckPayIdNameAvailabilityMessage(
      payIdMessage: CheckPayIdNameAvailabilityMessage
  ): MessageProcessorResult = {
    val payIdName = PayIdName(payIdMessage.nameToCheck)

    (for {
      _ <- EitherT.fromEither[Task](verifyPayIdString(payIdName.name))
      connectionWithTheSamePayIdName <- EitherT.liftF[Task, PrismError, Option[Connection]] {
        ConnectionDao
          .findByPayIdName(payIdName)
          .logSQLErrors("check payId name availability", logger)
          .transact(tx)
      }
      available = connectionWithTheSamePayIdName.isEmpty
      response = AtalaMessage()
        .withMirrorMessage(
          MirrorMessage().withCheckPayIdNameAvailabilityResponse(CheckPayIdNameAvailabilityResponse(available))
        )
    } yield Some(response)).value
  }

  val getPayIdNameMessageProcessor: MessageProcessor = { receivedMessage =>
    parseGetPayIdNameMessage(receivedMessage)
      .as(processGetPayIdNameMessage(receivedMessage))
  }

  private[services] def parseGetPayIdNameMessage(
      message: ReceivedMessage
  ): Option[GetPayIdNameMessage] = {
    Try(AtalaMessage.parseFrom(message.message.toByteArray)).toOption
      .flatMap(_.message.mirrorMessage)
      .flatMap(_.message.getPayIdNameMessage)
  }

  def processGetPayIdNameMessage(receivedMessage: ReceivedMessage): MessageProcessorResult = {
    (for {
      connection <- EitherT(
        ConnectionUtils
          .fromReceivedMessage(receivedMessage, ConnectionDao.findByConnectionId)
          .logSQLErrors("getting connection by received message", logger)
          .transact(tx)
      )

      response = AtalaMessage()
        .withMirrorMessage(
          MirrorMessage()
            .withGetPayIdNameResponse(GetPayIdNameResponse(payIdName = connection.payIdName.map(_.name).getOrElse("")))
        )

    } yield Some(response)).value
  }

  val getPayIdAddressesMessageProcessor: MessageProcessor = { receivedMessage =>
    parseGetPayIdAddressesMessage(receivedMessage)
      .as(processGetPayIdAddressesMessage(receivedMessage))
  }

  private[services] def parseGetPayIdAddressesMessage(
      message: ReceivedMessage
  ): Option[GetPayIdAddressesMessage] = {
    Try(AtalaMessage.parseFrom(message.message.toByteArray)).toOption
      .flatMap(_.message.mirrorMessage)
      .flatMap(_.message.getPayIdAddressesMessage)
  }

  def processGetPayIdAddressesMessage(receivedMessage: ReceivedMessage): MessageProcessorResult = {
    (for {
      allAddresses <- fetchAddressesForConnection(receivedMessage)
      (cardanoAddressesInfo, walletAddressesWithWalletName) = allAddresses

      manuallyRegisteredCardanoAddresses = cardanoAddressesInfo.map { cardanoAddress =>
        ManuallyRegisteredCardanoAddress(
          address = cardanoAddress.cardanoAddress.value,
          registrationDate = Some(Timestamp(seconds = cardanoAddress.registrationDate.date.getEpochSecond))
        )
      }

      generatedCardanoAddresses = walletAddressesWithWalletName.map { walletAddress =>
        GeneratedCardanoAddress(
          address = walletAddress.cardanoWalletAddress.address.value,
          walletName = walletAddress.walletName.getOrElse(""),
          sequenceNumber = walletAddress.cardanoWalletAddress.sequenceNo.toLong,
          usedAt =
            walletAddress.cardanoWalletAddress.usedAt.map(usedAt => Timestamp(seconds = usedAt.date.getEpochSecond))
        )
      }

      response = AtalaMessage()
        .withMirrorMessage(
          MirrorMessage()
            .withGetPayIdAddressesResponse(
              GetPayIdAddressesResponse(
                manuallyRegisteredCardanoAddresses,
                generatedCardanoAddresses
              )
            )
        )

    } yield Some(response)).value
  }

  private def fetchAddressesForConnection(
      receivedMessage: ReceivedMessage
  ): EitherT[Task, PrismError, (List[CardanoAddressInfo], List[CardanoWalletAddressWithWalletName])] = {
    (for {
      connection <- EitherT(
        ConnectionUtils
          .fromReceivedMessage(receivedMessage, ConnectionDao.findByConnectionId)
          .logSQLErrors("getting connection by received message", logger)
      )

      cardanoAddressesInfo <- EitherT.liftF[ConnectionIO, PrismError, List[CardanoAddressInfo]](
        CardanoAddressInfoDao
          .findBy(connection.token)
          .logSQLErrors("getting manually registered cardano addresses for connection", logger)
      )

      walletAddressesWithWalletName <-
        EitherT.liftF[ConnectionIO, PrismError, List[CardanoWalletAddressWithWalletName]](
          CardanoWalletAddressDao
            .findByConnectionTokenWithWalletName(connection.token)
            .logSQLErrors("getting generated cardano addresses for connection", logger)
        )
    } yield (cardanoAddressesInfo, walletAddressesWithWalletName)).transact(tx)
  }
}

object CardanoAddressInfoService {
  case class PaymentInformationDecodingFailure(payIdMessage: PayIdMessage, error: io.circe.Error) extends PrismError {
    override def toStatus: Status = {
      Status.INVALID_ARGUMENT.withDescription(
        s"Could not parse payment information: ${payIdMessage.paymentInformation} error: ${error.getMessage}"
      )
    }
  }

  case class InvalidPayIdName(paymentInformation: PaymentInformation) extends PrismError {
    override def toStatus: Status = {
      Status.INVALID_ARGUMENT.withDescription(
        s"Cannot split payId to prefix and host address: ${paymentInformation.payId}"
      )
    }
  }

  case class ConnectionDoesNotContainPayIdInfo(connection: Connection, payIdPrefix: String) extends PrismError {
    private val message = connection.holderDID match {
      case Some(_) =>
        s"holderDID from connection: ${connection.holderDID} doesn't match holderDID parsed from paymentId: $payIdPrefix"
      case None => "holderDID from connection is not defined"
    }

    override def toStatus: Status = {
      Status.INVALID_ARGUMENT.withDescription(
        s"$message and payIdName from connection ${connection.payIdName} doesn't match payIdName parsed from paymentId $payIdPrefix"
      )
    }
  }

  case class PayIdHostAddressDoesNotMach(httpConfig: HttpConfig, payIdHostAddress: String) extends PrismError {
    override def toStatus: Status = {
      Status.INVALID_ARGUMENT.withDescription(
        s"payIdHostAddress from config: ${httpConfig.payIdHostAddress} " +
          s"doesn't match host address from payment information: $payIdHostAddress"
      )
    }
  }

  case class VerifiedAddressesWithNotValidSignature(verifiedAddresses: List[VerifiedAddress]) extends PrismError {
    override def toStatus: Status = {
      Status.INVALID_ARGUMENT.withDescription(
        "Some of the verified addresses have not valid signature. " +
          "Details of those addresses:" +
          verifiedAddresses.map(_.content.payload.payIdAddress.addressDetails.toString).mkString(", ")
      )
    }
  }

  case class CryptoAddressDetailsMissing(addresses: List[Address]) extends PrismError {
    override def toStatus: Status = {
      Status.INVALID_ARGUMENT.withDescription(
        "Some of the addresses do not contain crypto address details: " +
          "Details of those addresses:" +
          addresses.map(_.addressDetails).mkString(", ")
      )
    }
  }

  case class CardanoAddressAlreadyExists(addresses: List[CardanoAddressInfo]) extends PrismError {
    override def toStatus: Status = {
      Status.ALREADY_EXISTS.withDescription(
        "Cardano addresses already exists in database" +
          addresses.map(_.cardanoAddress).mkString(", ")
      )
    }
  }

  case class PayIdNameNotAllowedCharacters(payIdName: String, allowedCharacters: Regex) extends PrismError {
    override def toStatus: Status = {
      Status.INVALID_ARGUMENT.withDescription(
        s"Cannot register pay id name: $payIdName, name contains not allowed characters. " +
          s"Allowed characters regex: $allowedCharacters"
      )
    }
  }

  case class PayIdNameIncorrectLength(payIdName: String, minimumLength: Int, maximumLength: Int) extends PrismError {
    override def toStatus: Status = {
      Status.INVALID_ARGUMENT.withDescription(
        s"Cannot register pay id name: $payIdName, " +
          s"name's length must be between $minimumLength and $maximumLength characters long"
      )
    }
  }

  case class ConnectionHasAlreadyRegisteredPayIdName(payIdName: String, connection: Connection) extends PrismError {
    override def toStatus: Status = {
      Status.ALREADY_EXISTS.withDescription(
        s"Cannot register pay id name: $payIdName, " +
          s"connection: ${connection.token} has already registered name: ${connection.payIdName}"
      )
    }
  }
}
