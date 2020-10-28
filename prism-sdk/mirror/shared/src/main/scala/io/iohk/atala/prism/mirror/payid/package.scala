package io.iohk.atala.prism.mirror

import io.circe.{Encoder, Json, Decoder, HCursor}
import io.circe.syntax._

import io.iohk.atala.prism.jose.implicits._

package object payid {

  object implicits {

    implicit val encodePayID: Encoder[PayID] = (payId: PayID) => {
      payId.value.asJson
    }

    implicit val encodeCryptoAddressDetails: Encoder[CryptoAddressDetails] = (ad: CryptoAddressDetails) => {
      Json
        .obj(
          "address" -> ad.address.asJson,
          "tag" -> ad.tag.asJson
        )
        .dropNullValues
    }

    implicit val encodeACHAddressDetails: Encoder[FiatAddressDetails] = (ad: FiatAddressDetails) => {
      Json
        .obj(
          "accountNumber" -> ad.accountNumber.asJson,
          "routingNumber" -> ad.routingNumber.asJson
        )
        .dropNullValues
    }

    implicit val encodeAddressDetails: Encoder[AddressDetails] = Encoder.instance {
      case details: CryptoAddressDetails => details.asJson
      case details: FiatAddressDetails => details.asJson
    }

    implicit val encodeAddress: Encoder[Address] = (a: Address) => {
      Json
        .obj(
          "paymentNetwork" -> a.paymentNetwork.asJson,
          "environment" -> a.environment.asJson,
          "addressDetailsType" -> a.addressDetails.addressDetailsType.asJson,
          "addressDetails" -> a.addressDetails.asJson
        )
        .dropNullValues
    }

    implicit val encodeVerifiedAddressWrapper: Encoder[VerifiedAddressWrapper] = (w: VerifiedAddressWrapper) => {
      Json
        .obj(
          "payId" -> w.payId.asJson,
          "payIdAddress" -> w.payIdAddress.asJson
        )
        .dropNullValues
    }

    implicit val encodePaymentInformation: Encoder[PaymentInformation] = (pi: PaymentInformation) => {
      Json
        .obj(
          "payId" -> pi.payId.asJson,
          "version" -> pi.version.asJson,
          "addresses" -> pi.addresses.asJson,
          "verifiedAddresses" -> pi.verifiedAddresses.asJson,
          "memo" -> pi.memo.asJson
        )
        .dropNullValues
    }

    implicit lazy val decodePayID: Decoder[PayID] = (c: HCursor) => {
      for {
        payId <- c.as[String]
      } yield PayID(payId)
    }

    implicit lazy val decodeCryptoAddressDetails: Decoder[CryptoAddressDetails] = (c: HCursor) => {
      for {
        address <- c.get[String]("address")
        tag <- c.get[Option[String]]("tag")
      } yield CryptoAddressDetails(address, tag)
    }

    implicit lazy val decodeFiatAddressDetails: Decoder[FiatAddressDetails] = (c: HCursor) => {
      for {
        accountNumber <- c.get[String]("accountNumber")
        routingNumber <- c.get[Option[String]]("routingNumber")
      } yield FiatAddressDetails(accountNumber, routingNumber)
    }

    implicit lazy val decodeAddress: Decoder[Address] = (c: HCursor) => {
      for {
        paymentNetwork <- c.get[String]("paymentNetwork")
        environment <- c.get[Option[String]]("environment")
        addressDetails <- c.get[String]("addressDetailsType").flatMap {
          case "CryptoAddressDetails" =>
            c.get[CryptoAddressDetails]("addressDetails")
          case "FiatAddressDetails" =>
            c.get[FiatAddressDetails]("addressDetails")
        }
      } yield Address(paymentNetwork, environment, addressDetails)
    }

    implicit lazy val decodeVerifiedAddressWrapper: Decoder[VerifiedAddressWrapper] = (c: HCursor) => {
      for {
        payId <- c.get[PayID]("payId")
        address <- c.get[Address]("payIdAddress")
      } yield VerifiedAddressWrapper(payId, address)
    }

    implicit val decodePaymentInformation: Decoder[PaymentInformation] =
      (c: HCursor) => {
        for {
          payId <- c.get[Option[PayID]]("payId")
          version <- c.get[Option[String]]("version")
          addresses <- c.get[Seq[Address]]("addresses")
          verifiedAddresses <- c.get[Seq[Address.VerifiedAddress]]("verifiedAddresses")
          memo <- c.get[Option[String]]("memo")
        } yield PaymentInformation(
          payId = payId,
          version = version,
          addresses = addresses,
          verifiedAddresses = verifiedAddresses,
          memo = memo
        )
      }
  }

}
