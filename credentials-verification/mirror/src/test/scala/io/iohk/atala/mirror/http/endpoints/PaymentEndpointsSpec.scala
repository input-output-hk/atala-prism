package io.iohk.atala.mirror.http.endpoints

import io.iohk.atala.mirror.MirrorFixtures
import io.iohk.atala.mirror.config.HttpConfig
import io.iohk.atala.mirror.http.models.payid.AddressDetails.CryptoAddressDetails
import io.iohk.atala.mirror.http.models.payid.PaymentInformation
import io.iohk.atala.mirror.services.CardanoAddressInfoService
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import monix.execution.Scheduler.Implicits.global
import org.http4s._
import org.http4s.implicits._
import io.iohk.atala.mirror.http.models.payid._
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder

// sbt "project mirror" "testOnly *endpoints.PaymentEndpointsSpec"
class PaymentEndpointsSpec extends PostgresRepositorySpec with MirrorFixtures {
  import ConnectionFixtures._, CardanoAddressInfoFixtures._

  "GET payId" should {
    "return BadRequest when PayId-Version header is not supplied" in new PaymentEndpointsFixtures {
      val response = service
        .run(Request(method = Method.GET, uri = uri"/somePayId", headers = Headers.of(acceptHeader)))
        .runSyncUnsafe()

      response.status mustBe Status.BadRequest
    }

    "return BadRequest when Accept header is not supplied or incorrect" in new PaymentEndpointsFixtures {
      val response = service
        .run(Request(method = Method.GET, uri = uri"/somePayId", headers = Headers.of(payIdHeader)))
        .runSyncUnsafe()

      response.status mustBe Status.BadRequest
    }

    "return BadRequest when given payId doesn't exist" in new PaymentEndpointsFixtures {
      val response = service.run(Request(method = Method.GET, uri = uri"/somePayId", headers = headers)).runSyncUnsafe()

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

      paymentInformation.addresses.size mustBe 2
      paymentInformation.addresses.foreach { address =>
        address.paymentNetwork mustBe "CARDANO"
        address.environment mustBe Some("TESTNET")
        address.addressDetailsType mustBe AddressDetailsType.CryptoAddress
      }
      paymentInformation.addresses(0).addressDetails mustBe CryptoAddressDetails(
        cardanoAddressInfo2.cardanoAddress.address,
        None
      )
      paymentInformation.addresses(1).addressDetails mustBe CryptoAddressDetails(
        cardanoAddressInfo3.cardanoAddress.address,
        None
      )
    }
  }

  trait PaymentEndpointsFixtures {
    val cardanoAddressInfoService = new CardanoAddressInfoService(databaseTask)
    val httpConfig = HttpConfig(8080, "localhost")
    val paymentEndpoints = new PaymentEndpoints(cardanoAddressInfoService, httpConfig)
    val service = paymentEndpoints.service.orNotFound

    val payIdHeader = Header("PayId-Version", "1.0")
    val acceptHeader = Header("Accept", "application/cardano-testnet+json")
    val headers = Headers.of(payIdHeader, acceptHeader)
  }

}
