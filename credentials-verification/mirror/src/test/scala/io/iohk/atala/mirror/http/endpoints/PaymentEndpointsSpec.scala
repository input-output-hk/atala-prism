package io.iohk.atala.mirror.http.endpoints

import io.iohk.atala.mirror.MirrorFixtures
import io.iohk.atala.mirror.services.CardanoAddressInfoService
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import monix.execution.Scheduler.Implicits.global
import org.http4s._
import org.http4s.implicits._
import io.iohk.atala.prism.mirror.payid._
import io.iohk.atala.prism.mirror.payid.implicits._
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder

// sbt "project mirror" "testOnly *endpoints.PaymentEndpointsSpec"
class PaymentEndpointsSpec extends PostgresRepositorySpec with MirrorFixtures {
  import ConnectionFixtures._, CardanoAddressInfoFixtures._, CredentialFixtures._

  "GET payId" should {
    "return BadRequest when PayId-Version header is not supplied" in new PaymentEndpointsFixtures {
      val response = service
        .run(Request(method = Method.GET, uri = uri"/did:prism:somepayid", headers = Headers.of(acceptHeader)))
        .runSyncUnsafe()

      response.status mustBe Status.BadRequest
    }

    "return BadRequest when Accept header is not supplied or incorrect" in new PaymentEndpointsFixtures {
      val response = service
        .run(Request(method = Method.GET, uri = uri"/did:prism:somepayid", headers = Headers.of(payIdHeader)))
        .runSyncUnsafe()

      response.status mustBe Status.BadRequest
    }

    "return BadRequest when given payId doesn't exist" in new PaymentEndpointsFixtures {
      val response =
        service.run(Request(method = Method.GET, uri = uri"/did:prism:somepayid", headers = headers)).runSyncUnsafe()

      response.status mustBe Status.NotFound
    }

    "return payId information" in new PaymentEndpointsFixtures {
      (for {
        _ <- ConnectionFixtures.insertAll(databaseTask)
        _ <- CardanoAddressInfoFixtures.insertAll(databaseTask)
      } yield ()).runSyncUnsafe()

      val response = service
        .run(
          Request(method = Method.GET, uri = Uri.unsafeFromString(s"/${connectionHolderDid2.value}"), headers = headers)
        )
        .runSyncUnsafe()

      response.status mustBe Status.Ok

      val paymentInformation = response.as[PaymentInformation].runSyncUnsafe()

      paymentInformation.addresses.size mustBe 1
      val address = paymentInformation.addresses.head
      address.paymentNetwork mustBe "CARDANO"
      address.environment mustBe Some("TESTNET")
      address.addressDetails mustBe CryptoAddressDetails(
        cardanoAddressInfo3.cardanoAddress.address,
        None
      )

      paymentInformation.verifiedAddresses.size mustBe 1
      val verifiedAddress = paymentInformation.verifiedAddresses.head.content.payload.payIdAddress
      verifiedAddress.paymentNetwork mustBe "cardano"
      verifiedAddress.environment mustBe Some("testnet")
      verifiedAddress.addressDetails mustBe CryptoAddressDetails(
        cardanoAddressInfo2.cardanoAddress.address,
        None
      )
    }
  }

  trait PaymentEndpointsFixtures {
    val cardanoAddressInfoService =
      new CardanoAddressInfoService(databaseTask, mirrorConfig.httpConfig, defaultNodeClientStub)
    val paymentEndpoints = new PaymentEndpoints(cardanoAddressInfoService, mirrorConfig.httpConfig)
    val service = paymentEndpoints.service.orNotFound

    val payIdHeader = Header("PayId-Version", "1.0")
    val acceptHeader = Header("Accept", "application/cardano-testnet+json")
    val headers = Headers.of(payIdHeader, acceptHeader)
  }

}
