package io.iohk.atala.prism.mirror.payid

import io.circe.Encoder

import io.iohk.atala.prism.jose.{Jwa, JwsHeader}
import io.iohk.atala.prism.jose.ec.{EcJwk, EcJws}
import io.iohk.atala.prism.crypto.{ECTrait, ECKeyPair}
import io.iohk.atala.prism.jose.ec.EcJwsContent

case class PayID(value: String) extends AnyVal

sealed abstract class AddressDetails(val addressDetailsType: String)

case class CryptoAddressDetails(
    address: String,
    tag: Option[String]
) extends AddressDetails("CryptoAddressDetails")

case class FiatAddressDetails(
    accountNumber: String,
    routingNumber: Option[String]
) extends AddressDetails("FiatAddressDetails")

case class Address(
    paymentNetwork: String,
    environment: Option[String],
    addressDetails: AddressDetails
)

case class VerifiedAddressWrapper(
    payId: PayID,
    payIdAddress: Address
)

object Address {

  type VerifiedAddress = EcJws[VerifiedAddressWrapper]

  object VerifiedAddress {
    def apply(address: VerifiedAddressWrapper, keys: ECKeyPair, keyId: String)(implicit
        ec: ECTrait,
        ep: Encoder[VerifiedAddressWrapper],
        eh: Encoder[JwsHeader[EcJwk]]
    ): VerifiedAddress = {
      val jwk = EcJwk(publicKey = keys.publicKey, didId = Some(keyId))
      val jwsHeader = JwsHeader(
        alg = Jwa.ES256K,
        jwk = jwk,
        typ = Some("JOSE+JSON"),
        crit = Some(Seq("b64", "name")),
        name = Some("identityKey"),
        b64 = Some(false)
      )

      EcJwsContent(jwsHeader, address).sign(keys.privateKey)
    }
  }
}

case class PaymentInformation(
    payId: Option[PayID],
    version: Option[String],
    addresses: Seq[Address],
    verifiedAddresses: Seq[Address.VerifiedAddress],
    memo: Option[String]
)
