package io.iohk.atala.prism.auth

import io.iohk.atala.prism.errors.{ErrorSupport, LoggingContext, PrismError}
import io.iohk.atala.prism.grpc.ProtoConverter
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.metrics.RequestMeasureUtil.measureRequestFuture
import io.iohk.atala.prism.utils.FutureEither
import scalapb.GeneratedMessage
import shapeless.Coproduct
import shapeless.ops.coproduct.Unifier

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait AuthAndMiddlewareSupport[Err <: PrismError, Id] {
  self: ErrorSupport[Err] =>
  import AuthAndMiddlewareSupport._

  protected val authenticator: Authenticator[Id]
  protected val serviceName: String

  final class AuthPartiallyApplied[Query <: Product] {
    def apply[Proto <: GeneratedMessage, Result](
        methodName: String,
        request: Proto
    )(f: (Id, TraceId, Query) => FutureEither[Err, Result])(implicit
        ec: ExecutionContext,
        protoConverter: ProtoConverter[Proto, Query]
    ): Future[Result] = {
      authenticator
        .authenticated(methodName, request) { (participantId, traceId) =>
          convertFromRequest[Proto, Result, Query](request, methodName)
            .flatMap { query =>
              implicit val lc: LoggingContext = LoggingContext(
                (0 until query.productArity)
                  .map(i =>
                    query
                      .productElementName(i) -> query.productElement(i).toString
                  )
                  .toMap + ("participantId" -> participantId.toString)
              )
              measureRequestFuture(serviceName, methodName)(
                f(participantId, traceId, query)
                  .wrapAndRegisterExceptions(serviceName, methodName)
                  .flatten
              )
            }
        }
    }
  }

  final class AuthCoproductPartiallyApplied[Query <: Product] {
    def apply[C <: Coproduct, Proto <: GeneratedMessage, Result](
        methodName: String,
        request: Proto
    )(f: (Id, TraceId, Query) => FutureEither[C, Result])(implicit
        ec: ExecutionContext,
        protoConverter: ProtoConverter[Proto, Query],
        unifier: Unifier.Aux[C, Err]
    ): Future[Result] = {
      authenticator
        .authenticated(methodName, request) { (participantId, traceId) =>
          convertFromRequest[Proto, Result, Query](request, methodName)
            .flatMap { query =>
              implicit val lc: LoggingContext = LoggingContext(
                (0 until query.productArity)
                  .map(i =>
                    query
                      .productElementName(i) -> query.productElement(i).toString
                  )
                  .toMap + ("participantId" -> participantId.toString)
              )
              measureRequestFuture(serviceName, methodName)(
                f(participantId, traceId, query)
                  .mapLeft(_.unify)
                  .wrapAndRegisterExceptions(serviceName, methodName)
                  .flatten
              )
            }
        }
    }
  }

  final class WithoutAuthPartiallyApplied[Query <: Product] {
    def apply[Proto <: GeneratedMessage, Result](
        methodName: String,
        request: Proto
    )(f: (TraceId, Query) => FutureEither[Err, Result])(implicit
        ec: ExecutionContext,
        protoConverter: ProtoConverter[Proto, Query]
    ): Future[Result] = {
      authenticator.public(methodName, request) { traceId =>
        convertFromRequest[Proto, Result, Query](request, methodName).flatMap { query =>
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
            f(traceId, query)
              .wrapAndRegisterExceptions(serviceName, methodName)
              .flatten
          )
        }
      }
    }
  }

  final class WithoutAuthCoproductPartiallyApplied[Query <: Product] {
    def apply[C <: Coproduct, Proto <: GeneratedMessage, Result](
        methodName: String,
        request: Proto
    )(f: (TraceId, Query) => FutureEither[C, Result])(implicit
        ec: ExecutionContext,
        protoConverter: ProtoConverter[Proto, Query],
        unifier: Unifier.Aux[C, Err]
    ): Future[Result] = {
      authenticator.public(methodName, request) { traceId =>
        convertFromRequest[Proto, Result, Query](request, methodName).flatMap { query =>
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
            f(traceId, query)
              .mapLeft(_.unify)
              .wrapAndRegisterExceptions(serviceName, methodName)
              .flatten
          )
        }
      }
    }
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
    protoConverter.fromProto(request, None) match {
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

object AuthAndMiddlewareSupport {
  // Sometimes we need to authenticate a request that requires no arguments, as the interface requires
  // a Product, we can't use the Unit type but an empty case-class.
  final case object EmptyQuery

  implicit def emptyQueryProtoConverter[T <: scalapb.GeneratedMessage]: ProtoConverter[T, EmptyQuery.type] = { (_, _) =>
    Try(EmptyQuery)
  }
}
