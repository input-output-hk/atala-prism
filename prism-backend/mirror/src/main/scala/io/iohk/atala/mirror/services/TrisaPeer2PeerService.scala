package io.iohk.atala.mirror.services

import cats.data.EitherT
import cats.implicits._
import com.google.protobuf.any.Any
import io.grpc.stub.StreamObserver
import io.iohk.atala.mirror.models.CardanoAddress
import io.iohk.atala.mirror.protos.ivms101.{Beneficiary, IdentityPayload}
import io.iohk.atala.mirror.protos.trisa.{Transaction, TransactionData, TrisaPeer2PeerGrpc}
import io.iohk.atala.mirror.protos.trisa_cardano_data.CardanoData
import io.iohk.atala.prism.mirror.trisa.TrisaAesGcm
import monix.eval.Task
import org.slf4j.LoggerFactory
import monix.execution.Scheduler.Implicits.global

class TrisaPeer2PeerService(mirrorService: MirrorService) extends TrisaPeer2PeerGrpc.TrisaPeer2Peer {

  private val logger = LoggerFactory.getLogger(classOf[TrisaPeer2PeerService])

  override def transactionStream(responseObserver: StreamObserver[Transaction]): StreamObserver[Transaction] =
    new StreamObserver[Transaction] {
      override def onNext(value: Transaction): Unit = {
        (for {
          cardanoData <- parseCardanoData(value).toEitherT[Task]
          person <- EitherT(
            mirrorService
              .getIdentityInfoForAddress(CardanoAddress(cardanoData.destination))
              .map(_.toRight("Cardano address not found"))
          )
          transactionData = TransactionData(
            identity = Some(
              Any(
                value = IdentityPayload(beneficiary = Some(Beneficiary(beneficiaryPersons = Seq(person)))).toByteString,
                typeUrl = "type.googleapis.com/ivms101.IdentityPayload"
              )
            )
          )
          transaction <- TrisaAesGcm.encryptTransactionData(transactionData).leftMap(_.message).toEitherT[Task]
          transactionWithId = transaction.withId(value.id)
          _ = responseObserver.onNext(transactionWithId)
        } yield ()).value
          .runSyncUnsafe()
          .left
          .foreach(errorMessage => logger.error(s"Error occured when processing transaction: $errorMessage"))
      }

      override def onError(t: Throwable): Unit = {
        logger.info(s"Error occurred ${t.getMessage}")
      }

      override def onCompleted(): Unit = {
        logger.info("Trisa stream completed")
      }
    }

  private def parseCardanoData(value: Transaction): Either[String, CardanoData] = {
    for {
      transactionDataByteArray <-
        TrisaAesGcm
          .decrypt(value)
          .leftMap(e => s"Cannot decrypt trisa transaction: ${e.message}")

      transactionData <-
        TransactionData
          .validate(transactionDataByteArray)
          .toEither
          .leftMap(e => s"Error occurred when parsing transaction data: ${e.getMessage}")

      data <- transactionData.data.toRight("TransactionData doesn't contain data")

      cardanoData <-
        CardanoData
          .validate(data.value.toByteArray)
          .toEither
          .leftMap(e => s"Error occurred when parsing cardano data: ${e.getMessage}")
    } yield cardanoData
  }

}
