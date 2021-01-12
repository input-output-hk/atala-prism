package io.iohk.atala.prism.kycbridge.services

import io.iohk.atala.prism.kycbridge.config.AcuantConfig
import io.iohk.atala.prism.kycbridge.models.faceId.{Data, FaceMatchRequest, FaceMatchResponse, Settings}
import io.iohk.atala.prism.kycbridge.services.ServiceUtils.basicAuthorization
import monix.eval.Task
import org.http4s.Method.POST
import org.http4s.{MediaType, Uri}
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.Accept
import io.circe.syntax._
import org.http4s.circe._
import ServiceUtils._
import io.iohk.atala.prism.kycbridge.models.faceId.implicits._

trait FaceIdService {
  def faceMatch(data: Data): Task[Either[Exception, FaceMatchResponse]]
}

class FaceIdServiceImpl(acuantConfig: AcuantConfig, client: Client[Task])
    extends FaceIdService
    with Http4sClientDsl[Task] {

  private val baseUri = Uri.unsafeFromString(acuantConfig.faceIdUrl)

  private lazy val authorization = basicAuthorization(acuantConfig)

  override def faceMatch(data: Data): Task[Either[Exception, FaceMatchResponse]] = {

    val faceMatchRequest = FaceMatchRequest(
      data = data,
      settings = Settings(
        subscriptionId = acuantConfig.subscriptionId
      )
    )

    val request = POST(
      faceMatchRequest.asJson,
      baseUri / "api/v1/FaceMatch",
      authorization,
      Accept(MediaType.application.json)
    )

    runRequestToEither[FaceMatchResponse](request, client)
  }
}
