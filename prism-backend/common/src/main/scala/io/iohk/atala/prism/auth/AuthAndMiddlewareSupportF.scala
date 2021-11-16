package io.iohk.atala.prism.auth

import cats.effect.unsafe.IORuntime
import io.iohk.atala.prism.errors.{ErrorSupport, LoggingContext, PrismError}
import io.iohk.atala.prism.grpc.ProtoConverter
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.metrics.RequestMeasureUtil.measureRequestFuture
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.{FutureEitherFOps, FutureEitherOps}
import scalapb.GeneratedMessage
import shapeless.Coproduct
import shapeless.ops.coproduct.Unifier

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait AuthAndMiddlewareSupportF[Err <: PrismError, Id] {
  self: ErrorSupport[Err] =>

  import AuthAndMiddlewareSupport._

  protected val authenticator: AuthenticatorF[Id, IOWithTraceIdContext]
  protected val serviceName: String
  protected val IOruntime: IORuntime

  final class AuthPartiallyApplied[Query <: Product] {
    def apply[Proto <: GeneratedMessage, Result](
        methodName: String,
        request: Proto,
        traceId: TraceId
    )(f: (Id, Query) => FutureEither[Err, Result])(implicit
        ec: ExecutionContext,
        protoConverter: ProtoConverter[Proto, Query]
    ): Future[Result] = {
      for {
        participantId <- authenticator
          .authenticated(methodName, request)
          .run(traceId)
          .unsafeToFuture()(IOruntime)
          .toFutureEither
        res <- convertFromRequest[Proto, Result, Query](request, methodName).flatMap { query =>
          implicit val lc: LoggingContext = LoggingContext(
            (0 until query.productArity)
              .map(i =>
                query
                  .productElementName(i) -> query.productElement(i).toString
              )
              .toMap + ("participantId" -> participantId.toString)
          )
          measureRequestFuture(serviceName, methodName)(
            f(participantId, query)
              .wrapAndRegisterExceptions(serviceName, methodName)
              .value
          )
        }.toFutureEither
      } yield res
    }.flatten
  }

  final class AuthCoproductPartiallyApplied[Query <: Product] {
    def apply[C <: Coproduct, Proto <: GeneratedMessage, Result](
        methodName: String,
        request: Proto,
        traceId: TraceId
    )(f: (Id, Query) => FutureEither[C, Result])(implicit
        ec: ExecutionContext,
        protoConverter: ProtoConverter[Proto, Query],
        unifier: Unifier.Aux[C, Err]
    ): Future[Result] = {
      for {
        participantId <- authenticator
          .authenticated(methodName, request)
          .run(traceId)
          .unsafeToFuture()(IOruntime)
          .toFutureEither
        result <- convertFromRequest[Proto, Result, Query](request, methodName).flatMap { query =>
          implicit val lc: LoggingContext = LoggingContext(
            (0 until query.productArity)
              .map(i =>
                query
                  .productElementName(i) -> query.productElement(i).toString
              )
              .toMap + ("participantId" -> participantId.toString)
          )
          measureRequestFuture(serviceName, methodName)(
            f(participantId, query)
              .mapLeft(_.unify)
              .wrapAndRegisterExceptions(serviceName, methodName)
              .value
          )
        }.toFutureEither
      } yield result
    }.flatten
  }

  final class WithoutAuthPartiallyApplied[Query <: Product] {
    def apply[Proto <: GeneratedMessage, Result](
        methodName: String,
        request: Proto,
        traceId: TraceId
    )(f: Query => FutureEither[Err, Result])(implicit
        ec: ExecutionContext,
        protoConverter: ProtoConverter[Proto, Query]
    ): Future[Result] = {
      for {
        _ <- authenticator.public(methodName, request).run(traceId).unsafeToFuture()(IOruntime).lift[Err]
        result <- convertFromRequest[Proto, Result, Query](request, methodName).flatMap { query =>
          // Assemble LoggingContext out of the case class fields
          implicit val lc: LoggingContext = LoggingContext(
            (0 until query.productArity)
              .map(i =>
                query
                  .productElementName(i) -> query.productElement(i).toString
              )
              .toMap
          )
          measureRequestFuture(serviceName, methodName)(
            f(query)
              .wrapAndRegisterExceptions(serviceName, methodName)
              .value
          )
        }.toFutureEither
      } yield result
    }.flatten
  }

  final class WithoutAuthCoproductPartiallyApplied[Query <: Product] {
    def apply[C <: Coproduct, Proto <: GeneratedMessage, Result](
        methodName: String,
        request: Proto,
        traceId: TraceId
    )(f: Query => FutureEither[C, Result])(implicit
        ec: ExecutionContext,
        protoConverter: ProtoConverter[Proto, Query],
        unifier: Unifier.Aux[C, Err]
    ): Future[Result] = {
      for {
        _ <- authenticator.public(methodName, request).run(traceId).unsafeToFuture()(IOruntime).lift[Err]
        result <- convertFromRequest[Proto, Result, Query](request, methodName).flatMap { query =>
          // Assemble LoggingContext out of the case class fields
          implicit val lc: LoggingContext = LoggingContext(
            (0 until query.productArity)
              .map(i =>
                query
                  .productElementName(i) -> query.productElement(i).toString
              )
              .toMap
          )
          measureRequestFuture(serviceName, methodName)(
            f(query)
              .mapLeft(_.unify)
              .wrapAndRegisterExceptions(serviceName, methodName)
              .value
          )
        }.toFutureEither
      } yield result
    }.flatten
  }

  // Just converts query from proto in our representation, on failure, responds with a thrown error
  private def convertFromRequest[
      Proto <: GeneratedMessage,
      Result,
      Query <: Product
  ](
      request: Proto,
      methodName: String
  )(implicit
      ec: ExecutionContext,
      protoConverter: ProtoConverter[Proto, Query]
  ): Future[Query] = {
    protoConverter.fromProto(request) match {
      case Failure(exception) =>
        val response = invalidRequest(exception.getMessage)
        respondWith(request, response, serviceName, methodName)
      case Success(query) =>
        Future.successful(query)
    }
  }

  // Query needs to be a Product (which all case class instances are) for easy access to its fields.
  // Partial application helps with Scala type inference. You can specify just a single type
  // parameter Query and all other type parameters can usually be inferred by Scala.
  def auth[Query <: Product]: AuthPartiallyApplied[Query] =
    new AuthPartiallyApplied[Query]()

  def authCo[Query <: Product]: AuthCoproductPartiallyApplied[Query] =
    new AuthCoproductPartiallyApplied[Query]()

  // Query needs to be a Product (which all case class instances are) for easy access to its fields.
  // Partial application helps with Scala type inference. You can specify just a single type
  // parameter Query and all other type parameters can usually be inferred by Scala.
  def public[Query <: Product]: WithoutAuthPartiallyApplied[Query] =
    new WithoutAuthPartiallyApplied[Query]()

  def publicCo[Query <: Product]: WithoutAuthCoproductPartiallyApplied[Query] =
    new WithoutAuthCoproductPartiallyApplied[Query]()

  def unitAuth: AuthPartiallyApplied[EmptyQuery.type] = auth[EmptyQuery.type]

  def unitPublic: WithoutAuthPartiallyApplied[EmptyQuery.type] =
    public[EmptyQuery.type]

}
