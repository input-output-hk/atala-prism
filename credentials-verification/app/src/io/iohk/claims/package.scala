package io.iohk

import java.net.{URI, URISyntaxException}
import java.time.LocalDateTime

import play.api.libs.json._

package object claims {

  case class SubjectClaims(id: URI, properties: Map[String, String])
  case class CertificateProof(`type`: String, created: LocalDateTime, verificationMethod: URI)
  case class Certificate(
      issuer: URI,
      issuanceDate: LocalDateTime,
      credentialSubject: SubjectClaims,
      proof: Option[CertificateProof]
  )

  trait ClaimsJsonFormats {
    implicit val uriFormat = Format[URI](
      Reads {
        case JsString(value) =>
          try {
            JsSuccess(URI.create(value))
          } catch {
            case ex: URISyntaxException => JsError(s"Invalid DID: ${ex.getMessage}")
          }
        case _ => JsError("URI must be represented as string")
      },
      Writes(uri => JsString(uri.toString))
    )

    implicit val claimFormat = Format[SubjectClaims](
      Reads {
        case obj: JsObject =>
          for {
            id <- (obj \ "id").validate[URI]
            properties = (obj.value - "id").collect { case (key: String, JsString(value)) => (key, value) }.toMap
          } yield SubjectClaims(id, properties)
        case _ =>
          JsError("Claims must be represented as JSON object")
      },
      Writes { subjectClaims =>
        val fields = subjectClaims.properties + ("id" -> subjectClaims.id.toString)

        JsObject(fields.map(kv => kv._1 -> JsString(kv._2)).toSeq)
      }
    )
    implicit val certificateProofFormat = Json.format[CertificateProof]
    implicit val certificateFormat = Json.format[Certificate]
  }

  object json extends ClaimsJsonFormats

}
