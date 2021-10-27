package io.iohk.atala.prism.cviews

import io.circe.Json
import io.iohk.atala.prism.connector.ConnectorAuthenticator
import io.iohk.atala.prism.intdemo.html._
import io.iohk.atala.prism.metrics.RequestMeasureUtil.measureRequestFuture
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.cviews_api.{GetCredentialViewTemplatesRequest, GetCredentialViewTemplatesResponse}
import io.iohk.atala.prism.protos.{cviews_api, cviews_models}
import io.iohk.atala.prism.utils.syntax._
import io.iohk.atala.prism.view.HtmlViewImage
import io.iohk.atala.prism.intdemo.IdServiceImpl.IdCredentialHtmlTemplateData
import io.iohk.atala.prism.intdemo.DegreeServiceImpl.DegreeCredentialHtmlTemplateData
import io.iohk.atala.prism.intdemo.EmploymentServiceImpl.EmploymentCredentialHtmlTemplateData
import io.iohk.atala.prism.intdemo.InsuranceServiceImpl.InsuranceCredentialHtmlTemplateData

import scala.concurrent.{ExecutionContext, Future}

class CredentialViewsService(authenticator: ConnectorAuthenticator)(implicit
    ec: ExecutionContext
) extends cviews_api.CredentialViewsServiceGrpc.CredentialViewsService {

  private val getCredsViewTemplatesMethodName = "getCredentialViewTemplates"

  override def getCredentialViewTemplates(
      request: GetCredentialViewTemplatesRequest
  ): Future[GetCredentialViewTemplatesResponse] = {
    authenticatedHandler(getCredsViewTemplatesMethodName, request) { _ =>
      val response = GetCredentialViewTemplatesResponse(templates = PredefinedHtmlTemplates.all)
      response.tryF
    }
  }

  private def authenticatedHandler[
      Request <: scalapb.GeneratedMessage,
      Response <: scalapb.GeneratedMessage
  ](
      methodName: String,
      request: Request
  )(
      block: ParticipantId => Future[Response]
  ): Future[Response] = {
    authenticator.authenticated(methodName, request) { (participantId, _) =>
      measureRequestFuture(
        "credential-views-service-service",
        getCredsViewTemplatesMethodName
      )(
        block(participantId)
      )
    }
  }
}

private object PredefinedHtmlTemplates {
  val all: Vector[cviews_models.CredentialViewTemplate] = Vector(
    id(),
    educational(),
    employment(),
    insurance(),
    georgiaNationalId(),
    georgiaEducationDegree(),
    georgiaEducationDegreeTranscript(),
    ethiopiaNationalId(),
    ethiopiaEducationDegree(),
    ethiopiaEducationDegreeTranscript()
  )

  private def id(): cviews_models.CredentialViewTemplate = {
    val logoImage = "icon.svg"
    cviews_models.CredentialViewTemplate(
      id = 1,
      name = "Government ID",
      encodedLogoImage = HtmlViewImage.imageBase64(logoImage),
      logoImageMimeType = HtmlViewImage.imageMimeType(logoImage),
      htmlTemplate = IdCredential(credential =
        IdCredentialHtmlTemplateData(
          issuerName = "{{issuerName}}",
          identityNumber = "{{credentialSubject.identityNumber}}",
          dateOfBirth = "{{credentialSubject.dateOfBirth}}",
          fullName = "{{credentialSubject.name}}",
          expirationDate = "{{expiryDate}}"
        )
      ).body
    )
  }

  private def educational(): cviews_models.CredentialViewTemplate = {
    val logoImage = "university.svg"
    cviews_models.CredentialViewTemplate(
      id = 2,
      name = "Educational Credential",
      encodedLogoImage = HtmlViewImage.imageBase64(logoImage),
      logoImageMimeType = HtmlViewImage.imageMimeType(logoImage),
      htmlTemplate = UniversityDegree(credential =
        DegreeCredentialHtmlTemplateData(
          degreeAwarded = "{{credentialSubject.degreeAwarded}}",
          issuerName = "{{issuerName}}",
          degreeResult = "{{credentialSubject.degreeResult}}",
          fullName = "{{credentialSubject.name}}",
          startDate = "{{credentialSubject.startDate}}",
          graduationDate = "{{credentialSubject.graduationDate}}"
        )
      ).body
    )
  }

