package io.iohk.cef.frontend.client

import javax.ws.rs.Path

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout
import io.iohk.cef.frontend.models.IdentityTransactionRequest
import io.iohk.cef.frontend.services.IdentityTransactionService
import io.swagger.annotations._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

@Api(value = "transaction", produces = "application/json")
@Path("/identities")
class IdentityServiceApi(service: IdentityTransactionService)(implicit executionContext: ExecutionContext)
    extends Directives {

  private implicit val timeout = Timeout(2.seconds)

  @Path("/")
  @ApiOperation(value = "Transaction Request", nickname = "Message Request", httpMethod = "POST")
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name = "body",
        value = "Message Request",
        required = true,
        dataTypeClass = classOf[IdentityTransactionRequest],
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
        entity(as[IdentityTransactionRequest]) { request =>
          val responseHandler = service.process(request)

          onSuccess(responseHandler) { response =>
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
