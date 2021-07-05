package io.iohk.atala.prism.kycbridge.services

import java.nio.charset.StandardCharsets
import java.util.Base64

import io.circe.Decoder
import io.iohk.atala.prism.kycbridge.config.AcuantConfig
import monix.eval.Task
import org.http4s.client.Client
import org.http4s.{AuthScheme, Credentials, Request, Status}
import org.http4s.headers._
import org.http4s.circe.CirceEntityDecoder._

object ServiceUtils {

  def basicAuthorization(acuantConfig: AcuantConfig): Authorization = {
    val credentials = Base64.getEncoder
      .encodeToString(s"${acuantConfig.username}:${acuantConfig.password}".getBytes(StandardCharsets.UTF_8))

    Authorization(Credentials.Token(AuthScheme.Basic, credentials))
  }

  def basicAuthorization(username: String, password: String): Authorization = {
    val credentials = Base64.getEncoder
      .encodeToString(s"$username:$password".getBytes(StandardCharsets.UTF_8))

    Authorization(Credentials.Token(AuthScheme.Basic, credentials))
  }

  def runRequestToEither[A: Decoder](
      request: Task[Request[Task]],
      client: Client[Task]
  ): Task[Either[Exception, A]] = {
    request
      .flatMap(client.run(_).use {
        case Status.Successful(r) => r.attemptAs[A].value
        case r =>
          r.as[String]
            .map(b =>
              Left(new Exception(s"Request failed with status ${r.status.code} ${r.status.reason} and body $b"))
            )
      })
      .onErrorHandle(throwable => Left(new Exception(throwable.getMessage)))
  }

  def fetchBinaryData(
      request: Task[Request[Task]],
      client: Client[Task]
  ): Task[Either[Exception, Array[Byte]]] = {
    request
      .flatMap(client.run(_).use {
        case Status.Successful(r) => r.body.compile.toList.map(_.toArray).map(Right(_))
        case r =>
          r.as[String]
            .map(b =>
              Left(new Exception(s"Request failed with status ${r.status.code} ${r.status.reason} and body $b"))
            )
      })
      .onErrorHandle(throwable => Left(new Exception(throwable.getMessage)))
  }

}
