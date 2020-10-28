package io.iohk.atala.prism.mirror.payid

import io.circe.parser
import io.circe.syntax._

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.EitherValues

import io.iohk.atala.prism.crypto.ECTrait

import io.iohk.atala.prism.mirror.payid.implicits._
import io.iohk.atala.prism.jose.implicits._

abstract class ProtocolSpecBase(implicit ec: ECTrait) extends AnyWordSpec with Matchers with EitherValues {

  "PaymentInformation" should {
    "be encodable to json" in new Fixtures {
      pi.asJson mustBe parser.parse(s"""{
      |  "payId" : "iohk$$127.0.0.1",
      |  "addresses" : [
      |    {
      |      "paymentNetwork" : "BTC",
      |      "environment" : "MAINNET",
      |      "addressDetailsType" : "CryptoAddressDetails",
      |      "addressDetails" : {
      |        "address" : "2N3oefVeg6stiTb5Kh3ozCSkaqmx91FDbsm"
      |      }
      |    }
      |  ],
      |  "verifiedAddresses" : [
      |    ${pi.verifiedAddresses.head.asJson}
      |  ]
      |}""".stripMargin).getOrElse(fail())
    }

    "be decodable from json" in new Fixtures {
      val paymentInformation = parser.decode[PaymentInformation](s"""{
        |  "payId" : "test$$127.0.0.1",
        |  "addresses" : [
        |    {
        |      "paymentNetwork" : "BTC",
        |      "environment" : "TESTNET",
        |      "addressDetailsType" : "CryptoAddressDetails",
        |      "addressDetails" : {
        |        "address" : "2N3oefVeg6stiTb5Kh3ozCSkaqmx91FDbsm"
        |      }
        |    }
        |  ],
        |  "verifiedAddresses" : [
        |    ${pi.verifiedAddresses.head.asJson}
        |  ]
        |}""".stripMargin).getOrElse(fail())

      val va = paymentInformation.verifiedAddresses.head
      va.isValidSignature(va.content.protectedHeader.jwk.publicKey) mustBe true
    }
  }

  trait Fixtures {
    val keys = ec.generateKeyPair()

    val payId = PayID("iohk$127.0.0.1")

    val address = Address(
      paymentNetwork = "BTC",
      environment = Some("MAINNET"),
      addressDetails = CryptoAddressDetails(
        address = "2N3oefVeg6stiTb5Kh3ozCSkaqmx91FDbsm",
        tag = None
      )
    )

    val verifiedAddressWrapper = VerifiedAddressWrapper(
      payId = payId,
      payIdAddress = Address(
        paymentNetwork = "BTC",
        environment = Some("TESTNET"),
        addressDetails = CryptoAddressDetails(
          address = "2N7HHqnGx3CZnLdoMcKfnkFYoz8fJtsB6rE",
          tag = None
        )
      )
    )

    val verifiedAddress = Address.VerifiedAddress(verifiedAddressWrapper, keys, "master")

    val pi = PaymentInformation(
      payId = Some(payId),
      version = None,
      addresses = Seq(address),
      verifiedAddresses = Seq(verifiedAddress),
      memo = None
    )
  }

}