  private def employment(): cviews_models.CredentialViewTemplate = {
    val logoImage = "employment.svg"
    cviews_models.CredentialViewTemplate(
      id = 3,
      name = "Proof of Employment",
      encodedLogoImage = HtmlViewImage.imageBase64(logoImage),
      logoImageMimeType = HtmlViewImage.imageMimeType(logoImage),
      htmlTemplate = ProofOfEmployment(credential =
        EmploymentCredentialHtmlTemplateData(
          issuerName = "{{issuerName}}",
          issuerAddress = "{{issuerAddress}}",
          fullName = "{{credentialSubject.name}}",
          employmentStatus = "{{employmentStatus}}",
          employmentStartDate = "{{employmentStartDate}}"
        )
      ).body
    )
  }

  private def insurance(): cviews_models.CredentialViewTemplate = {
    val logoImage = "health.svg"
    cviews_models.CredentialViewTemplate(
      id = 4,
      name = "Health Insurance",
      encodedLogoImage = HtmlViewImage.imageBase64(logoImage),
      logoImageMimeType = HtmlViewImage.imageMimeType(logoImage),
      htmlTemplate = HealthCredential(credential =
        InsuranceCredentialHtmlTemplateData(
          issuerName = "{{issuerName}}",
          productClass = "{{productClass}}",
          policyNumber = "{{policyNumber}}",
          fullName = "{{credentialSubject.name}}",
          expirationDate = "{{expiryDate}}"
        )
      ).body
    )
  }

  private def georgiaNationalId(): cviews_models.CredentialViewTemplate = {
    val logoImage = "georgiaNationalIdIcon.svg"
    cviews_models.CredentialViewTemplate(
      id = 5,
      name = "Georgia National ID",
      encodedLogoImage = HtmlViewImage.imageBase64(logoImage),
      logoImageMimeType = HtmlViewImage.imageMimeType(logoImage),
      htmlTemplate = GeorgiaNationalId(credential =
        Json.obj(
          "issuer" -> Json.obj("name" -> asStringVar("issuer.name")),
          "credentialSubject" -> Json.obj(
            "name" -> asStringVar("credentialSubject.name"),
            "gender" -> asStringVar("credentialSubject.gender"),
            "country" -> asStringVar("credentialSubject.country"),
            "placeOfBirth" -> asStringVar("credentialSubject.placeOfBirth"),
            "dateOfBirth" -> asStringVar("credentialSubject.dateOfBirth")
          ),
          "issueDate" -> asStringVar("issueDate"),
          "cardNumber" -> asStringVar("cardNumber"),
          "expiryDate" -> asStringVar("expiryDate"),
          "personalNumber" -> asStringVar("personalNumber")
        )
      ).body
    )
  }

  private def georgiaEducationDegree(): cviews_models.CredentialViewTemplate = {
    val logoImage = "georgiaEducationalDegreeIcon.svg"
    cviews_models.CredentialViewTemplate(
      id = 6,
      name = "Georgia Educational Degree",
      encodedLogoImage = HtmlViewImage.imageBase64(logoImage),
      logoImageMimeType = HtmlViewImage.imageMimeType(logoImage),
      htmlTemplate = GeorgiaEducationalDegree(credential =
        Json.obj(
          "issuer" -> Json.obj("name" -> asStringVar("issuer.name")),
          "credentialSubject" -> Json.obj(
            "firstName" -> asStringVar("credentialSubject.firstName"),
            "lastName" -> asStringVar("credentialSubject.lastName")
          ),
          "degreeName" -> asStringVar("degreeName"),
          "degreeResult" -> asStringVar("degreeResult"),
          "issueDate" -> asStringVar("issueDate")
        )
      ).body
    )
  }

