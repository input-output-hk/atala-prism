package io.iohk.cef.frontend.client

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout
import io.iohk.cef.LedgerId
import io.iohk.cef.frontend.models.{
  CreateIdentityTransactionRequest,
  IdentityTransactionType,
  SubmitIdentityTransactionRequest
}
import io.iohk.cef.frontend.services.IdentityTransactionService
import io.iohk.cef.ledger.identity.IdentityTransaction
import io.swagger.annotations._
import javax.ws.rs.Path

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

@Api(value = "transaction", produces = "application/json")
@Path("/identities")
class IdentityServiceApi(service: IdentityTransactionService)(implicit executionContext: ExecutionContext)
    extends Directives {

  import IdentityServiceApi._

  private implicit val timeout = Timeout(2.seconds)

  @Path("/")
  @ApiOperation(value = "Transaction Request", nickname = "Message Request", httpMethod = "POST")
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name = "body",
        value = "Message Request",
        required = true,
        dataTypeClass = classOf[CreateIdentityTransactionRequest],
        paramType = "body")
    ))
  @ApiResponses(
    Array(
      new ApiResponse(code = 201, message = "Created"),
      new ApiResponse(code = 400, message = "BadRequest Malformed Json")
    ))
  def createIdentity: Route = {
    path("identities") {
      post {
        entity(as[CreateIdentityTransactionRequest]) { request =>
          val responseHandler =
            for {
              tr <- service.createIdentityTransaction(request).onFor
              res <- service.submitIdentityTransaction(toSubmitIdentityTransactionRequest(tr, request.ledgerId)).onFor
            } yield res

          onSuccess(responseHandler.res) { response =>
            complete(
              response match {
                case Right(_) => StatusCodes.Created
                case Left(_) => StatusCodes.BadRequest
              }
            )
          }
        }
      }
    }
  }

}

object IdentityServiceApi {

  private def toSubmitIdentityTransactionRequest(
      it: IdentityTransaction,
      ledgerId: LedgerId): SubmitIdentityTransactionRequest =
    SubmitIdentityTransactionRequest(
      `type` = IdentityTransactionType.of(it),
      identity = it.identity,
      ledgerId = ledgerId,
      publicKey = it.key,
      signature = it.signature)

}
