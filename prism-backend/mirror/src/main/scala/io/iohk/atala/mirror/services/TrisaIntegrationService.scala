package io.iohk.atala.mirror.services

import com.google.protobuf.any.{Any => ProtobufAny}
import io.grpc.netty.{GrpcSslContexts, NettyChannelBuilder}
import io.iohk.atala.mirror.protos.trisa.{Transaction, TransactionData, TrisaPeer2PeerGrpc}
import io.grpc.stub.StreamObserver
import io.iohk.atala.mirror.config.TrisaConfig
import io.iohk.atala.mirror.models.{CardanoAddress, LovelaceAmount, TrisaVaspAddress}
import io.iohk.atala.mirror.protos.ivms101.{IdentityPayload, Person}
import io.iohk.atala.mirror.protos.trisa_cardano_data.CardanoData
import io.iohk.atala.prism.mirror.trisa.TrisaAesGcm
import monix.eval.Task
import monix.execution.CancelablePromise
import org.slf4j.LoggerFactory

import java.io.File
import java.util.concurrent.TimeoutException
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._
import cats.data.EitherT
import cats.implicits._

import java.util.UUID

trait TrisaIntegrationService {
  def initiateTransaction(
      source: CardanoAddress,
      destination: CardanoAddress,
      lovelaceAmount: LovelaceAmount,
      trisaVaspAddress: TrisaVaspAddress
  ): Task[Either[Throwable, Person]]
}

class TrisaIntegrationServiceImpl(trisaConfig: TrisaConfig) extends TrisaIntegrationService {
  private val REQUEST_TIMEOUT = 10.seconds

  private val logger = LoggerFactory.getLogger(classOf[TrisaIntegrationServiceImpl])

  private val sslContext = {
    val sslContext = GrpcSslContexts.forClient()

    val sslConfig = trisaConfig.sslConfig

    sslContext.trustManager(new File(sslConfig.serverTrustChainLocation))
    sslContext.keyManager(
      new File(sslConfig.serverCertificateLocation),
      new File(sslConfig.serverCertificatePrivateKeyLocation)
    )

    sslContext.build()
  }

  private def connect(trisaVaspAddress: TrisaVaspAddress) = {
    val channel = NettyChannelBuilder
      .forAddress(trisaVaspAddress.host, trisaVaspAddress.port)
      .sslContext(sslContext)
      .build()

    (TrisaPeer2PeerGrpc.stub(channel), channel)
  }

  private def sendRequest(
      transaction: Transaction,
      trisaVaspAddress: TrisaVaspAddress
  ): Task[Either[Throwable, Transaction]] = {
    Try {
      val transactionResponsePromise = CancelablePromise[Either[Throwable, Transaction]]()

      val responseObserver = new StreamObserver[Transaction] {
        override def onNext(value: Transaction): Unit = {
          transactionResponsePromise.tryComplete(Try(Right(value)))
          ()
        }

        override def onError(throwable: Throwable): Unit = {
          transactionResponsePromise.tryComplete(Try(Left(throwable)))
          ()
        }

        override def onCompleted(): Unit = {
          logger.info("Trisa stream completed")
        }
      }

      val (stub, channel) = connect(trisaVaspAddress)
      val stream = stub.transactionStream(responseObserver)
      val transactionWithId = transaction.withId(UUID.randomUUID().toString)
      stream.onNext(transactionWithId)
      stream.onCompleted()
      Task
        .fromCancelablePromise(transactionResponsePromise)
        .timeoutTo(
          REQUEST_TIMEOUT,
          Task.pure(Left(new TimeoutException("Timeout occurred when waiting for response from trisa vasp")))
        )
        .guarantee(Task.pure(channel.shutdown()).void)
    } match {
      case Success(value) => value
      case Failure(exception) => Task.pure(Left(exception))
    }
  }

  override def initiateTransaction(
      source: CardanoAddress,
      destination: CardanoAddress,
      lovelaceAmount: LovelaceAmount,
      trisaVaspAddress: TrisaVaspAddress
  ): Task[Either[Throwable, Person]] = {
    val transactionData = TransactionData(
      data = Some(
        ProtobufAny(
          value = CardanoData(source.value, destination.value, lovelaceAmount.amount).toByteString,
          typeUrl = "type.googleapis.com/trisa.protocol.v1alpha1.CardanoData"
        )
      )
    )

    (for {
      transactionToSend <- TrisaAesGcm.encryptTransactionData(transactionData).toEitherT[Task]
      transactionResponse <- EitherT(sendRequest(transactionToSend, trisaVaspAddress))
      person <- parseIdentityData(transactionResponse).toEitherT[Task]
    } yield person).value
  }

  private def parseIdentityData(value: Transaction): Either[Throwable, Person] = {
    val decryptionResult = TrisaAesGcm
      .decrypt(value)
      .left
      .map(exception => new RuntimeException(s"Cannot decrypt trisa transaction: ${exception.message}"))

    for {
      transactionDataByteArray <- decryptionResult
      transactionData <-
        TransactionData
          .validate(transactionDataByteArray)
          .toEither
          .leftMap(e => new RuntimeException(s"Error occurred when parsing transaction data: ${e.getMessage}"))

      identity <- transactionData.identity.toRight(new RuntimeException("TransactionData doesn't contain identity"))

      identityPayload <-
        IdentityPayload
          .validate(identity.value.toByteArray)
          .toEither
          .leftMap(e => new RuntimeException(s"Error occurred when parsing Identity payload: ${e.getMessage}"))

      beneficiary <- identityPayload.beneficiary.toRight(
        new RuntimeException("Beneficiary data is missing in identity payload")
      )

      person <- beneficiary.beneficiaryPersons.headOption.toRight(
        new RuntimeException("Beneficiary data persons array is empty")
      )
    } yield person
  }
}
