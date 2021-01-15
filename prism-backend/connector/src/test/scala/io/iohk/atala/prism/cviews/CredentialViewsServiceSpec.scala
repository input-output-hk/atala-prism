package io.iohk.atala.prism.cviews

import io.grpc.ServerServiceDefinition
import io.iohk.atala.prism.{DIDGenerator, RpcSpecBase}
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.connector.ConnectorAuthenticator
import io.iohk.atala.prism.console.DataPreparation
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.protos.connector_api.GetCurrentUserRequest
import io.iohk.atala.prism.view.HtmlViewImage.imageBase64
import io.iohk.atala.prism.protos.cviews_api.{CredentialViewsServiceGrpc, GetCredentialViewTemplatesRequest}
import org.mockito.MockitoSugar._

class CredentialViewsServiceSpec extends RpcSpecBase with DIDGenerator {
  private val usingApiAs = usingApiAsConstructor(
    new CredentialViewsServiceGrpc.CredentialViewsServiceBlockingStub(_, _)
  )

  private lazy val participantsRepository = new ParticipantsRepository(database)
  private lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)(executionContext)
  protected lazy val nodeMock = mock[io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService]
  private lazy val authenticator = new ConnectorAuthenticator(
    participantsRepository,
    requestNoncesRepository,
    nodeMock,
    GrpcAuthenticationHeaderParser
  )

  override def services: Seq[ServerServiceDefinition] =
    Seq(CredentialViewsServiceGrpc.bindService(new CredentialViewsService(authenticator), executionContext))

  "getCredentialViewTemplates" should {
    "return the predefined templates" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val _ = DataPreparation.createIssuer("Great Issuer", publicKey = Some(publicKey), did = Some(did))
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, GetCurrentUserRequest())

      usingApiAs(rpcRequest) { serviceStub =>
        val response = serviceStub.getCredentialViewTemplates(GetCredentialViewTemplatesRequest())

        val templates = response.templates
        templates.length mustBe 7
        val expectedTemplateNames = Array(
          "Government ID",
          "Educational Credential",
          "Proof of Employment",
          "Health Insurance",
          "Georgia National ID",
          "Georgia Educational Degree",
          "Georgia Educational Degree Transcript"
        )
        val expectedTemplateLogos = Array(
          "icon.svg",
          "university.svg",
          "employment.svg",
          "health.svg",
          "georgiaNationalIdIcon.svg",
          "georgiaEducationalDegreeIcon.svg",
          "georgiaEducationalDegreeTranscriptIcon.svg"
        )
        val expectedTemplateViews = Array(
          "id_credential.html",
          "educational_credential.html",
          "employment_credential.html",
          "health_credential.html",
          "georgia_national_id.html",
          "georgia_educational_degree.html",
          "georgia_educational_degree_transcript.html"
        )
        for (i <- templates.indices) {
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
