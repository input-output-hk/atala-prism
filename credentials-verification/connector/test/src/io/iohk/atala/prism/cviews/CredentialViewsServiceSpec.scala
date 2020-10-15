package io.iohk.atala.prism.cviews

import io.grpc.ServerServiceDefinition
import io.iohk.atala.prism.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.connector.{RpcSpecBase, SignedRequestsAuthenticator}
import io.iohk.atala.prism.cmanager.repositories.common.DataPreparation
import io.iohk.atala.prism.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.view.HtmlViewImage.imageBase64
import io.iohk.atala.prism.protos.cviews_api.{CredentialViewsServiceGrpc, GetCredentialViewTemplatesRequest}
import org.mockito.MockitoSugar._

class CredentialViewsServiceSpec extends RpcSpecBase {
  private implicit val executionContext = scala.concurrent.ExecutionContext.global
  private val usingApiAs = usingApiAsConstructor(
    new CredentialViewsServiceGrpc.CredentialViewsServiceBlockingStub(_, _)
  )

  private lazy val participantsRepository = new ParticipantsRepository(database)
  private lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)(executionContext)
  private lazy val nodeMock = mock[io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService]
  private lazy val authenticator = new SignedRequestsAuthenticator(
    participantsRepository,
    requestNoncesRepository,
    nodeMock,
    GrpcAuthenticationHeaderParser
  )

  override def services: Seq[ServerServiceDefinition] =
    Seq(CredentialViewsServiceGrpc.bindService(new CredentialViewsService(authenticator), executionContext))

  "getCredentialViewTemplates" should {
    "return the predefined templates" in {
      val issuerId = DataPreparation.createIssuer("Great Issuer")

      usingApiAs(ParticipantId(issuerId.value)) { serviceStub =>
        val response = serviceStub.getCredentialViewTemplates(GetCredentialViewTemplatesRequest())

        val templates = response.templates
        templates.length mustBe 4
        val expectedTemplateNames =
          Array("Government ID", "Educational Credential", "Proof of Employment", "Health Insurance")
        val expectedTemplateLogos = Array("icon.svg", "university.svg", "employment.svg", "health.svg")
        val expectedTemplateViews =
          Array(
            "id_credential.html",
            "educational_credential.html",
            "employment_credential.html",
            "health_credential.html"
          )
        for (i <- 0 until 4) {
          val template = templates(i)
          template.id mustBe i + 1
          template.name mustBe expectedTemplateNames(i)
          template.encodedLogoImage mustBe imageBase64(expectedTemplateLogos(i))
          template.logoImageMimeType mustBe "image/svg+xml"
          template.htmlTemplate mustBe readResource(s"templates/${expectedTemplateViews(i)}")
        }
      }
    }
  }

  private def readResource(resource: String): String = {
    try {
      scala.io.Source.fromResource(s"cviews/$resource").mkString
    } catch {
      case _: Throwable => throw new RuntimeException(s"Resource $resource not found")
    }
  }
}
