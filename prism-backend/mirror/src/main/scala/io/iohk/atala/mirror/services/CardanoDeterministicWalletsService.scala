package io.iohk.atala.mirror.services

import java.time.Instant

import scala.util.Try

import cats.data.EitherT
import monix.eval.Task
import io.grpc.Status
import org.slf4j.LoggerFactory
import doobie.util.transactor.Transactor

import io.iohk.atala.prism.services.MessageProcessor
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.protos.credential_models.{
  AtalaMessage,
  RegisterWalletMessage,
  MirrorMessage,
  WalletRegistered
}
import io.iohk.atala.mirror.models.CardanoWallet
import io.iohk.atala.mirror.db.CardanoWalletDao
import io.iohk.atala.prism.utils.ConnectionUtils
import io.iohk.atala.mirror.db.ConnectionDao
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.errors.PrismError
import io.iohk.atala.mirror.models.CardanoWalletAddress
import io.iohk.atala.mirror.db.CardanoWalletAddressDao
import io.iohk.atala.mirror.config.CardanoConfig

import cats.implicits._
import doobie.implicits._

class CardanoDeterministicWalletsService(
    tx: Transactor[Task],
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

      walletId <- EitherT.liftF[Task, PrismError, CardanoWallet.Id](
        CardanoWalletDao
          .insert(
            CardanoWallet(
              id = CardanoWallet.Id.random(),
              name = Option(registerWalletMessage.name).filter(_.nonEmpty),
              connectionToken = connection.token,
              extendedPublicKey = registerWalletMessage.extendedPublicKey,
              lastGeneratedNo = cardanoConfig.addressCount,
              lastUsedNo = 0,
              registrationDate = CardanoWallet.RegistrationDate(Instant.now())
            )
          )
          .transact(tx)
      )

      addresses <- EitherT.fromEither[Task](
        (0 until cardanoConfig.addressCount).toList
          .map(sequenceNo =>
            cardanoAddressService
              .generateWalletAddress(registerWalletMessage.extendedPublicKey, sequenceNo, cardanoConfig.network.name)
          )
          .sequence
      )

      _ <- EitherT.liftF[Task, PrismError, Int](
        CardanoWalletAddressDao.insertMany
          .updateMany(
            addresses.mapWithIndex((address, sequenceNo) =>
              CardanoWalletAddress(
                address = address,
                walletId = walletId,
                sequenceNo = sequenceNo,
                usedAt = None
              )
            )
          )
          .transact(tx)
      )

      atalaMessage = AtalaMessage().withMirrorMessage(
        MirrorMessage().withWalletRegistered(
          WalletRegistered().withId(walletId.uuid.toString)
        )
      )
    } yield Some(atalaMessage)).value
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
}
