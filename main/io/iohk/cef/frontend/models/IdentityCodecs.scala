package io.iohk.cef.frontend.models

import akka.util.ByteString
import io.iohk.cef.ledger.identity._
import io.iohk.cef.utils.HexStringCodec
import io.iohk.crypto._
import play.api.libs.json._

trait IdentityCodecs {

  implicit val transactionTypeReads: Format[IdentityTransactionType] = new Format[IdentityTransactionType] {
    override def reads(json: JsValue): JsResult[IdentityTransactionType] = {
      json.validate[String].flatMap { string =>
        IdentityTransactionType
          .withNameInsensitiveOption(string)
          .map(JsSuccess.apply(_))
          .getOrElse {
            JsError.apply("Invalid transaction type")
          }
      }
    }

    override def writes(o: IdentityTransactionType): JsValue = JsString(o.entryName)
  }

  implicit val claimDataFormats: Format[ClaimData] = Json.format[ClaimData]
  implicit val linkDataFormats: Format[LinkData] = Json.format[LinkData]
  implicit val unlinkDataFormats: Format[UnlinkData] = Json.format[UnlinkData]
  implicit val endorseDataFormats: Format[EndorseData] = Json.format[EndorseData]
  implicit val grantDataFormats: Format[GrantData] = Json.format[GrantData]
  implicit val revokeEndorsementDataFormats: Format[RevokeEndorsementData] = Json.format[RevokeEndorsementData]
  implicit val linkCertificateDataFormat: Format[LinkCertificateData] = new Format[LinkCertificateData] {
    override def writes(o: LinkCertificateData): JsValue = {
      Json.obj(
        "linkingIdentity" -> o.linkingIdentity,
        "pem" -> ByteString(o.pem).utf8String
      )
    }

    override def reads(json: JsValue): JsResult[LinkCertificateData] = {
      for {
        linkingIdentity <- json.\("linkingIdentity").validate[String]
        pem <- json.\("pem").validate[String]
      } yield {
        LinkCertificateData(linkingIdentity, HexStringCodec.fromHexString(pem).utf8String)
      }
    }
  }

  private val identityTransactionDataFormat: OFormat[IdentityTransactionData] = new OFormat[IdentityTransactionData] {
    override def writes(o: IdentityTransactionData): JsObject = {
      val (tpe, jsonData) = o match {
        case data: ClaimData => IdentityTransactionType.Claim -> Json.toJson(data)
        case data: LinkData => IdentityTransactionType.Link -> Json.toJson(data)
        case data: UnlinkData => IdentityTransactionType.Unlink -> Json.toJson(data)
        case data: EndorseData => IdentityTransactionType.Endorse -> Json.toJson(data)
        case data: GrantData => IdentityTransactionType.Grant -> Json.toJson(data)
        case data: RevokeEndorsementData => IdentityTransactionType.Revoke -> Json.toJson(data)
        case data: LinkCertificateData => IdentityTransactionType.LinkCertificate -> Json.toJson(data)
      }

      Json.obj(
        "type" -> JsString(tpe.toString),
        "data" -> jsonData
      )
    }

    override def reads(json: JsValue): JsResult[IdentityTransactionData] = {
      val data = json \ "data"
      (json \ "type")
        .validate[IdentityTransactionType]
        .flatMap {
          case IdentityTransactionType.Claim => data.validate[ClaimData]
          case IdentityTransactionType.Link => data.validate[LinkData]
          case IdentityTransactionType.Unlink => data.validate[UnlinkData]
          case IdentityTransactionType.Endorse => data.validate[EndorseData]
          case IdentityTransactionType.Grant => data.validate[GrantData]
          case IdentityTransactionType.Revoke => data.validate[RevokeEndorsementData]
          case IdentityTransactionType.LinkCertificate => data.validate[LinkCertificateData]
        }
    }
  }

  implicit val createIdentityTransactionRequestFormat: OFormat[CreateIdentityTransactionRequest] =
    new OFormat[CreateIdentityTransactionRequest] {
      override def writes(o: CreateIdentityTransactionRequest): JsObject = {
        val data = identityTransactionDataFormat.writes(o.data)
        val json = Json.obj(
          "privateKey" -> Json.toJson(o.privateKey),
          "linkingIdentityPrivateKey" -> Json.toJson(o.linkingIdentityPrivateKey)
        )

        json ++ data
      }

      override def reads(json: JsValue): JsResult[CreateIdentityTransactionRequest] = {
        for {
          privateKey <- (__ \ "privateKey").read[SigningPrivateKey].reads(json)
          linkingIdentityPrivateKey <- (__ \ "linkingIdentityPrivateKey").readNullable[SigningPrivateKey].reads(json)
          data <- identityTransactionDataFormat.reads(json)
        } yield CreateIdentityTransactionRequest(data, privateKey, linkingIdentityPrivateKey)
      }
    }

  implicit val identityTransactionWrites: OWrites[IdentityTransaction] = OWrites[IdentityTransaction] { obj =>
    val signature = Json.obj("signature" -> Json.toJson(obj.signature))
    val extraSignatures = obj match {
      case l: Link =>
        Json.obj("linkingIdentitySignature" -> Json.toJson(l.linkingIdentitySignature))

      case l: LinkCertificate =>
        Json.obj("signatureFromCertificate" -> Json.toJson(l.signatureFromCertificate))

      case _ => JsObject.empty
    }

    identityTransactionDataFormat.writes(obj.data) ++ signature ++ extraSignatures
  }
}