  private def georgiaEducationDegreeTranscript(): cviews_models.CredentialViewTemplate = {
    val logoImage = "georgiaEducationalDegreeTranscriptIcon.svg"
    cviews_models.CredentialViewTemplate(
      id = 7,
      name = "Georgia Educational Degree Transcript",
      encodedLogoImage = HtmlViewImage.imageBase64(logoImage),
      logoImageMimeType = HtmlViewImage.imageMimeType(logoImage),
      htmlTemplate = GeorgiaEducationalDegreeTranscript(credential =
        Json.obj(
          "issuer" -> Json.obj("name" -> asStringVar("issuer.name")),
          "credentialSubject" -> Json.obj(
            "name" -> asStringVar("credentialSubject.name")
          ),
          "degreeName" -> asStringVar("degreeName"),
          "issueDate" -> asStringVar("issueDate"),
          "cumulativeScore" -> asStringVar("cumulativeScore"),
          "coursesHtml" -> asStringVar("coursesHtml")
        )
      ).body
    )
  }

  private def ethiopiaNationalId(): cviews_models.CredentialViewTemplate = {
    val logoImage = "ethiopiaFlag.png"
    cviews_models.CredentialViewTemplate(
      id = 8,
      name = "Ethiopia National ID",
      encodedLogoImage = HtmlViewImage.imageBase64(logoImage),
      logoImageMimeType = HtmlViewImage.imageMimeType(logoImage),
      htmlTemplate = EthiopiaNationalId(credential =
        Json.obj(
          "issuer" -> Json.obj("name" -> asStringVar("issuer.name")),
          "credentialSubject" -> Json.obj(
            "name" -> asStringVar("credentialSubject.name"),
            "gender" -> asStringVar("credentialSubject.gender"),
            "country" -> asStringVar("credentialSubject.country"),
            "placeOfBirth" -> asStringVar("credentialSubject.placeOfBirth"),
            "dateOfBirth" -> asStringVar("credentialSubject.dateOfBirth")
          ),
          "issueDate" -> asStringVar("issueDate"),
          "cardNumber" -> asStringVar("cardNumber"),
          "expiryDate" -> asStringVar("expiryDate"),
          "personalNumber" -> asStringVar("personalNumber")
        )
      ).body
    )
  }

  private def ethiopiaEducationDegree(): cviews_models.CredentialViewTemplate = {
    val logoImage = "ethiopiaEdu.png"
    cviews_models.CredentialViewTemplate(
      id = 9,
      name = "Ethiopia Educational Degree",
      encodedLogoImage = HtmlViewImage.imageBase64(logoImage),
      logoImageMimeType = HtmlViewImage.imageMimeType(logoImage),
      htmlTemplate = EthiopiaEducationalDegree(credential =
        Json.obj(
          "issuer" -> Json.obj("name" -> asStringVar("issuer.name")),
          "credentialSubject" -> Json.obj(
            "firstName" -> asStringVar("credentialSubject.firstName"),
            "lastName" -> asStringVar("credentialSubject.lastName")
          ),
          "degreeName" -> asStringVar("degreeName"),
          "degreeResult" -> asStringVar("degreeResult"),
          "issueDate" -> asStringVar("issueDate")
        )
      ).body
    )
  }

  private def ethiopiaEducationDegreeTranscript(): cviews_models.CredentialViewTemplate = {
    val logoImage = "ethiopiaEduTrans.png"
    cviews_models.CredentialViewTemplate(
      id = 10,
      name = "Ethiopia Educational Degree Transcript",
      encodedLogoImage = HtmlViewImage.imageBase64(logoImage),
      logoImageMimeType = HtmlViewImage.imageMimeType(logoImage),
      htmlTemplate = EthiopiaEducationalDegreeTranscript(credential =
        Json.obj(
          "issuer" -> Json.obj("name" -> asStringVar("issuer.name")),
          "credentialSubject" -> Json.obj(
            "name" -> asStringVar("credentialSubject.name")
          ),
          "degreeName" -> asStringVar("degreeName"),
          "issueDate" -> asStringVar("issueDate"),
          "cumulativeScore" -> asStringVar("cumulativeScore"),
          "coursesHtml" -> asStringVar("coursesHtml")
        )
      ).body
    )
  }

  private def asStringVar(varName: String): Json = {
    Json.fromString(s"{{$varName}}")
  }
}
