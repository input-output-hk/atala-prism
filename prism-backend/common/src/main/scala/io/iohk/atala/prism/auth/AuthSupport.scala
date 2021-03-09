package io.iohk.atala.prism.auth

import io.iohk.atala.prism.errors.{ErrorSupport, LoggingContext, PrismError}
import io.iohk.atala.prism.grpc.ProtoConverter
import io.iohk.atala.prism.utils.FutureEither
import scalapb.GeneratedMessage

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait AuthSupport[Err <: PrismError, Id] {
  self: ErrorSupport[Err] =>
  import AuthSupport._

  protected val authenticator: Authenticator[Id]

  final class AuthPartiallyApplied[Query <: Product] {
    def apply[Proto <: GeneratedMessage, Result](
        methodName: String,
        request: Proto
    )(f: (Id, Query) => FutureEither[Err, Result])(implicit
        ec: ExecutionContext,
        protoConverter: ProtoConverter[Proto, Query]
    ): Future[Result] = {
      authenticator
        .authenticated(methodName, request) { participantId =>
          protoConverter.fromProto(request) match {
            case Failure(exception) =>
              val response = invalidRequest(exception.getMessage)
              respondWith(request, response)
            case Success(query) =>
              // Assemble LoggingContext out of the case class fields and the resolved participantId
              implicit val lc: LoggingContext = LoggingContext(
                (0 until query.productArity)
                  .map(i => query.productElementName(i) -> query.productElement(i).toString)
                  .toMap + ("participantId" -> participantId.toString)
              )
              f(participantId, query).wrapExceptions.flatten
          }
        }
    }
  }

  // Query needs to be a Product (which all case class instances are) for easy access to its fields.
  // Partial application helps with Scala type inference. You can specify just a single type
  // parameter Query and all other type parameters can usually be inferred by Scala.
  def auth[Query <: Product]: AuthPartiallyApplied[Query] =
    new AuthPartiallyApplied[Query]()

  def unitAuth: AuthPartiallyApplied[EmptyQuery.type] = auth[EmptyQuery.type]
}

object AuthSupport {
  // Sometimes we need to authenticate a request that requires no arguments, as the interface requires
  // a Product, we can't use the Unit type but an empty case-class.
  final case object EmptyQuery

  implicit def emptyQueryProtoConverter[T <: scalapb.GeneratedMessage]: ProtoConverter[T, EmptyQuery.type] = { _ =>
    Try(EmptyQuery)
  }
}
