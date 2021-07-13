package io.iohk.atala.mirror.services

import cats.data.EitherT
import cats.implicits._
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.grpc.Status
import io.iohk.atala.mirror.config.CardanoConfig
import io.iohk.atala.mirror.db.{CardanoDBSyncDao, CardanoWalletAddressDao, CardanoWalletDao, ConnectionDao}
import io.iohk.atala.mirror.models.{CardanoAddress, CardanoAddressBlockInfo, CardanoWallet, CardanoWalletAddress}
import io.iohk.atala.prism.errors.PrismError
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.protos.credential_models.{
  AtalaMessage,
  MirrorMessage,
  RegisterWalletMessage,
  WalletRegistered
}
import io.iohk.atala.prism.services.MessageProcessor
import io.iohk.atala.prism.utils.ConnectionUtils
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import monix.eval.Task
import org.slf4j.LoggerFactory

import java.time.Instant
import scala.util.Try

class CardanoDeterministicWalletsService(
    tx: Transactor[Task],
    cardanoDbSyncTx: Transactor[Task],
    cardanoAddressService: CardanoAddressService,
    cardanoConfig: CardanoConfig
) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  val registerWalletMessageProcessor: MessageProcessor = { receivedMessage =>
    parseRegisterWalletMessage(receivedMessage)
      .map(saveCardanoWallet(receivedMessage, _))
  }

  private def parseRegisterWalletMessage(message: ReceivedMessage): Option[RegisterWalletMessage] = {
    Try(AtalaMessage.parseFrom(message.message.toByteArray)).toOption
      .flatMap(_.message.mirrorMessage)
      .flatMap(_.message.registerWalletMessage)
  }

  private[services] def saveCardanoWallet(
      receivedMessage: ReceivedMessage,
      registerWalletMessage: RegisterWalletMessage
  ): MessageProcessor.MessageProcessorResult = {
    (for {
      connection <- EitherT(
        ConnectionUtils
          .fromReceivedMessage(receivedMessage, ConnectionDao.findByConnectionId)
          .logSQLErrors("getting connection from received message", logger)
          .transact(tx)
      )

      cardanoWallet = CardanoWallet(
        id = CardanoWallet.Id.random(),
        name = Option(registerWalletMessage.name).filter(_.nonEmpty),
        connectionToken = connection.token,
        extendedPublicKey = registerWalletMessage.extendedPublicKey,
        lastGeneratedNo = cardanoConfig.addressCount - 1,
        lastUsedNo = None,
        registrationDate = CardanoWallet.RegistrationDate(Instant.now())
      )

      walletId <- EitherT(
        CardanoWalletDao
          .insert(cardanoWallet)
          .transact(tx)
          .attempt
      ).leftMap(_ => CardanoDeterministicWalletsService.CardanoWalletExists)

      _ <- EitherT(generateAddresses(cardanoWallet))

      messageContent = WalletRegistered(
        id = walletId.uuid.toString,
        name = registerWalletMessage.name,
        extendedPublicKey = registerWalletMessage.extendedPublicKey
      )
      atalaMessage = AtalaMessage().withMirrorMessage(
        MirrorMessage().withWalletRegistered(messageContent)
      )
    } yield Some(atalaMessage)).value
  }

  private[services] def generateAddresses(
      cardanoWallet: CardanoWallet
  ): Task[Either[PrismError, Unit]] = {
    val initialSequenceNo = 0
    Task.tailRecM(initialSequenceNo) { currentSequenceNo =>
      cardanoAddressService.generateWalletAddresses(
        cardanoWallet.extendedPublicKey,
        currentSequenceNo,
        currentSequenceNo + cardanoConfig.addressCount,
        cardanoConfig.network.name
      ) match {
        case Left(error) => // cannot generate cardano addresses
          Task.pure(
            Right( // end iteration
              Left(error) // with error
            )
          )
        case Right(addressesWithSequenceNo) =>
          processGeneratedAddresses(cardanoWallet.id, addressesWithSequenceNo).map { continueIteration =>
            if (continueIteration) {
              val nextSequenceNo = currentSequenceNo + cardanoConfig.addressCount
              Left(nextSequenceNo)
            } else {
              Right( // end iteration
                Right(()) // with success
              )
            }
          }
      }
    }
  }

  private[services] def processGeneratedAddresses(
      cardanoWalletId: CardanoWallet.Id,
      addressesWithSequenceNo: List[(CardanoAddress, Int)]
  ): Task[Boolean] = {
    for {
      _ <- insertNewAddresses(cardanoWalletId, addressesWithSequenceNo)

      usedAddresses <-
        CardanoDBSyncDao
          .findUsedAddresses(addressesWithSequenceNo.map { case (address, _) => address })
          .logSQLErrors("updating cardano wallet and cardano addresses usage", logger)
          .transact(cardanoDbSyncTx)

      _ <-
        if (usedAddresses.isEmpty) Task.unit
        else updateAddressesUsage(cardanoWalletId, addressesWithSequenceNo, usedAddresses).as(Left(()))

      continueIteration = usedAddresses.nonEmpty
    } yield continueIteration
  }

  private[services] def insertNewAddresses(
      cardanoWalletId: CardanoWallet.Id,
      addressesWithSequenceNo: List[(CardanoAddress, Int)]
  ): Task[Unit] = {
    val (_, lastGeneratedNo) = addressesWithSequenceNo.last
    (for {
      _ <-
        CardanoWalletAddressDao.insertMany
          .updateMany(
            addressesWithSequenceNo.map {
              case (address, sequenceNo) =>
                CardanoWalletAddress(
                  address = address,
                  walletId = cardanoWalletId,
                  sequenceNo = sequenceNo,
                  usedAt = None
                )
            }
          )
      _ <- CardanoWalletDao.updateLastGeneratedNo(cardanoWalletId, lastGeneratedNo)
    } yield ())
      .logSQLErrors("inserting new addresses and updating cardano wallet", logger)
      .transact(tx)
  }

  private[services] def updateAddressesUsage(
      cardanoWalletId: CardanoWallet.Id,
      addressesWithSequenceNo: List[(CardanoAddress, Int)],
      usedAddressesWithUsageInfo: List[CardanoAddressBlockInfo]
  ): Task[Unit] = {
    (for {
      _ <- usedAddressesWithUsageInfo.traverse { addressWithUsageInfo =>
        CardanoWalletAddressDao.updateUsedAt(
          addressWithUsageInfo.cardanoAddress,
          CardanoWalletAddress.UsedAt(addressWithUsageInfo.blockIssueTime)
        )
      }

      usedAddressesSet = usedAddressesWithUsageInfo.map(_.cardanoAddress).toSet

      (_, lastUsedAddressSequenceNo) =
        addressesWithSequenceNo
          .filter { case (address, _) => usedAddressesSet.contains(address) }
          .maxBy { case (_, sequenceNo) => sequenceNo }

      _ <- CardanoWalletDao.updateLastUsedNo(cardanoWalletId, lastUsedAddressSequenceNo)
    } yield ())
      .logSQLErrors("updating cardano wallet and cardano addresses usage", logger)
      .transact(tx)
  }

}

object CardanoDeterministicWalletsService {
  case class CardanoDeterministicWalletsServiceError(message: String) extends PrismError {
    override def toStatus: Status = {
      Status.INTERNAL.withDescription(
        s"Cardano deterministic wallet service error: $message"
      )
    }
  }

  case object CardanoWalletExists extends PrismError {
    override def toStatus: Status = {
      Status.ALREADY_EXISTS.withDescription(
        "Cardano Wallet with the given extended public key already exists."
      )
    }
  }
}
