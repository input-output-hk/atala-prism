package io.iohk.cef.frontend.client

import java.util.Base64

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.util.ByteString
import io.iohk.cef.crypto._
import io.iohk.cef.frontend.models.IdentityTransactionRequest
import io.iohk.cef.ledger.identity.{Claim, IdentityTransaction, Link, Unlink}
import spray.json._

import scala.util.Try

trait DefaultJsonFormats extends DefaultJsonProtocol with SprayJsonSupport {

  implicit object IdentityTransactionJsonFormat extends JsonFormat[IdentityTransaction] {

    private val ClaimType = "claim"
    private val LinkType = "link"
    private val UnlinkType = "unlink"

    private type IdentityTransactionConstructor = (String, SigningPublicKey, Signature) => IdentityTransaction

    private def from(tpe: String): Option[IdentityTransactionConstructor] = tpe match {
      case ClaimType => Some(Claim.apply)
      case LinkType => Some(Link.apply)
      case UnlinkType => Some(Unlink.apply)
      case _ => None
    }

    override def read(json: JsValue): IdentityTransaction = {
      val decoder = Base64.getDecoder
      val fields = json.asJsObject().fields

      (fields("type"), fields("identity"), fields("key"), fields("signature")) match {
        case (JsString(typeString), JsString(identity), JsString(keyString), JsString(signatureString)) =>
          val constructor = from(typeString.toString)
            .getOrElse(deserializationError("Unknown type"))

          val keyBytes = Try(decoder.decode(keyString))
            .getOrElse(deserializationError("The given key is not base64 encoded"))

          val key = SigningPublicKey
            .decodeFrom(ByteString(keyBytes))
            .getOrElse(deserializationError("Invalid key"))

          val signatureBytes = Try(decoder.decode(signatureString))
            .getOrElse(deserializationError("The given signature is not base64 encoded"))

          val signature = Signature
            .decodeFrom(ByteString(signatureBytes))
            .getOrElse(deserializationError("Invalid signature"))

          constructor.apply(identity, key, signature)

        case _ => deserializationError("Invalid body")
      }
    }

    override def write(obj: IdentityTransaction): JsValue = {
      val tpe = obj match {
        case _: Claim => ClaimType
        case _: Link => LinkType
        case _: Unlink => UnlinkType
      }

      val encoder = Base64.getEncoder
      val key = encoder.encodeToString(obj.key.toByteString.toArray)
      val signature = encoder.encodeToString(obj.signature.toByteString.toArray)

      JsObject(
        "type" -> JsString(tpe),
        "identity" -> JsString(obj.identity),
        "key" -> JsString(key),
        "signature" -> JsString(signature)
      )
    }
  }

  implicit val identityRequestJsonFormat: RootJsonFormat[IdentityTransactionRequest] = jsonFormat2(
    IdentityTransactionRequest)

}
