package io.iohk.atala.prism.auth

import cats.MonadThrow
import cats.syntax.applicativeError._
import io.iohk.atala.prism.identity.PrismDid
import scalapb.GeneratedMessage
import tofu.higherKind.Mid
import tofu.logging.{Loggable, ServiceLogging}
import tofu.syntax.logging._
import tofu.syntax.monadic._

private[auth] final class AuthenticatorFLogs[Id: Loggable, F[_]: ServiceLogging[
  *[_],
  AuthenticatorF[Id, F]
]: MonadThrow]
    extends AuthenticatorF[Id, Mid[F, *]] {
  def whitelistedDid[Request <: GeneratedMessage, Response](
      whitelist: Set[PrismDid],
      methodName: String,
      request: Request
  ): Mid[F, Either[errors.AuthError, PrismDid]] = fa =>
    info"starting $methodName request = ${request.toProtoString}" *> fa
      .flatTap(
        _.fold(
          err => error"$methodName FAILED request = ${request.toProtoString} - failed cause: $err",
          {
            case res: GeneratedMessage => info"request = ${request.toProtoString}, response = ${res.toProtoString}"
            case res => info"request = ${request.toProtoString}, response = ${res.toString}"
          }
        )
      )
      .onError(errorCause"Encountered an error while $methodName" (_))

  def authenticated[Request <: GeneratedMessage, Response](
      methodName: String,
      request: Request
  ): Mid[F, Either[errors.AuthError, Id]] = fa =>
    info"starting $methodName request = ${request.toProtoString}" *> fa
      .flatTap(
        _.fold(
          err => error"$methodName FAILED request = ${request.toProtoString} - failed cause: $err",
          id => info"request = ${request.toProtoString}, id = $id"
        )
      )
      .onError(errorCause"Encountered an error while $methodName" (_))

  def public[Request <: GeneratedMessage, Response](methodName: String, request: Request): Mid[F, Unit] = fa =>
    info"starting $methodName request = ${request.toProtoString}" *> fa
      .flatTap(_ => info"$methodName - successfully done")
      .onError(
        errorCause"Encountered an error while $methodName" (_)
      )
}
