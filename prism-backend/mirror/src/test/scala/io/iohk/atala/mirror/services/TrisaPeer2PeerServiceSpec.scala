package io.iohk.atala.mirror.services

import com.google.protobuf.any.Any
import io.grpc.stub.StreamObserver
import io.iohk.atala.mirror.models.CardanoAddress
import io.iohk.atala.mirror.protos.ivms101.{Beneficiary, IdentityPayload, Person}
import io.iohk.atala.mirror.protos.mirror_api.{
  CreateAccountResponse,
  GetCredentialForAddressRequest,
  GetCredentialForAddressResponse
}
import io.iohk.atala.mirror.protos.trisa.{Transaction, TransactionData}
import io.iohk.atala.mirror.protos.trisa_cardano_data.CardanoData
import io.iohk.atala.prism.mirror.trisa.TrisaAesGcm
import monix.eval.Task
import monix.execution.CancelablePromise
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.OptionValues._
import monix.execution.Scheduler.Implicits.global

import scala.util.Try
import scala.concurrent.duration._

class TrisaPeer2PeerServiceSpec extends AnyWordSpec with Matchers {

  "TrisaPeer2PeerService" should {
    "process incoming transaction" in new Fixtures {
      val transactionResponsePromise = CancelablePromise[Transaction]()

      val streamObserver = new StreamObserver[Transaction] {
        override def onNext(value: Transaction): Unit = {
          transactionResponsePromise.tryComplete(Try(value))
          ()
        }

        override def onError(t: Throwable): Unit = ???

        override def onCompleted(): Unit = ???
      }

      trisaPeer2PeerService.transactionStream(streamObserver).onNext(transaction)

      val responseTransaction = Task.fromCancelablePromise(transactionResponsePromise).runSyncUnsafe(1.second)

      TrisaAesGcm.decrypt(responseTransaction).map(TransactionData.parseFrom) mustBe Right(expectedTransactionData)
    }
  }

  trait Fixtures {
    val mockMirrorService = new MirrorService {
      override def createAccount: Task[CreateAccountResponse] = ???

      override def getCredentialForAddress(
          request: GetCredentialForAddressRequest
      ): Task[GetCredentialForAddressResponse] = ???

      override def getIdentityInfoForAddress(cardanoAddress: CardanoAddress): Task[Option[Person]] =
        Task.pure(Some(Person()))
    }

    val trisaPeer2PeerService = new TrisaPeer2PeerService(mockMirrorService)

    val transactionData = TransactionData(
      data = Some(
        Any(
          value = CardanoData(destination = "randomDestinationAddress").toByteString
        )
      )
    )

    val transaction = TrisaAesGcm.encryptTransactionData(transactionData).toOption.value

    val expectedTransactionData = TransactionData(
      identity = Some(
        Any(
          value = IdentityPayload(beneficiary = Some(Beneficiary(beneficiaryPersons = Seq(Person())))).toByteString,
          typeUrl = "type.googleapis.com/ivms101.IdentityPayload"
        )
      )
    )
  }
}
