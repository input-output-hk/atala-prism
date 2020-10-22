package io.iohk.atala.prism.cviews

import io.circe.Json
import io.iohk.atala.prism.connector.Authenticator
import io.iohk.atala.prism.console.models.Institution
import io.iohk.atala.prism.intdemo.html.{HealthCredential, IdCredential, ProofOfEmployment, UniversityDegree}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.utils.syntax._
import io.iohk.atala.prism.view.HtmlViewImage
import io.iohk.atala.prism.protos.cviews_api.{GetCredentialViewTemplatesRequest, GetCredentialViewTemplatesResponse}
import io.iohk.atala.prism.protos.{cviews_api, cviews_models}

import scala.concurrent.{ExecutionContext, Future}

class CredentialViewsService(authenticator: Authenticator)(implicit ec: ExecutionContext)
    extends cviews_api.CredentialViewsServiceGrpc.CredentialViewsService {
  override def getCredentialViewTemplates(
      request: GetCredentialViewTemplatesRequest
  ): Future[GetCredentialViewTemplatesResponse] = {
    authenticatedHandler("getCredentialViewTemplates", request) { _ =>
      val response = GetCredentialViewTemplatesResponse(templates = PredefinedHtmlTemplates.all)
      Right(response).tryF.toFutureEither
    }
  }

  private def authenticatedHandler[Request <: scalapb.GeneratedMessage, Response <: scalapb.GeneratedMessage](
      methodName: String,
      request: Request
  )(
      block: Institution.Id => FutureEither[Nothing, Response]
  ): Future[Response] = {
    authenticator.authenticated(methodName, request) { participantId =>
      block(Institution.Id(participantId.uuid)).value
        .map {
          case Right(x) => x
          case Left(e) => throw new RuntimeException(s"FAILED: $e")
        }
    }
  }
}

private object PredefinedHtmlTemplates {
  private val SVG_MIME_TYPE = "image/svg+xml"

  val all: Vector[cviews_models.CredentialViewTemplate] = Vector(
    idCredential(),
    educationalCredential(),
    employmentCredential(),
    insuranceCredential()
  )

  private def idCredential(): cviews_models.CredentialViewTemplate = {
    cviews_models.CredentialViewTemplate(
      id = 1,
      name = "Government ID",
      encodedLogoImage = HtmlViewImage.imageBase64("icon.svg"),
      logoImageMimeType = SVG_MIME_TYPE,
      htmlTemplate = IdCredential(credential =
        Json.obj(
          "issuer" -> Json.obj("name" -> asStringVar("issuer.name")),
          "credentialSubject" -> Json.obj(
            "identityNumber" -> asStringVar("credentialSubject.identityNumber"),
            "dateOfBirth" -> asStringVar("credentialSubject.dateOfBirth"),
            "name" -> asStringVar("credentialSubject.name")
          ),
          "expiryDate" -> asStringVar("expiryDate")
        )
      ).body
    )
  }

  private def educationalCredential(): cviews_models.CredentialViewTemplate = {
    cviews_models.CredentialViewTemplate(
      id = 2,
      name = "Educational Credential",
      encodedLogoImage = HtmlViewImage.imageBase64("university.svg"),
      logoImageMimeType = SVG_MIME_TYPE,
      htmlTemplate = UniversityDegree(credential =
        Json.obj(
          "issuer" -> Json.obj("name" -> asStringVar("issuer.name")),
          "credentialSubject" -> Json.obj(
            "degreeAwarded" -> asStringVar("credentialSubject.degreeAwarded"),
            "degreeResult" -> asStringVar("credentialSubject.degreeResult"),
            "name" -> asStringVar("credentialSubject.name"),
            "startDate" -> asStringVar("credentialSubject.startDate")
          ),
          "issuanceDate" -> asStringVar("issuanceDate")
        )
      ).body
    )
  }

  private def employmentCredential(): cviews_models.CredentialViewTemplate = {
    cviews_models.CredentialViewTemplate(
      id = 3,
      name = "Proof of Employment",
      encodedLogoImage = HtmlViewImage.imageBase64("employment.svg"),
      logoImageMimeType = SVG_MIME_TYPE,
      htmlTemplate = ProofOfEmployment(credential =
        Json.obj(
          "issuer" -> Json.obj(
            "name" -> asStringVar("issuer.name"),
            "address" -> asStringVar("issuer.address")
          ),
          "credentialSubject" -> Json.obj(
            "name" -> asStringVar("credentialSubject.name")
          ),
          "employmentStatus" -> asStringVar("employmentStatus"),
          "employmentStartDate" -> asStringVar("employmentStartDate")
        )
      ).body
    )
  }

  private def insuranceCredential(): cviews_models.CredentialViewTemplate = {
    cviews_models.CredentialViewTemplate(
      id = 4,
      name = "Health Insurance",
      encodedLogoImage = HtmlViewImage.imageBase64("health.svg"),
      logoImageMimeType = SVG_MIME_TYPE,
      htmlTemplate = HealthCredential(credential =
        Json.obj(
          "issuer" -> Json.obj("name" -> asStringVar("issuer.name")),
          "credentialSubject" -> Json.obj(
            "name" -> asStringVar("credentialSubject.name")
          ),
          "productClass" -> asStringVar("productClass"),
          "policyNumber" -> asStringVar("policyNumber"),
          "expiryDate" -> asStringVar("expiryDate")
        )
      ).body
    )
  }

  private def asStringVar(varName: String): Json = {
    Json.fromString(s"{{$varName}}")
  }
}
