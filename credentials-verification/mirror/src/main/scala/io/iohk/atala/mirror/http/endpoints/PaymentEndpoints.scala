package io.iohk.atala.mirror.http.endpoints

import io.iohk.atala.mirror.models.payid.AddressDetails.CryptoAddressDetails
import io.iohk.atala.mirror.models.CardanoAddressInfo.CardanoNetwork
import io.iohk.atala.mirror.models.CardanoAddressInfo
import io.iohk.atala.mirror.services.CardanoAddressInfoService
import monix.eval.Task
import org.http4s.{Headers, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.util.CaseInsensitiveString
import org.http4s.circe._
import io.circe.syntax._
import io.iohk.atala.mirror.config.HttpConfig
import io.iohk.atala.mirror.models.payid._
import io.iohk.atala.prism.identity.DID

class PaymentEndpoints(cardanoAddressInfoService: CardanoAddressInfoService, httpConfig: HttpConfig)
    extends Http4sDsl[Task] {

  private val PayIDVersionHeader = "PayID-Version"
  private val AcceptHeader = "Accept"

  val acceptHeaderToPaymentNetwork = Map(
    "application/cardano-mainnet+json" -> CardanoNetwork("mainnet"),
    "application/cardano-testnet+json" -> CardanoNetwork("testnet")
  )

  def parseAcceptHeader(headers: Headers): Option[CardanoNetwork] = {
    headers
      .find(_.name == CaseInsensitiveString(AcceptHeader))
      .flatMap(header => acceptHeaderToPaymentNetwork.get(header.value))
  }

  val service: HttpRoutes[Task] = HttpRoutes.of[Task] {
    case request @ GET -> Root / did =>
      val payIdHeader = request.headers.find(_.name == CaseInsensitiveString(PayIDVersionHeader))
      val cardanoNetwork = parseAcceptHeader(request.headers)

      (payIdHeader, cardanoNetwork) match {
        case (None, _) => BadRequest(s"$PayIDVersionHeader header is required")
        case (_, None) =>
          BadRequest(
            s"$AcceptHeader header must have one of " +
              s"the following value: ${acceptHeaderToPaymentNetwork.keys.mkString(", ")}"
          )

        case (_, Some(cardanoNetwork)) =>
          cardanoAddressInfoService.findPaymentInfo(DID(did), cardanoNetwork).flatMap {
            case None => NotFound()
            case Some((_, cardanoAddresses)) => Ok(toPaymentInformation(did, cardanoAddresses, cardanoNetwork).asJson)
          }
      }
  }

  def toPaymentInformation(
      did: String,
      addressesInfo: List[CardanoAddressInfo],
      cardanoNetwork: CardanoNetwork
  ): PaymentInformation = {
    PaymentInformation(
      addresses = addressesInfo.map(toPayIdAddress(_, cardanoNetwork)),
      verifiedAddresses = List.empty,
      payId = Some(did + "$" + httpConfig.payIdHostAddress),
      memo = None
    )
  }

  def toPayIdAddress(cardanoAddressInfo: CardanoAddressInfo, cardanoNetwork: CardanoNetwork): Address = {
    Address(
      paymentNetwork = "CARDANO",
      environment = Some(cardanoNetwork.network.toUpperCase),
      addressDetailsType = AddressDetailsType.CryptoAddress,
      addressDetails = CryptoAddressDetails(
        address = cardanoAddressInfo.cardanoAddress.address,
        tag = None
      )
    )
  }

}
