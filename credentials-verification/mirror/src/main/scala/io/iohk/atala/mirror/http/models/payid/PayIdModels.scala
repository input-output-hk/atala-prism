package io.iohk.atala.mirror.http.models.payid

import io.circe._
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.syntax._
import cats.syntax.functor._

case class PaymentInformation(
    addresses: List[Address],
    payId: Option[String],
    memo: Option[String]
)

object PaymentInformation {
  implicit val paymentInformationEncoder: Encoder[PaymentInformation] = deriveEncoder[PaymentInformation]
  implicit val paymentInformationDecoder: Decoder[PaymentInformation] = deriveDecoder[PaymentInformation]
}

trait AddressDetailsType {
  val value: String
}

object AddressDetailsType {
  case object CryptoAddress extends AddressDetailsType {
    override val value = "CryptoAddressDetails"
  }
  case object FiatAddress extends AddressDetailsType {
    override val value = "FiatAddressDetails"
  }

  implicit val addressDetailsTypeEncoder: Encoder[AddressDetailsType] =
    (addressDetailsType: AddressDetailsType) => Json.fromString(addressDetailsType.value)

  implicit val addressDetailsTypeDecoder: Decoder[AddressDetailsType] = Decoder.decodeString.emap {
    case CryptoAddress.value => Right(CryptoAddress)
    case FiatAddress.value => Right(FiatAddress)
    case _ => Left(s"AddressDetails is not one of the following value: ${CryptoAddress.value} or ${FiatAddress.value}")
  }
}

trait AddressDetails

object AddressDetails {
  case class CryptoAddressDetails(
      address: String,
      tag: Option[String]
  ) extends AddressDetails

  case class FiatAddressDetails(
      accountNumber: String,
      routingNumber: Option[String]
  ) extends AddressDetails

  implicit val addressDetailsEncoder: Encoder[AddressDetails] = Encoder.instance {
    case cryptoAddressDetails: CryptoAddressDetails => cryptoAddressDetails.asJson
    case fiatAddressDetails: FiatAddressDetails => fiatAddressDetails.asJson
  }

  implicit val addressDetailsDecoder: Decoder[AddressDetails] =
    List[Decoder[AddressDetails]](
      Decoder[CryptoAddressDetails].widen,
      Decoder[FiatAddressDetails].widen
    ).reduceLeft(_ or _)

}

case class Address(
    paymentNetwork: String,
    environment: Option[String],
    addressDetailsType: AddressDetailsType,
    addressDetails: AddressDetails
)

object Address {
  implicit val addressEncoder: Encoder[Address] = deriveEncoder[Address]
  implicit val addressDecoder: Decoder[Address] = deriveDecoder[Address]
}
