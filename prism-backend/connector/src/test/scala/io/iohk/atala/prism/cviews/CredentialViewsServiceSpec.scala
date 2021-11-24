package io.iohk.atala.prism.cviews

import cats.effect.unsafe.implicits.global
import io.grpc.ServerServiceDefinition
import io.iohk.atala.prism.auth.{AuthenticatorF, SignedRpcRequest}
import io.iohk.atala.prism.connector.{ConnectorAuthenticator, DataPreparation}
import io.iohk.atala.prism.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.protos.connector_api.GetCurrentUserRequest
import io.iohk.atala.prism.protos.cviews_api.{CredentialViewsServiceGrpc, GetCredentialViewTemplatesRequest}
import io.iohk.atala.prism.utils.IOUtils._
import io.iohk.atala.prism.view.HtmlViewImage.imageBase64
import io.iohk.atala.prism.{DIDUtil, RpcSpecBase}
import org.mockito.MockitoSugar._

import java.io.{File, PrintWriter}

class CredentialViewsServiceSpec extends RpcSpecBase with DIDUtil {
  private val usingApiAs = usingApiAsConstructor(
    new CredentialViewsServiceGrpc.CredentialViewsServiceBlockingStub(_, _)
  )

  private lazy val participantsRepository =
    ParticipantsRepository.unsafe(dbLiftedToTraceIdIO, testLogs)
  private lazy val requestNoncesRepository =
    RequestNoncesRepository.unsafe(dbLiftedToTraceIdIO, testLogs)
  protected lazy val nodeMock =
    mock[io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService]
  private lazy val authenticator = AuthenticatorF.unsafe(
    nodeMock,
    new ConnectorAuthenticator(
      participantsRepository,
      requestNoncesRepository
    ),
    testLogs
  )

  private val authenticatorLogger = org.slf4j.LoggerFactory
    .getLogger(authenticator.getClass)
    .asInstanceOf[ch.qos.logback.classic.Logger]

  override def services: Seq[ServerServiceDefinition] =
    Seq(
      CredentialViewsServiceGrpc.bindService(
        new CredentialViewsService(authenticator),
        executionContext
      )
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    // Hide INFO logging because it's hard to debug tests because of the massive template responses
    authenticatorLogger.setLevel(ch.qos.logback.classic.Level.WARN)
  }

  override def afterEach(): Unit = {
    authenticatorLogger.setLevel(ch.qos.logback.classic.Level.INFO)
    super.afterEach()
  }

  "getCredentialViewTemplates" should {
    val SVG_MIME_TYPE = "image/svg+xml"
    val PNG_MIME_TYPE = "image/png"

    "return the predefined templates" in {
      val (keyPair, did) = createDid
      val _ = DataPreparation.createIssuer(
        "Great Issuer",
        publicKey = Some(keyPair.getPublicKey),
        did = Some(did)
      )
      val rpcRequest =
        SignedRpcRequest.generate(keyPair, did, GetCurrentUserRequest())

      usingApiAs(rpcRequest) { serviceStub =>
        val response = serviceStub.getCredentialViewTemplates(
          GetCredentialViewTemplatesRequest()
        )

        val templates = response.templates
        templates.length mustBe 10
        val expectedTemplateNames = Array(
          "Government ID",
          "Educational Credential",
          "Proof of Employment",
          "Health Insurance",
          "Georgia National ID",
          "Georgia Educational Degree",
          "Georgia Educational Degree Transcript",
          "Ethiopia National ID",
          "Ethiopia Educational Degree",
          "Ethiopia Educational Degree Transcript"
        )
        val expectedTemplateLogos = Array(
          "icon.svg",
          "university.svg",
          "employment.svg",
          "health.svg",
          "georgiaNationalIdIcon.svg",
          "georgiaEducationalDegreeIcon.svg",
          "georgiaEducationalDegreeTranscriptIcon.svg",
          "ethiopiaFlag.png",
          "ethiopiaEdu.png",
          "ethiopiaEduTrans.png"
        )
        val expectedTemplateLogoMimeTypes = Array(
          SVG_MIME_TYPE,
          SVG_MIME_TYPE,
          SVG_MIME_TYPE,
          SVG_MIME_TYPE,
          SVG_MIME_TYPE,
          SVG_MIME_TYPE,
          SVG_MIME_TYPE,
          PNG_MIME_TYPE,
          PNG_MIME_TYPE,
          PNG_MIME_TYPE
        )
        val expectedTemplateViews = Array(
          "id_credential.html",
          "educational_credential.html",
          "employment_credential.html",
          "health_credential.html",
          "georgia_national_id.html",
          "georgia_educational_degree.html",
          "georgia_educational_degree_transcript.html",
          "ethiopia_national_id.html",
          "ethiopia_educational_degree.html",
          "ethiopia_educational_degree_transcript.html"
        )
        for (i <- templates.indices) {
          val template = templates(i)
          template.id mustBe i + 1
          template.name mustBe expectedTemplateNames(i)
          template.encodedLogoImage mustBe imageBase64(expectedTemplateLogos(i))
          template.logoImageMimeType mustBe expectedTemplateLogoMimeTypes(i)

          val expectedHtmlTemplateName = expectedTemplateViews(i)
          val expectedHtmlTemplate =
            readResource(s"templates/${expectedTemplateViews(i)}")
          if (template.htmlTemplate != expectedHtmlTemplate) {
            val actualTemplateFile =
              saveTemplate(expectedHtmlTemplateName, template.htmlTemplate)
            fail(
              s"HTML of template ${template.name} is different from the expected one at $expectedHtmlTemplateName. A temporary file with the actual contents was stored at ${actualTemplateFile.getAbsolutePath}."
            )
          }
        }
      }
    }
  }

  private def saveTemplate(
      templateName: String,
      templateContent: String
  ): File = {
    val tempFile = File.createTempFile("test-output-", s"-$templateName")
    new PrintWriter(tempFile) {
      try {
        write(templateContent)
      } finally {
        close()
      }
    }
    tempFile
  }

  private def readResource(resource: String): String = {
    try {
      scala.io.Source.fromResource(s"cviews/$resource").mkString
    } catch {
      case _: Throwable =>
        throw new RuntimeException(s"Resource $resource not found")
    }
  }
}
